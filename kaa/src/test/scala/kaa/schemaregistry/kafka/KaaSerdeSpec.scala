package kaa.schemaregistry.kafka

import org.scalatest._
import flatspec._
import matchers._
import kaa.schemaregistry.test.TestSchemaRegistry

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

  it should "serialize and deserialize a null" in {
    val target = new KaaSerde[FooUser](registry)

    val expected: FooUser = null
    val bytes = target.serialize("topic", expected)
    val result = target.deserialize("topic", bytes)

    result should equal (expected)
  }

  case class FooUser (name: String) {
  }
}