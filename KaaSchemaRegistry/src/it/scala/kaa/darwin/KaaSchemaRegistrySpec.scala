package kaa.darwin

import com.sksamuel.avro4s.AvroSchema
import org.scalatest._
import flatspec._
import matchers._
import kaa.KaaSchemaRegistry
import kaa.KaaSchemaRegistryAdmin
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

    val schema = AvroSchema[Foo]
    val schemaId = target.put(schema)

    target.get(schemaId) match {
      case None => fail("Schema not found")
      case Some(schemaRetrieved) => schemaRetrieved should be (schema)
    }
  }

  case class Foo(name: String, age: Int) {}
}
