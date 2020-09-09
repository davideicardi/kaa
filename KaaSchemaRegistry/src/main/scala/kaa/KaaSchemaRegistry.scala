package kaa

import org.apache.avro.Schema
import org.apache.kafka.clients.producer.{KafkaProducer, ProducerRecord}
import org.apache.avro.SchemaNormalization
import org.apache.kafka.clients.consumer.KafkaConsumer
import java.util.{Collections, Properties, UUID}
import java.util.concurrent.Executors
import java.util.concurrent.ExecutorService
import collection.JavaConverters._
import com.github.blemale.scaffeine.{ Cache, Scaffeine }
import org.apache.kafka.clients.producer.ProducerConfig
import org.apache.kafka.clients.consumer.ConsumerConfig
import org.apache.kafka.common.serialization.{LongDeserializer, StringDeserializer}
import org.apache.kafka.common.serialization.{LongSerializer, StringSerializer}

object KaaSchemaRegistry {
  val DEFAULT_TOPIC_NAME = "kaa-schema-registry-v1"
}

class KaaSchemaRegistry(
  brokers: String,
  topic: String = KaaSchemaRegistry.DEFAULT_TOPIC_NAME,
  cliendId: String = "KaaSchemaRegistry"
) extends SchemaRegistry {

  // TODO Eval to put this code inside an "init" function instead of here in the constructor
  private val producer: KafkaProducer[java.lang.Long, String]
    = new KafkaProducer(createProducerConfig(), new LongSerializer(), new StringSerializer())
  private val consumer: KafkaConsumer[java.lang.Long, String]
    = new KafkaConsumer(createConsumerConfig(), new LongDeserializer(), new StringDeserializer())
  private var executor: ExecutorService = null
  private val cache: Cache[Long, String] = Scaffeine().build[Long, String]()
  consumer.subscribe(Collections.singletonList(topic))
  // TODO eval alternative for this executor
  executor = Executors.newSingleThreadExecutor
  executor.execute(new Runnable {
    override def run(): Unit = {
      while (true) {
        val records = consumer.poll(1000)

        records.forEach((record) => {
          cache.put(record.key(), record.value())
        })
      }
    }
  })

  // TODO eval if shutdown is called properly
  def shutdown(): Unit = {
    consumer.close();
    if (executor != null)
      executor.shutdown();
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
    cache.getIfPresent(id.value)
      .map(new Schema.Parser().parse)
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