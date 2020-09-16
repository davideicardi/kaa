package com.davideicardi.kaa

import org.apache.avro.Schema
import org.apache.kafka.clients.producer.{KafkaProducer, ProducerRecord}
import org.apache.avro.SchemaNormalization
import org.apache.kafka.clients.consumer.KafkaConsumer
import java.util.{Collections, Properties, UUID}
import java.time.{Duration => JavaDuration}
import com.github.blemale.scaffeine.{ Cache, Scaffeine }
import org.apache.kafka.clients.producer.ProducerConfig
import org.apache.kafka.clients.consumer.ConsumerConfig
import org.apache.kafka.common.serialization.{LongDeserializer, StringDeserializer}
import org.apache.kafka.common.serialization.{LongSerializer, StringSerializer}
import scala.concurrent.duration._
import com.davideicardi.kaa.utils.Retry
import java.util.concurrent.atomic.AtomicBoolean
import scala.concurrent.Future
import scala.concurrent.ExecutionContext
import scala.concurrent.Await

object KaaSchemaRegistry {
  val DEFAULT_TOPIC_NAME = "schemas-v1"
}

class KaaSchemaRegistry(
  brokers: String,
  topic: String = KaaSchemaRegistry.DEFAULT_TOPIC_NAME,
  cliendId: String = "KaaSchemaRegistry",
  pollInterval: Duration = 5.second,
  getRetries: Int = 5,
  getRetryDelay: Duration = 2.second
) extends SchemaRegistry {

  // TODO Eval to put this code inside an "init" function instead of here in the constructor
  private val producer: KafkaProducer[java.lang.Long, String]
    = new KafkaProducer(createProducerConfig(), new LongSerializer(), new StringSerializer())
  private val consumer: KafkaConsumer[java.lang.Long, String]
    = new KafkaConsumer(createConsumerConfig(), new LongDeserializer(), new StringDeserializer())
  private val cache: Cache[Long, String] = Scaffeine().build[Long, String]()
  private val stopping = new AtomicBoolean(false)
  implicit private val ec = ExecutionContext.global
  private val subscriber = Future {
    consumer.subscribe(Collections.singletonList(topic))
      val jPollInterval = JavaDuration.ofMillis(pollInterval.toMillis)
      while (!stopping.get()) {
        val records = consumer.poll(jPollInterval)

        records.forEach((record) => {
          cache.put(record.key(), record.value())
        })
      }

      consumer.close();
  }

  // TODO eval if shutdown is called properly
  def shutdown(): Unit = {
    stopping.set(true)
    Await.result(subscriber, 10.seconds)
  }

  override def put(schema: Schema): SchemaId = {
    val fingerprint = SchemaNormalization.parsingFingerprint64(schema)

    if (cache.getIfPresent(fingerprint).isEmpty) {
      val record = new ProducerRecord[java.lang.Long, String](topic, fingerprint, schema.toString())
      producer.send(record).get()
    }

    SchemaId(fingerprint)
  }

  override def get(id: SchemaId): Option[Schema] = {
    Retry.retryIfNone(getRetries, getRetryDelay) {
      cache.getIfPresent(id.value)
        .map(new Schema.Parser().parse)
    }
  }

  def createConsumerConfig(): Properties = {
    val props = new Properties()
    props.put(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, brokers)
    props.put(ConsumerConfig.CLIENT_ID_CONFIG, cliendId)
    props.put(ConsumerConfig.GROUP_ID_CONFIG, UUID.randomUUID().toString());
    props.put(ConsumerConfig.ENABLE_AUTO_COMMIT_CONFIG, "false")
    props.put(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "earliest")
    // TODO study if we need other default properties and allow to extend this from outside
    props
  }

  def createProducerConfig(): Properties = {
    val props = new Properties()
    props.put(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, brokers)
    props.put(ProducerConfig.CLIENT_ID_CONFIG, cliendId)
    // TODO study if we need other default properties and allow to extend this from outside
    props
  }
}