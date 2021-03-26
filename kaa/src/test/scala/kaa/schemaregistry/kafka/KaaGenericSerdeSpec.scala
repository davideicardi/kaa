package kaa.schemaregistry.kafka

import kaa.schemaregistry.test.TestSchemaRegistry
import org.apache.avro.generic.GenericData
import org.scalatest.flatspec._
import org.scalatest.matchers._

class KaaGenericSerdeSpec extends AnyFlatSpec with should.Matchers {

  private val registry = new TestSchemaRegistry

  private val schema =
    """
      |{"namespace": "example.avro",
      | "type": "record",
      | "name": "User",
      | "fields": [
      |     {"name": "name", "type": "string"},
      |     {"name": "favorite_number",  "type": "int"},
      |     {"name": "favorite_color", "type": "string"}
      | ]
      |}
    """.stripMargin

  "KaaGenericSerde" should "serialize and deserialize a generic record" in {
    val target = new KaaGenericSerde(registry)

    val schemaObj = new org.apache.avro.Schema.Parser().parse(schema)

    val user1 = new GenericData.Record(schemaObj)
    user1.put("name", "Alyssa")
    user1.put("favorite_number", 256)
    user1.put("favorite_color", "blue")

    val bytes = target.serialize("topic", user1)
    val result = target.deserialize("topic", bytes)

    result.toString should equal ("{\"name\": \"Alyssa\", \"favorite_number\": 256, \"favorite_color\": \"blue\"}")
  }

}
