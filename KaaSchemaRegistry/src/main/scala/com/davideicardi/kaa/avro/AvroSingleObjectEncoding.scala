package com.davideicardi.kaa.avro

import java.nio.{ByteBuffer, ByteOrder}

import com.davideicardi.kaa.SchemaId

/**
 * https://avro.apache.org/docs/current/spec.html#single_object_encoding
 */
object AvroSingleObjectEncoding {
  private val V1_HEADER = Array(0xC3.toByte, 0x01.toByte)
  private val ID_SIZE = 8
  private val HEADER_LENGTH = V1_HEADER.length + ID_SIZE

  def encode(avroPayload: Array[Byte], schemaId: SchemaId): Array[Byte] = {
    Array.concat(V1_HEADER, schemaId.value.longToByteArray, avroPayload)
  }

  def decode(avroSingleObjectEncoded: Array[Byte]): (SchemaId, Array[Byte]) = {
    if (matchEncoding(avroSingleObjectEncoded)) {
      val id = avroSingleObjectEncoded.slice(V1_HEADER.length, HEADER_LENGTH).byteArrayToLong
      val data = avroSingleObjectEncoded.drop(HEADER_LENGTH)

      SchemaId(id) -> data
    }
    else {
      throw new Exception(s"Byte array is not in correct format. First ${V1_HEADER.length} bytes are not equal" +
        s" to ${V1_HEADER.mkString("[", ", ", "]")}")
    }
  }

  def matchEncoding(data: Array[Byte]): Boolean = {
    if (data.length < V1_HEADER.length) {
      false
    } else {
      data.take(V1_HEADER.length).sameElements(V1_HEADER)
    }
  }

  private implicit class EnrichedLong(l: Long) {
    def longToByteArray: Array[Byte] = {
      ByteBuffer.allocate(ID_SIZE)
        .order(ByteOrder.LITTLE_ENDIAN)
        .putLong(l).array()
    }
  }

  private implicit class EnrichedByteArray(a: Array[Byte]) {
    def byteArrayToLong: Long = ByteBuffer
      .wrap(a)
      .order(ByteOrder.LITTLE_ENDIAN)
      .getLong
  }

}
