package kaa.schemaregistry.test

import org.apache.avro.Schema
import org.apache.avro.SchemaNormalization
import kaa.schemaregistry.SchemaId
import kaa.schemaregistry.SchemaRegistry

class TestSchemaRegistry() extends SchemaRegistry {
  val schemas = collection.mutable.Map[SchemaId, Schema]()

  override def put(schema: Schema): SchemaId = {
    val fingerprint = SchemaNormalization.parsingFingerprint64(schema)
    val key = SchemaId(fingerprint)

    schemas.put(key, schema)

    key
  }

  override def get(id: SchemaId): Option[Schema] = {
    schemas.get(id)
  }
}
