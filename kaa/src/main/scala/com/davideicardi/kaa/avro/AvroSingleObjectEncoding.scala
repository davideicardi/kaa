package com.davideicardi.kaa.avro

import java.nio.{ByteBuffer, ByteOrder}

import com.davideicardi.kaa.SchemaId
import com.davideicardi.kaa.InvalidSchemaException

object AvroSingleObjectEncoding {
  /**
   * https://avro.apache.org/docs/current/spec.html#single_object_encoding
   * Note: this is not the same encoding using by Confluent Avro serializer.
   * Confluent Avro serializer uses 0x00 as magic byte and and id of 4 bytes (incremental? not an hash).
   */
  val default : AvroSingleObjectEncoding = {
    new AvroSingleObjectEncoding(
      Array(0xC3.toByte, 0x01.toByte),
      ByteOrder.LITTLE_ENDIAN,
    )
  }
}

class AvroSingleObjectEncoding(
                              header: Array[Byte],
                              schemaIdByteOrder: ByteOrder,
                              ) {
  private val ID_SIZE = 8
  private val HEADER_LENGTH = header.length + ID_SIZE

  def encode(avroPayload: Array[Byte], schemaId: SchemaId): Array[Byte] = {
    Array.concat(header, schemaId.value.longToByteArray, avroPayload)
  }

  def decode(avroSingleObjectEncoded: Array[Byte]): (SchemaId, Array[Byte]) = {
    if (matchEncoding(avroSingleObjectEncoded)) {
      val id = avroSingleObjectEncoded.slice(header.length, HEADER_LENGTH).byteArrayToLong
      val data = avroSingleObjectEncoded.drop(HEADER_LENGTH)

      SchemaId(id) -> data
    }
    else {
      throw new InvalidSchemaException(s"Byte array is not in correct format. First ${header.length} bytes are not equal" +
        s" to ${header.mkString("[", ", ", "]")}")
    }
  }

  def matchEncoding(data: Array[Byte]): Boolean = {
    if (data.length < header.length) {
      false
    } else {
      data.take(header.length).sameElements(header)
    }
  }

  private implicit class EnrichedLong(l: Long) {
    def longToByteArray: Array[Byte] = {
      ByteBuffer.allocate(ID_SIZE)
        .order(schemaIdByteOrder)
        .putLong(l).array()
    }
  }

  private implicit class EnrichedByteArray(a: Array[Byte]) {
    def byteArrayToLong: Long = ByteBuffer
      .wrap(a)
      .order(schemaIdByteOrder)
      .getLong
  }

}
