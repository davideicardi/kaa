package kaa

import com.sksamuel.avro4s.{Decoder, Encoder, SchemaFor}
import com.sksamuel.avro4s.kafka.GenericSerde

class KaaGenericSerde[T >: Null : SchemaFor : Encoder : Decoder] extends GenericSerde[T] {

  override def deserialize(topic: String, data: Array[Byte]): T = {
    ???
  }

  override def serialize(topic: String, data: T): Array[Byte] = {
    ???
  }
}
