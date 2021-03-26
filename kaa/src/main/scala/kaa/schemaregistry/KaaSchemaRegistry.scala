package kaa.schemaregistry

import java.lang
import org.apache.avro.Schema
import org.apache.kafka.clients.producer.{KafkaProducer, ProducerRecord}
import org.apache.avro.SchemaNormalization
import org.apache.kafka.clients.consumer.KafkaConsumer

import java.util.{Collections, Properties, UUID}
import java.time.{Duration => JavaDuration}
import com.github.blemale.scaffeine.{Cache, Scaffeine}
import org.apache.kafka.clients.producer.ProducerConfig
import org.apache.kafka.clients.consumer.ConsumerConfig
import org.apache.kafka.common.serialization.{LongDeserializer, StringDeserializer}
import org.apache.kafka.common.serialization.{LongSerializer, StringSerializer}
import org.apache.kafka.clients.CommonClientConfigs
import kaa.schemaregistry.utils.Retry
import kaa.schemaregistry.utils.RetryConfig
import kaa.schemaregistry.KaaSchemaRegistry._
import org.slf4j.{Logger, LoggerFactory}

import scala.concurrent.duration._
import java.util.concurrent.atomic.{AtomicBoolean, AtomicReference}
import scala.concurrent._

object KaaSchemaRegistry {
  val DEFAULT_TOPIC_NAME = "schemas-v1"
  val DEFAULT_CLIENT_ID = "KaaSchemaRegistry"
  val DEFAULT_POLL_INTERVAL: FiniteDuration = 5.second
  val DEFAULT_RETRY_CONFIG: RetryConfig = RetryConfig(5, 2.second)

  def createProps(
                   brokers: String,
                   clientId: String = DEFAULT_CLIENT_ID,
                 ): Properties = {
    val props = new Properties()
    props.put(CommonClientConfigs.BOOTSTRAP_SERVERS_CONFIG, brokers)
    props.put(CommonClientConfigs.CLIENT_ID_CONFIG, clientId)
    props
  }
}

/**
 * Create a new instance of the Schema Registry. Call `start` to connect to Kafka (consumer and producer) and
 * start reading available schemas.
 * @param producerProps Kafka Producer properties
 * @param consumerProps Kafka Consumer properties
 * @param onError Callback executed when there is a background error. You should usually log, close and start again the instance.
 * @param topic Topic where to read and write schemas
 * @param pollInterval Interval used to read schemas
 * @param getRetry Get retry configurations
 */
class KaaSchemaRegistry(
                         producerProps: Properties,
                         consumerProps: Properties,
                         onError: Exception => Unit,
                         topic: String = DEFAULT_TOPIC_NAME,
                         pollInterval: Duration = DEFAULT_POLL_INTERVAL,
                         getRetry: RetryConfig = DEFAULT_RETRY_CONFIG,
                       ) extends SchemaRegistry {

  def this(producerProps: Properties,
           consumerProps: Properties,
           onError: Exception => Unit,
          ) = {
    this(
      producerProps = producerProps,
      consumerProps = consumerProps,
      onError,
      topic = DEFAULT_TOPIC_NAME,
      pollInterval = DEFAULT_POLL_INTERVAL,
      getRetry = DEFAULT_RETRY_CONFIG,
    )
  }

  def this(brokers: String, onError: Exception => Unit) = {
    this(
      producerProps = createProps(brokers),
      consumerProps = createProps(brokers),
      onError,
    )
  }

  private val logger: Logger = LoggerFactory.getLogger(this.getClass.getSimpleName)
  private val cache: Cache[Long, String] = Scaffeine().build[Long, String]()
  private val stopping = new AtomicBoolean(false)
  private val producer = new AtomicReference[Option[KaaProducer]](None)
  private val consumer = new AtomicReference[Option[KaaConsumer]](None)

  def start()(implicit ec: ExecutionContext): Unit = {
    logger.debug("Starting ...")

    if (stopping.get())
      throw InvalidStateException("Schema registry is stopping")
    if (!consumer.compareAndSet(None, Some(new KaaConsumer())))
      throw InvalidStateException("Schema registry consumer already created")
    if (!producer.compareAndSet(None, Some(new KaaProducer())))
      throw InvalidStateException("Schema registry producer already created")
  }

  def close(maxWait: Duration = 10.seconds): Unit = {
    logger.debug("Closing ...")

    stopping.set(true)
    consumer.getAndSet(None) match {
      case Some(c) => c.close(maxWait)
      case None =>
    }
    producer.getAndSet(None) match {
      case Some(p) => p.close(maxWait)
      case None =>
    }
    cache.invalidateAll()
    stopping.set(false)
  }

  override def put(schema: Schema): SchemaId = {
    producer.get() match {
      case Some(p) => p.put(schema)
      case None => throw InvalidStateException("Schema registry not started")
    }
  }

  override def get(id: SchemaId): Option[Schema] = {
    Retry.retryIfNone(getRetry) {
      cache.getIfPresent(id.value)
        .map(new Schema.Parser().parse)
    }
  }

  class KaaConsumer()(implicit ec: ExecutionContext) {
    private val consumer = createConsumer()
    private val startConsumerFuture = startConsumer()

    private def startConsumer(): Future[Unit] = Future {
      try {
        consumer.subscribe(Collections.singletonList(topic))
        val jPollInterval = JavaDuration.ofMillis(pollInterval.toMillis)
        while (!stopping.get()) {
          logger.debug(s"Polling from topic $topic")
          val records = consumer.poll(jPollInterval)
          records.forEach(record => {
            logger.debug(s"Found schema ${record.key()}")
            cache.put(record.key(), record.value())
          })
        }
      } catch {
        case ex: Exception => onError(ex)
      }
    }

    def close(maxWait: Duration): Unit = {
      Await.result(startConsumerFuture, maxWait)
      val maxWaitJava = JavaDuration.ofMillis(maxWait.toMillis)
      consumer.close(maxWaitJava)
    }

    protected def createConsumer(): KafkaConsumer[lang.Long, String] = {
      new KafkaConsumer(fillConsumerProps(), new LongDeserializer(), new StringDeserializer())
    }

    protected def fillConsumerProps(): Properties = {
      consumerProps.putIfAbsent(ConsumerConfig.CLIENT_ID_CONFIG, KaaSchemaRegistry.DEFAULT_CLIENT_ID)

      consumerProps.put(ConsumerConfig.GROUP_ID_CONFIG, UUID.randomUUID().toString)
      consumerProps.put(ConsumerConfig.ENABLE_AUTO_COMMIT_CONFIG, "false")
      consumerProps.put(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "earliest")

      consumerProps
    }
  }

  class KaaProducer() {
    private val producer = createProducer()

    def close(maxWait: Duration): Unit = {
      val maxWaitJava = JavaDuration.ofMillis(maxWait.toMillis)
      producer.close(maxWaitJava)
    }

    def put(schema: Schema): SchemaId = {
      if (stopping.get()) throw new UnsupportedOperationException("KaaSchemaRegistry is not available")

      val fingerprint = SchemaNormalization.parsingFingerprint64(schema)

      if (cache.getIfPresent(fingerprint).isEmpty) {
        logger.debug(s"Writing schema $fingerprint to $topic")

        val record = new ProducerRecord[java.lang.Long, String](topic, fingerprint, schema.toString())
        producer.send(record).get()
      }

      SchemaId(fingerprint)
    }

    protected def createProducer(): KafkaProducer[lang.Long, String] = {
      new KafkaProducer(fillProducerProps(), new LongSerializer(), new StringSerializer())
    }

    protected def fillProducerProps(): Properties = {
      producerProps.putIfAbsent(ProducerConfig.CLIENT_ID_CONFIG, KaaSchemaRegistry.DEFAULT_CLIENT_ID)

      producerProps
    }
  }
}