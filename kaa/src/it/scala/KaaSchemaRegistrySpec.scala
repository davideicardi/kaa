import java.util.UUID

import kaa.schemaregistry.{KaaSchemaRegistry, KaaSchemaRegistryAdmin, SchemaId}
import com.sksamuel.avro4s.AvroSchema
import org.scalatest._
import org.scalatest.flatspec._
import org.scalatest.matchers._

class KaaSchemaRegistrySpec extends AnyFlatSpec with should.Matchers with BeforeAndAfterAll {

  private val BROKERS = "localhost:9092"
  private val TOPIC_NAME = "schema-registry-test" + UUID.randomUUID().toString
  private val props = KaaSchemaRegistry.createProps(BROKERS, "KaaSchemaRegistrySpec")
  private val admin = new KaaSchemaRegistryAdmin(
    props,
    TOPIC_NAME,
  )

  private def createTarget() = {
    new KaaSchemaRegistry(props, props, topic = TOPIC_NAME)
  }

  override protected def beforeAll(): Unit = {
    if (!admin.topicExists())
      admin.createTopic()
  }

  override protected def afterAll(): Unit = {
    admin.deleteTopic()
  }

  "KaaSchemaRegistry" should "put and retrieve a schema" in {
    val target = createTarget()

    try {
      val schema = AvroSchema[Foo]
      val schemaId = target.put(schema)

      target.get(schemaId) match {
        case None => fail("Schema not found")
        case Some(schemaRetrieved) => schemaRetrieved should be (schema)
      }
    } finally {
      target.close()
    }
  }

  it should "return None for not existing schema" in {
    val target = createTarget()

    try {
      target.get(SchemaId(999L)) match {
        case None => succeed
        case Some(_) => fail("Schema should not be retrieved")
      }
    } finally {
      target.close()
    }
  }

  case class Foo(name: String, age: Int) {}
}
