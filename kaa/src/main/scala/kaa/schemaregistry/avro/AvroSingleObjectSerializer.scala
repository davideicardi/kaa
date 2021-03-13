package kaa.schemaregistry.avro

import com.sksamuel.avro4s._
import kaa.schemaregistry.SchemaRegistry
import kaa.schemaregistry.SchemaNotFoundException

class AvroSingleObjectSerializer[T >: Null : SchemaFor : Encoder : Decoder]
(
  schemaRegistry: SchemaRegistry,
  encoding: AvroSingleObjectEncoding = AvroSingleObjectEncoding.AVRO_OFFICIAL
){
  private val binarySerializer = new AvroBinarySerializer[T]()
  private lazy val currentSchemaId = schemaRegistry.put(binarySerializer.currentSchema)

  def serialize(obj: T): Array[Byte] = {
    val bytes = binarySerializer.write(obj)

    encoding.encode(bytes, currentSchemaId)
  }

  def deserialize(bytes: Array[Byte]): T = {
    val (schemaId, serialized) = encoding.decode(bytes)
    val schema = schemaRegistry.get(schemaId)
      .getOrElse(throw new SchemaNotFoundException(s"Schema $schemaId not found in registry"))

    binarySerializer.read(serialized, schema)
  }
}
