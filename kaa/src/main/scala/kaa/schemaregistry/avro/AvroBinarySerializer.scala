package kaa.schemaregistry.avro

import com.sksamuel.avro4s._
import org.apache.avro._
import org.apache.avro.generic.{GenericDatumReader, GenericDatumWriter}
import org.apache.avro.io.{DecoderFactory, EncoderFactory}

import java.io.ByteArrayOutputStream

class AvroBinarySerializer[T : SchemaFor : Encoder : Decoder] {

  val currentSchema: Schema = AvroSchema[T]
  private val avroDecoder = Decoder[T]
  private val avroEncoder = Encoder[T]
  private val datumWriter = new GenericDatumWriter[AnyRef](currentSchema)

  def write(value: T): Array[Byte] = {
    val stream = new ByteArrayOutputStream()

    try {
      val binaryEncoder = EncoderFactory.get().binaryEncoder(stream, null)
      val datum = avroEncoder.encode(value)
      datumWriter.write(datum, binaryEncoder)
      binaryEncoder.flush()
      stream.flush()
    } finally {
      stream.close()
    }

    stream.toByteArray
  }

  def read(value: Array[Byte], writerSchema: Schema): T = {
    val datumReader = new GenericDatumReader[Any](writerSchema, currentSchema)
    val binaryDecoder = DecoderFactory.get().binaryDecoder(value, null)

    val datum = datumReader.read(null, binaryDecoder)
    avroDecoder.decode(datum)
  }
}
