package kaa.schemaregistry.kafka

import org.scalatest._
import flatspec._
import matchers._
import kaa.schemaregistry.test.TestSchemaRegistry

import java.util.UUID

class KaaSerdeSpec extends AnyFlatSpec with should.Matchers {

  val registry = new TestSchemaRegistry

  "KaaSerde" should "serialize and deserialize a case class" in {
    val target = new KaaSerde[FooUser](registry)

    val expected = FooUser("foo")
    val bytes = target.serialize("topic", expected)
    val result = target.deserialize("topic", bytes)

    result should equal (expected)
  }

  it should "serialize and deserialize an Option Some" in {
    val target = new KaaSerde[Option[FooUser]](registry)

    val expected = Some(FooUser("foo"))
    val bytes = target.serialize("topic", expected)
    val result = target.deserialize("topic", bytes)
    result should equal (expected)
  }

  it should "serialize and deserialize an Option None" in {
    val target = new KaaSerde[Option[FooUser]](registry)

    val expected: Option[FooUser] = None
    val bytes = target.serialize("topic", expected)
    val result = target.deserialize("topic", bytes)
    result should equal (expected)
  }

  // tomb stone support
  it should "serialize and deserialize a case class from null" in {
    val target = new KaaSerde[FooUser](registry)

    val expected: FooUser = null
    val bytes = target.serialize("topic", expected)
    val result = target.deserialize("topic", bytes)

    result should equal (expected)
  }

  it should "serialize and deserialize primitive type: String" in {
    val target = new KaaSerde[String](registry)

    val expected = "hello world"
    val bytes = target.serialize("topic", expected)
    val result = target.deserialize("topic", bytes)

    result should equal (expected)
  }

  it should "serialize and deserialize primitive type: Long" in {
    val target = new KaaSerde[Long](registry)

    val expected = 81L
    val bytes = target.serialize("topic", expected)
    val result = target.deserialize("topic", bytes)

    result should equal (expected)
  }

  it should "serialize and deserialize primitive type: UUID" in {
    val target = new KaaSerde[UUID](registry)

    val expected = UUID.randomUUID()
    val bytes = target.serialize("topic", expected)
    val result = target.deserialize("topic", bytes)

    result should equal (expected)
  }

  it should "deserialize null to primitive type: Long" in {
    val target = new KaaSerde[Long](registry)
    target.deserialize("topic", Array()) == 0L should be (true)
    target.deserialize("topic", null) == 0L should be (true)
  }

  it should "deserialize null to primitive type: String" in {
    val target = new KaaSerde[String](registry)
    target.deserialize("topic", Array()) should be (null)
    target.deserialize("topic", null) should be (null)
  }

  it should "deserialize null to primitive type: UUID" in {
    val target = new KaaSerde[UUID](registry)
    target.deserialize("topic", Array()) should be (null)
    target.deserialize("topic", null) should be (null)
  }

  case class FooUser (name: String) {
  }
}
