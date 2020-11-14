package com.davideicardi.kaa.darwin

import org.scalatest._
import flatspec._
import matchers._
import com.davideicardi.kaa.kafka.GenericSerde
import com.davideicardi.kaa.test.TestSchemaRegistry
import com.sksamuel.avro4s.AvroSortPriority

class GenericSerdeSpec extends AnyFlatSpec with should.Matchers {

  val registry = new TestSchemaRegistry

  "GenericSerde" should "serialize and deserialize a case class" in {
    val target = new GenericSerde[FooUser](registry)

    val expected = FooUser("foo")
    val bytes = target.serialize("topic", expected)
    val result = target.deserialize("topic", bytes)

    result should equal (expected)
  }

  it should "serialize and deserialize an Option Some" in {
    val target = new GenericSerde[Option[FooUser]](registry)

    val expected = Some(FooUser("foo"))
    val bytes = target.serialize("topic", expected)
    val result = target.deserialize("topic", bytes)
    result should equal (expected)
  }

  it should "serialize and deserialize an Option None" in {
    val target = new GenericSerde[Option[FooUser]](registry)

    val expected: Option[FooUser] = None
    val bytes = target.serialize("topic", expected)
    val result = target.deserialize("topic", bytes)
    result should equal (expected)
  }

  it should "serialize and deserialize a null" in {
    val target = new GenericSerde[FooUser](registry)

    val expected: FooUser = null
    val bytes = target.serialize("topic", expected)
    val result = target.deserialize("topic", bytes)

    result should equal (expected)
  }

  it should "serialize sealed trait of case classes using a different sealed trait" in {
    val v2 = new GenericSerde[serviceV2.Fruit](registry)
    val bytes = v2.serialize("topic", serviceV2.Mango(81, "green"))

    val v1 = new GenericSerde[serviceV1.Fruit](registry)
    val result = v1.deserialize("topic", bytes)

    result should equal (serviceV1.Mango(81))
  }

  it should "serialize sealed trait of case classes using first element in case there isn't a match" in {
    val v2 = new GenericSerde[serviceV2.Fruit](registry)
    val bytes = v2.serialize("topic", serviceV2.Banana("yellow"))

    val v1 = new GenericSerde[serviceV1.Fruit](registry)
    val result = v1.deserialize("topic", bytes)

    result should equal (serviceV1.Unknown)
  }

  case class FooUser (name: String) {
  }
}

object serviceV1 {
  sealed trait Fruit
  @AvroSortPriority(Float.MaxValue)
  case object Unknown extends Fruit
  case class Mango(size: Int) extends Fruit
  case class Orange(color: String) extends Fruit
  case class Apple(angle: Double) extends Fruit
}

object serviceV2 {
  sealed trait Fruit
  @AvroSortPriority(Float.MaxValue)
  case object Unknown extends Fruit
  case class Mango(size: Int, color: String) extends Fruit
  case class Orange(color: String) extends Fruit
  case class Apple(angle: Double) extends Fruit
  case class Banana(color: String) extends Fruit
}
