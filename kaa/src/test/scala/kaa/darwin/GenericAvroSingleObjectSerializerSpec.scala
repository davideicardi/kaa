package kaa.darwin

import com.davideicardi.kaa.SchemaId
import com.davideicardi.kaa.avro.{AvroSingleObjectEncoding, GenericAvroBinarySerializer, GenericAvroSingleObjectSerializer}
import com.davideicardi.kaa.test.TestSchemaRegistry
import org.apache.avro.generic.GenericData
import org.apache.avro.{Schema, SchemaNormalization}
import org.scalatest.flatspec._
import org.scalatest.matchers._

class GenericAvroSingleObjectSerializerSpec extends AnyFlatSpec with should.Matchers {

  val registry = new TestSchemaRegistry

  val singleObjectSerializer = new GenericAvroSingleObjectSerializer(registry)

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

  "GenericAvroSingleObjectSerializer" should "serialize and deserialize a record" in {
    val record = new GenericData.Record(schemaV1)
    record.put("name", "foo")

    val encoded = singleObjectSerializer.serialize(record)
    val decoded = singleObjectSerializer.deserialize(encoded)

    decoded.get("name").toString should equal ("foo")
  }

  it should "serialize and deserialize a record passing an expected record" in {
    val record = new GenericData.Record(schemaV1)
    record.put("name", "foo")

    val encoded = singleObjectSerializer.serialize(record)
    val decoded = singleObjectSerializer.deserialize(encoded, Some(schemaV2))

    decoded.get("name").toString should equal ("foo")
    decoded.get("age") should equal (25)
  }

  it should "serialize a class with a single object encoding" in {
    val record = new GenericData.Record(schemaV1)
    record.put("name", "foo")

    val encoded = singleObjectSerializer.serialize(record)

    val (schemaId, bin) = AvroSingleObjectEncoding.default.decode(encoded)
    val binarySerializer = new GenericAvroBinarySerializer

    schemaId should be (AvroUtils.calcFingerprint(schemaV1))
    bin should be (binarySerializer.write(record))
  }

  object AvroUtils {
    def calcFingerprint(schema: Schema): SchemaId = {
      SchemaId(SchemaNormalization.parsingFingerprint64(schema))
    }
  }
}
