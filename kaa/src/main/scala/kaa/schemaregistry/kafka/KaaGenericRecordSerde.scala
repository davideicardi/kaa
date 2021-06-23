package kaa.schemaregistry.kafka

import kaa.schemaregistry.SchemaRegistry
import kaa.schemaregistry.avro.GenericRecordSingleObjectSerializer
import org.apache.avro.generic.GenericRecord
import org.apache.kafka.common.serialization._


class KaaGenericRecordSerde
(schemaManager: SchemaRegistry)
  extends Serde[GenericRecord]
    with Deserializer[GenericRecord]
    with Serializer[GenericRecord]
    with Serializable {

  private val avroSerializer = new GenericRecordSingleObjectSerializer(schemaManager)

  def serializer: Serializer[GenericRecord] = this
  def deserializer: Deserializer[GenericRecord] = this

  override def deserialize(topic: String, data: Array[Byte]): GenericRecord = {
    if (data == null || data.length == 0) {
      null
    } else {
      avroSerializer.deserialize(data)
    }
  }

  override def serialize(topic: String, data: GenericRecord): Array[Byte] = {
    if (data == null) {
      Array()
    } else {
      avroSerializer.serialize(data)
    }
  }

  override def close(): Unit = ()

  override def configure(configs: java.util.Map[String, _], isKey: Boolean): Unit = ()
}
