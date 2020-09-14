package com.davideicardi.kaa.darwin

import com.sksamuel.avro4s.AvroSchema
import org.scalatest._
import flatspec._
import matchers._
import com.davideicardi.kaa.{KaaSchemaRegistry, KaaSchemaRegistryAdmin, SchemaId}
import java.util.UUID

class KaaSchemaRegistrySpec extends AnyFlatSpec with should.Matchers with BeforeAndAfterAll {

  val BROKERS = "localhost:9092"
  val TOPIC_NAME = "schema-registry-test" + UUID.randomUUID().toString()
  val admin = new KaaSchemaRegistryAdmin(BROKERS, TOPIC_NAME)

  override protected def beforeAll(): Unit = {
    if (!admin.topicExists())
      admin.createTopic()
  }

  override protected def afterAll(): Unit = {
    admin.deleteTopic()
  }

  "KaaSchemaRegistry" should "put and retrieve a schema" in {
    val target = new KaaSchemaRegistry(BROKERS, TOPIC_NAME)

    try {
      val schema = AvroSchema[Foo]
      val schemaId = target.put(schema)

      target.get(schemaId) match {
        case None => fail("Schema not found")
        case Some(schemaRetrieved) => schemaRetrieved should be (schema)
      }
    } finally {
      target.shutdown()
    }
  }

  it should "return None for not existing schema" in {
    val target = new KaaSchemaRegistry(BROKERS, TOPIC_NAME)

    try {
      target.get(SchemaId(999L)) match {
        case None => succeed
        case Some(_) => fail("Schema should not be retrieved")
      }
    } finally {
      target.shutdown()
    }
  }

  case class Foo(name: String, age: Int) {}
}
