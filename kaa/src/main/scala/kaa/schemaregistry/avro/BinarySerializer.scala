package kaa.schemaregistry.avro

import com.sksamuel.avro4s._
import org.apache.avro._

class BinarySerializer[T : SchemaFor : Encoder : Decoder] {
  val currentSchema: Schema = AvroSchema[T]
  private val avroDecoder = Decoder[T]
  private val avroEncoder = Encoder[T]

  def serialize(value: T): Array[Byte] = {
    val datum = avroEncoder.encode(value)
    AvroBinary.write(datum, currentSchema)
  }

  def deserialize(bytes: Array[Byte], writerSchema: Schema): T = {
    val datum = AvroBinary.read(bytes, writerSchema, Some(currentSchema))
    avroDecoder.decode(datum)
  }
}
