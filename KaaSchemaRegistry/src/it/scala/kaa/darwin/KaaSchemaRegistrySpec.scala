package kaa.darwin

import com.sksamuel.avro4s.AvroSchema
import org.scalatest._
import flatspec._
import matchers._
import kaa.KaaSchemaRegistry
import kaa.KaaSchemaRegistryAdmin

class KaaSchemaRegistrySpec extends AnyFlatSpec with should.Matchers with BeforeAndAfterAll {

  val BROKERS = "localhost:9092"

  override protected def beforeAll(): Unit = {
    val admin = new KaaSchemaRegistryAdmin(BROKERS)
    admin.createTopic()
  }

  override protected def afterAll(): Unit = {
    val admin = new KaaSchemaRegistryAdmin(BROKERS)
    admin.deleteTopic()
  }

  "KaaSchemaRegistry" should "put and retrieve a schema" in {
    val target = new KaaSchemaRegistry(BROKERS)

    val schema = AvroSchema[Foo]
    val schemaId = target.put(schema)

    target.get(schemaId) match {
      case None => fail("Schema not found")
      case Some(schemaRetrieved) => schemaRetrieved should be (schema)
    }
  }

  case class Foo(name: String, age: Int) {}
}
