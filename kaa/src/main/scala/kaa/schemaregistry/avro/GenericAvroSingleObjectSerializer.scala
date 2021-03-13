package kaa.schemaregistry.avro

import kaa.schemaregistry.{SchemaNotFoundException, SchemaRegistry}
import org.apache.avro.Schema
import org.apache.avro.generic.GenericRecord

class GenericAvroSingleObjectSerializer
(
  schemaRegistry: SchemaRegistry,
  encoding: AvroSingleObjectEncoding,
){
  def this(schemaRegistry: SchemaRegistry) = {
    this(schemaRegistry, AvroSingleObjectEncoding.AVRO_OFFICIAL)
  }

  private val binarySerializer = new GenericAvroBinarySerializer()

  def serialize(record: GenericRecord): Array[Byte] = {
    val currentSchemaId = schemaRegistry.put(record.getSchema)
    val bytes = binarySerializer.write(record)

    encoding.encode(bytes, currentSchemaId)
  }

  def deserialize(bytes: Array[Byte]): GenericRecord = {
    deserialize(bytes, None)
  }
  def deserialize(bytes: Array[Byte], readerSchema: Option[Schema]): GenericRecord = {
    val (schemaId, serialized) = encoding.decode(bytes)
    val schema = schemaRegistry.get(schemaId)
      .getOrElse(throw new SchemaNotFoundException(s"Schema $schemaId not found in registry"))

    binarySerializer.read(serialized, schema, readerSchema)
  }
}
