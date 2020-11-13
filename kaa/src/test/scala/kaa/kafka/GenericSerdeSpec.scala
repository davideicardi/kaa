package com.davideicardi.kaa.darwin

import org.scalatest._
import flatspec._
import matchers._
import com.davideicardi.kaa.kafka.GenericSerde
import com.davideicardi.kaa.test.TestSchemaRegistry
import com.sksamuel.avro4s.AvroSchema

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
    println(AvroSchema[serviceV1.Fruit].toString(true))
    println("---------")
    println(AvroSchema[serviceV2.Fruit].toString(true))

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


/**
 * Sealed case classes are serialized in AVRO using unions. AVRO's unions are "exclusive".
 * So when deserializing the first element of the union that is compatible will be used.
 * This will generate a problem when we want to evolve unions (adding/removing types).
 * For this reason we have implemented the following workarounds:
 * - we always create an Unknown object without fields, this will be used to match any "not compatible" type
 * - we add a special field with an unique name (t1, t2, t3) so that a type can match only the corresponding type or unknown
 * - this special type is called AvroId (internally an Boolean to reduce size) will be set always to 0
 * - this field is set to a default value to allow to not set it when working with code but is marked as AvroNoDefault to not store the default value in avro
*/
object AvroUnions {
  type id = com.sksamuel.avro4s.AvroNoDefault
  type AvroId = Boolean
  val me = false
}

import AvroUnions._

object serviceV1 {
  sealed trait Fruit
  case object Unknown extends Fruit
  case class Mango(size: Int, @id t1: AvroId = me) extends Fruit
  case class Orange(color: String, @id t2: AvroId = me) extends Fruit
  case class Apple(angle: Double, @id t3: AvroId = me) extends Fruit
}

object serviceV2 {
  sealed trait Fruit
  case object Unknown extends Fruit
  case class Mango(size: Int, color: String, @id t1: AvroId = me) extends Fruit
  case class Orange(color: String, @id t2: AvroId = me) extends Fruit
  case class Apple(angle: Double, @id t3: AvroId = me) extends Fruit
  case class Banana(color: String, @id t4: AvroId = me) extends Fruit
}
