package kaa.schemaregistry.avro

import com.sksamuel.avro4s.AvroSchema
import org.scalatest._
import flatspec._
import matchers._

import java.util.UUID

class BinarySerializerSpec extends AnyFlatSpec with should.Matchers {

  "AvroBinarySerializer" should "serialize a case class" in {
    val target = new BinarySerializer[FooUser]()

    val result = target.serialize(FooUser("foo"))

    val expected = Array("06", "66", "6f", "6f") // contains a string "foo"
      .map(Integer.parseInt(_, 16).toByte)

    result should equal (expected)
  }

  it should "serialize a case class V2" in {
    val target = new BinarySerializer[FooUserV2]()

    val result = target.serialize(FooUserV2("foo", 1))

    val expected = Array("06", "66", "6f", "6f", "02") // contains a string "foo" and a long "1"
      .map(Integer.parseInt(_, 16).toByte)

    result should equal (expected)
  }

  it should "deserialize a case class" in {
    val target = new BinarySerializer[FooUser]()

    val input = Seq("06", "66", "6f", "6f") // contains a string "foo"
      .map(Integer.parseInt(_, 16).toByte)
      .toArray

    val result = target.deserialize(input, target.currentSchema)

    result.name should equal ("foo")
  }

  it should "get the current schema of a case class" in {
    val target = new BinarySerializer[FooUser]()

    val schema = target.currentSchema

    //noinspection ScalaStyle
    val expectedSchema = """{"type":"record","name":"FooUser","namespace":"kaa.schemaregistry.avro.BinarySerializerSpec","fields":[{"name":"name","type":"string"}]}"""

    schema.toString(false) should be (expectedSchema)
  }

  it should "deserialize a V1 byte array to V2 case class" in {
    val target = new BinarySerializer[FooUserV2]()

    val input = Seq("06", "66", "6f", "6f") // contains a string "foo"
      .map(Integer.parseInt(_, 16).toByte)
      .toArray

    val result = target.deserialize(input, AvroSchema[FooUser])

    result.name should equal ("foo")
    result.age should equal (25)
  }

  it should "deserialize a V2 byte array to V1 case class" in {
    val target = new BinarySerializer[FooUser]()

    val input = Seq("06", "66", "6f", "6f", "02") // contains a string "foo" and a long "1"
      .map(Integer.parseInt(_, 16).toByte)
      .toArray

    val result = target.deserialize(input, AvroSchema[FooUserV2])

    result.name should equal ("foo")
  }

  it should "serialize and deserialize primitive type: string" in {
    val target = new BinarySerializer[String]()

    target.currentSchema.toString() should be ("\"string\"")

    val expected = "foo"
    val resultBytes = target.serialize(expected)

    val expectedBytes = Array("06", "66", "6f", "6f") // contains a string "foo"
      .map(Integer.parseInt(_, 16).toByte)

    resultBytes should equal (expectedBytes)

    val result = target.deserialize(resultBytes, target.currentSchema)
    result should equal (expected)
  }

  it should "serialize and deserialize primitive type: long" in {
    val target = new BinarySerializer[Long]()

    target.currentSchema.toString() should be ("\"long\"")

    val expected = 64L
    val resultBytes = target.serialize(expected)

    val expectedBytes = Array("80", "01") // int and long values are written using variable-length zig-zag coding
      .map(Integer.parseInt(_, 16).toByte)

    resultBytes should equal (expectedBytes)

    val result = target.deserialize(resultBytes, target.currentSchema)
    result should equal (expected)
  }

  it should "serialize and deserialize primitive type: UUID" in {
    val target = new BinarySerializer[UUID]()

    target.currentSchema.toString() should be ("{\"type\":\"string\",\"logicalType\":\"uuid\"}")

    val expected = UUID.fromString("1f04d1f3-2f8c-4743-85c7-9ac02328c32c")
    val resultBytes = target.serialize(expected)

    val expectedBytes = Array(
      72, 49, 102, 48, 52, 100, 49, 102, 51, 45, 50, 102, 56, 99, 45, 52, 55, 52,
      51, 45, 56, 53, 99, 55, 45, 57, 97, 99, 48, 50, 51, 50, 56, 99, 51, 50, 99)

    resultBytes should equal (expectedBytes)

    val result = target.deserialize(resultBytes, target.currentSchema)
    result should equal (expected)
  }

  case class FooUser (name: String) {
  }

  case class FooUserV2 (name: String, age: Int = 25) {
  }
}
