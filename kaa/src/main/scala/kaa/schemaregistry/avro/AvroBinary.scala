package kaa.schemaregistry.avro

import org.apache.avro._
import org.apache.avro.generic.{GenericDatumReader, GenericDatumWriter}
import org.apache.avro.io.{DecoderFactory, EncoderFactory}

import java.io.ByteArrayOutputStream

object AvroBinary {
  def write(value: Any, writerSchema: Schema): Array[Byte] = {
    val datumWriter = new GenericDatumWriter[Any](writerSchema)
    val outputStream = new ByteArrayOutputStream()
    try {
      val encoder = EncoderFactory.get.binaryEncoder(outputStream, null)
      datumWriter.write(value, encoder)
      encoder.flush()
      outputStream.flush()
      outputStream.toByteArray
    } finally {
      outputStream.close()
    }
  }

  def read(bytes: Array[Byte], writerSchema: Schema, readerSchema: Option[Schema] = None): Any = {
    val datumReader = new GenericDatumReader[Any](
      writerSchema,
      readerSchema.getOrElse(writerSchema)
    )
    val binaryDecoder = DecoderFactory.get.binaryDecoder(bytes, null)
    datumReader.read(null, binaryDecoder)
  }
}
