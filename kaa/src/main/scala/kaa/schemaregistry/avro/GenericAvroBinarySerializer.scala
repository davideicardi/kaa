package kaa.schemaregistry.avro

import org.apache.avro._
import java.io.ByteArrayOutputStream

import org.apache.avro.file.SeekableByteArrayInput
import org.apache.avro.generic.{GenericDatumReader, GenericDatumWriter, GenericRecord}
import org.apache.avro.io.{DecoderFactory, EncoderFactory}

class GenericAvroBinarySerializer() {
  def write(record: GenericRecord): Array[Byte] = {
    val datumWriter = new GenericDatumWriter[GenericRecord](record.getSchema)

    val outputStream = new ByteArrayOutputStream()
    try {
      val encoder = EncoderFactory.get.binaryEncoder(outputStream, null)

      datumWriter.write(record, encoder)

      encoder.flush()

      outputStream.toByteArray
    } finally {
      outputStream.close()
    }
  }

  def read(bytes: Array[Byte], writerSchema: Schema, readerSchema: Option[Schema] = None): GenericRecord = {
    val datumReader = new GenericDatumReader[GenericRecord](
      writerSchema,
      readerSchema.getOrElse(writerSchema)
    )
    val inputStream = new SeekableByteArrayInput(bytes)
    try {
      val decoder = DecoderFactory.get.binaryDecoder(inputStream, null)
      datumReader.read(null, decoder)
    } finally {
      inputStream.close()
    }
  }
}
