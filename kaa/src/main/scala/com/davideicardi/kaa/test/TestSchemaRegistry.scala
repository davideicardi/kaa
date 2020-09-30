package com.davideicardi.kaa.test

import org.apache.avro.Schema
import org.apache.avro.SchemaNormalization
import com.davideicardi.kaa.SchemaId
import com.davideicardi.kaa.SchemaRegistry

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
