package kaa.schemaregistry

import org.apache.avro.Schema

trait SchemaRegistry {
  def put(schema: Schema): SchemaId

  def get(id: SchemaId): Option[Schema]
}
