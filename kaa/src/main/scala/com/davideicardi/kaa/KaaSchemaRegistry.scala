package com.davideicardi.kaa

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

import scala.concurrent.duration._
import com.davideicardi.kaa.utils.Retry
import java.util.concurrent.atomic.AtomicBoolean

import com.davideicardi.kaa.KaaSchemaRegistry._

import scala.concurrent._
import com.davideicardi.kaa.utils.RetryConfig
import org.apache.kafka.clients.CommonClientConfigs

object KaaSchemaRegistry {
  val DEFAULT_TOPIC_NAME = "schemas-v1"
  val DEFAULT_CLIENT_ID = "KaaSchemaRegistry"
  val DEFAULT_POLL_INTERVAL: FiniteDuration = 5.second
  val DEFAULT_RETRY_CONFIG: RetryConfig = RetryConfig(5, 2.second)

  def createProps(
            brokers: String,
            clientId: String = DEFAULT_CLIENT_ID,
          ): Properties = {
    val consumerProps = new Properties()
    consumerProps.put(CommonClientConfigs.BOOTSTRAP_SERVERS_CONFIG, brokers)
    consumerProps.put(CommonClientConfigs.CLIENT_ID_CONFIG, clientId)
    consumerProps
  }
}

class KaaSchemaRegistry(
  producerProps: Properties,
  consumerProps: Properties,
  topic: String = DEFAULT_TOPIC_NAME,
  pollInterval: Duration = DEFAULT_POLL_INTERVAL,
  getRetry: RetryConfig = DEFAULT_RETRY_CONFIG
) extends SchemaRegistry {

  def this(producerProps: Properties,
           consumerProps: Properties) = {
    this(
      producerProps = producerProps,
      consumerProps = consumerProps,
      topic = DEFAULT_TOPIC_NAME,
      pollInterval = DEFAULT_POLL_INTERVAL,
      getRetry = DEFAULT_RETRY_CONFIG,
    )
  }
  def this(brokers: String) = {
    this(
      producerProps = createProps(brokers),
      consumerProps = createProps(brokers),
    )
  }

  implicit private val ec: ExecutionContextExecutor = ExecutionContext.global

  private val producer = createProducer()
  private val consumer = createConsumer()
  private val cache: Cache[Long, String] = Scaffeine().build[Long, String]()
  private val stopping = new AtomicBoolean(false)
  private val startConsumerFuture = startConsumer()

  private def startConsumer(): Future[Unit] = Future {
    consumer.subscribe(Collections.singletonList(topic))
    val jPollInterval = JavaDuration.ofMillis(pollInterval.toMillis)
    while (!stopping.get()) {
      val records = consumer.poll(jPollInterval)

      records.forEach(record => {
        cache.put(record.key(), record.value())
      })
    }
  }

  def close(maxWait: Duration = 10.seconds): Unit = {
    stopping.set(true)
    Await.result(startConsumerFuture, maxWait)
    val maxWaitJava = JavaDuration.ofMillis(maxWait.toMillis)
    consumer.close(maxWaitJava)
    producer.close(maxWaitJava)
  }

  override def put(schema: Schema): SchemaId = {
    if (stopping.get()) throw new UnsupportedOperationException("KaaSchemaRegistry is not available")

    val fingerprint = SchemaNormalization.parsingFingerprint64(schema)

    if (cache.getIfPresent(fingerprint).isEmpty) {
      val record = new ProducerRecord[java.lang.Long, String](topic, fingerprint, schema.toString())
      producer.send(record).get()
    }

    SchemaId(fingerprint)
  }

  override def get(id: SchemaId): Option[Schema] = {
    Retry.retryIfNone(getRetry) {
      cache.getIfPresent(id.value)
        .map(new Schema.Parser().parse)
    }
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

  protected def createProducer(): KafkaProducer[lang.Long, String] = {
    new KafkaProducer(fillProducerProps(), new LongSerializer(), new StringSerializer())
  }

  protected def fillProducerProps(): Properties = {
    producerProps.putIfAbsent(ProducerConfig.CLIENT_ID_CONFIG, KaaSchemaRegistry.DEFAULT_CLIENT_ID)

    producerProps
  }
}
