package com.davideicardi.kaa.avro

import com.sksamuel.avro4s._
import com.davideicardi.kaa.SchemaRegistry
import com.davideicardi.kaa.SchemaNotFoundException

class AvroSingleObjectSerializer[T >: Null : SchemaFor : Encoder : Decoder]
(schemaRegistry: SchemaRegistry){

  private val binarySerializer = new AvroBinarySerializer[T]()
  private lazy val currentSchemaId = schemaRegistry.put(binarySerializer.currentSchema)

  def serialize(obj: T): Array[Byte] = {
    val bytes = binarySerializer.write(obj)

    AvroSingleObjectEncoding.encode(bytes, currentSchemaId)
  }

  def deserialize(bytes: Array[Byte]): T = {
    val (schemaId, serialized) = AvroSingleObjectEncoding.decode(bytes)
    val schema = schemaRegistry.get(schemaId)
      .getOrElse(throw new SchemaNotFoundException(s"Schema $schemaId not found in registry"))

    binarySerializer.read(serialized, schema)
  }
}
