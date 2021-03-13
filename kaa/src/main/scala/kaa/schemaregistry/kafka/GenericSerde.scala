package kaa.schemaregistry.kafka

import com.sksamuel.avro4s.{Decoder, Encoder, SchemaFor}
import kaa.schemaregistry.SchemaRegistry
import kaa.schemaregistry.avro.AvroSingleObjectSerializer
import org.apache.kafka.common.serialization.{Deserializer, Serde, Serializer}

class GenericSerde[T >: Null : SchemaFor : Encoder : Decoder]
(schemaManager: SchemaRegistry)
  extends Serde[T]
    with Deserializer[T]
    with Serializer[T]
    with Serializable {

  private val avroSerializer = new AvroSingleObjectSerializer[T](schemaManager)

  def serializer: Serializer[T] = this
  def deserializer: Deserializer[T] = this

  override def deserialize(topic: String, data: Array[Byte]): T = {
    if (data == null || data.length == 0) {
      null
    } else {
      avroSerializer.deserialize(data)
    }
  }

  override def serialize(topic: String, data: T): Array[Byte] = {
    if (data == null) {
      Array()
    } else {
      avroSerializer.serialize(data)
    }
  }

  override def close(): Unit = ()

  override def configure(configs: java.util.Map[String, _], isKey: Boolean): Unit = ()
}