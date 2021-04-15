import java.util.UUID
import kaa.schemaregistry.{InvalidStateException, KaaSchemaRegistry, KaaSchemaRegistryAdmin, SchemaId}
import com.sksamuel.avro4s.AvroSchema
import org.scalatest._
import org.scalatest.flatspec._
import org.scalatest.matchers._

import scala.util.{Failure, Success, Try}

class KaaSchemaRegistrySpec extends AnyFlatSpec with should.Matchers with BeforeAndAfterAll {

  import scala.concurrent.ExecutionContext.Implicits.global

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

  "KaaSchemaRegistry" should "create" in {
    val _ = createTarget()
    succeed
  }

  it should "cannot put schema if not starter" in {
    val target = createTarget()
    val schema = AvroSchema[Foo]
    Try(target.put(schema)) match {
      case Success(_) => fail("Expected to fail")
      case Failure(_: InvalidStateException) => succeed
      case Failure(_) => fail("Expected to fail with InvalidStateException")
    }
  }

  it should "cannot get schema if not starter" in {
    val target = createTarget()
    target.get(SchemaId(999L)) match {
      case Some(_) => fail("Expected None")
      case None => succeed
    }
  }

  it should "put and get a schema" in {
    val target = createTarget()
    try {
      target.start(ex => println(ex))

      val schema = AvroSchema[Foo]
      val schemaId = target.put(schema)

      target.get(schemaId) match {
        case Some(schemaRetrieved) => schemaRetrieved should be (schema)
        case None => fail("Schema not found")
      }
    } finally {
      target.close()
    }
  }

  it should "put and get a schema then close and start again" in {
    val target = createTarget()
    try {
      target.start(ex => println(ex))

      val schema = AvroSchema[Foo]
      val schemaId = target.put(schema)

      target.get(schemaId) match {
        case Some(schemaRetrieved) => schemaRetrieved should be (schema)
        case None => fail("Schema not found")
      }

      target.close()
      target.get(schemaId) match {
        case Some(_) => fail("Expected None")
        case None => succeed
      }

      target.start(ex => println(ex))
      target.get(schemaId) match {
        case Some(schemaRetrieved) => schemaRetrieved should be (schema)
        case None => fail("Schema not found")
      }

    } finally {
      target.close()
    }
  }

  it should "return None for not existing schema" in {
    val target = createTarget()
    try {
      target.start(ex => println(ex))

      target.get(SchemaId(999L)) match {
        case Some(_) => fail("Schema should not be retrieved")
        case None => succeed
      }
    } finally {
      target.close()
    }
  }

  case class Foo(name: String, age: Int) {}
}
