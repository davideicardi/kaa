package kaa

import org.apache.avro.Schema
import org.apache.kafka.clients.producer.{KafkaProducer, ProducerRecord}
import org.apache.avro.SchemaNormalization
import org.apache.kafka.clients.consumer.KafkaConsumer
import java.util.{Collections, Properties}
import java.util.concurrent.Executors
import java.util.concurrent.ExecutorService
import collection.JavaConverters._
import com.github.blemale.scaffeine.{ Cache, Scaffeine }
import org.apache.kafka.clients.producer.ProducerConfig
import org.apache.kafka.clients.consumer.ConsumerConfig
import org.apache.kafka.common.serialization.{LongDeserializer, StringDeserializer}
import org.apache.kafka.common.serialization.{LongSerializer, StringSerializer}

trait SchemaRegistry {
  def put(schema: Schema): SchemaId

  def get(id: SchemaId): Option[Schema]
}
