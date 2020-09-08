package kaa.darwin

import com.sksamuel.avro4s.AvroSchema
import org.scalatest._
import flatspec._
import matchers._
import kaa.avro.AvroBinarySerializer

class AvroBinarySerializerSpec extends AnyFlatSpec with should.Matchers {

  "AvroBinarySerializer" should "serialize a case class" in {
    val target = new AvroBinarySerializer[FooUser]()

    val result = target.write(FooUser("foo"))

    val expected = Array("06", "66", "6f", "6f") // contains a string "foo"
      .map(Integer.parseInt(_, 16).toByte)

    result should equal (expected)
  }

  it should "serialize a case class V2" in {
    val target = new AvroBinarySerializer[FooUserV2]()

    val result = target.write(FooUserV2("foo", 1))

    val expected = Array("06", "66", "6f", "6f", "02") // contains a string "foo" and a long "1"
      .map(Integer.parseInt(_, 16).toByte)

    result should equal (expected)
  }

  it should "deserialize a case class" in {
    val target = new AvroBinarySerializer[FooUser]()

    val input = Seq("06", "66", "6f", "6f") // contains a string "foo"
      .map(Integer.parseInt(_, 16).toByte)
      .toArray

    val result = target.read(input, target.currentSchema)

    result.name should equal ("foo")
  }

  it should "get the current schema" in {
    val target = new AvroBinarySerializer[FooUser]()

    val schema = target.currentSchema

    //noinspection ScalaStyle
    val expectedSchema = """{"type":"record","name":"FooUser","namespace":"kaa.darwin.AvroBinarySerializerSpec","fields":[{"name":"name","type":"string"}]}"""

    schema.toString(false) should be (expectedSchema)
  }

  it should "deserialize a V1 byte array to V2 case class" in {
    val target = new AvroBinarySerializer[FooUserV2]()

    val input = Seq("06", "66", "6f", "6f") // contains a string "foo"
      .map(Integer.parseInt(_, 16).toByte)
      .toArray

    val result = target.read(input, AvroSchema[FooUser])

    result.name should equal ("foo")
    result.age should equal (25)
  }

  it should "deserialize a V2 byte array to V1 case class" in {
    val target = new AvroBinarySerializer[FooUser]()

    val input = Seq("06", "66", "6f", "6f", "02") // contains a string "foo" and a long "1"
      .map(Integer.parseInt(_, 16).toByte)
      .toArray

    val result = target.read(input, AvroSchema[FooUserV2])

    result.name should equal ("foo")
  }

  case class FooUser (name: String) {
  }

  case class FooUserV2 (name: String, age: Int = 25) {
  }
}
