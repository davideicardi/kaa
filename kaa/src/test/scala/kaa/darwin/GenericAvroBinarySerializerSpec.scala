package kaa.darwin

import com.davideicardi.kaa.avro.GenericAvroBinarySerializer
import org.apache.avro.Schema
import org.apache.avro.generic.GenericData
import org.scalatest.flatspec._
import org.scalatest.matchers._

class GenericAvroBinarySerializerSpec extends AnyFlatSpec with should.Matchers {
  val target = new GenericAvroBinarySerializer()

  private val schemaV1 = new Schema.Parser().parse(
    """
      |{
      | "namespace": "test.avro",
      | "type": "record",
      | "name": "FooUser",
      | "fields": [
      |     {"name": "name", "type": "string"}
      | ]
      |}
      """.stripMargin
  )

  private val schemaV2 = new Schema.Parser().parse(
    """
      |{
      | "namespace": "test.avro",
      | "type": "record",
      | "name": "FooUser",
      | "fields": [
      |     {"name": "name", "type": "string"},
      |     {"name": "age",  "type": "int", "default": 25}
      | ]
      |}
      """.stripMargin
  )

  "GenericAvroBinarySerializer" should "serialize a record" in {
    val record = new GenericData.Record(schemaV1)
    record.put("name", "foo")

    val result = target.write(record)

    val expected = Array("06", "66", "6f", "6f") // contains a string "foo"
      .map(Integer.parseInt(_, 16).toByte)

    result should equal (expected)
  }

  it should "serialize a record V2" in {
    val record = new GenericData.Record(schemaV2)
    record.put("name", "foo")
    record.put("age", 1)

    val result = target.write(record)

    val expected = Array("06", "66", "6f", "6f", "02") // contains a string "foo" and a long "1"
      .map(Integer.parseInt(_, 16).toByte)

    result should equal (expected)
  }

  it should "deserialize to a record" in {
    val input = Seq("06", "66", "6f", "6f") // contains a string "foo"
      .map(Integer.parseInt(_, 16).toByte)
      .toArray

    val result = target.read(input, schemaV1)

    result.get("name").toString should equal ("foo")
  }

  it should "deserialize a V1 byte array to V2 record" in {
    val input = Seq("06", "66", "6f", "6f") // contains a string "foo"
      .map(Integer.parseInt(_, 16).toByte)
      .toArray

    val result = target.read(input, schemaV1, Some(schemaV2))

    result.get("name").toString should equal ("foo")
    result.get("age") should equal (25) // default value
  }

  it should "deserialize a V2 byte array to V1 record" in {
    val input = Seq("06", "66", "6f", "6f", "02") // contains a string "foo" and a long "1"
      .map(Integer.parseInt(_, 16).toByte)
      .toArray

    val result = target.read(input, schemaV2)

    result.get("name").toString should equal ("foo")
  }
}
