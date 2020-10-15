package com.davideicardi.kaa.avro

import java.io.ByteArrayOutputStream

import com.sksamuel.avro4s._
import org.apache.avro._

class AvroBinarySerializer[T >: Null : SchemaFor : Encoder : Decoder] {
  val currentSchema: Schema = AvroSchema[T]

  def write(value: T): Array[Byte] = {
    val stream = new ByteArrayOutputStream()

    val output = AvroOutputStream.binary[T]
      .to(stream)
      .build()

    try {
      output.write(value)
      output.flush()
    } finally {
      output.close()
    }

    stream.toByteArray
  }

  def read(value: Array[Byte], writerSchema: Schema): T = {
    val input = AvroInputStream.binary[T]
      .from(value)
      .build(writerSchema)

    try {
      input.iterator
        .toSeq
        .head
    } finally {
      input.close()
    }
  }
}
