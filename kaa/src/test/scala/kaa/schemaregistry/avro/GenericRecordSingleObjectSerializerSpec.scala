package kaa.schemaregistry.avro

import kaa.schemaregistry.SchemaId
import kaa.schemaregistry.test.TestSchemaRegistry
import org.apache.avro.generic.GenericData
import org.apache.avro.{Schema, SchemaNormalization}
import org.scalatest.flatspec._
import org.scalatest.matchers._

class GenericRecordSingleObjectSerializerSpec extends AnyFlatSpec with should.Matchers {

  val registry = new TestSchemaRegistry

  val singleObjectSerializer = new GenericRecordSingleObjectSerializer(registry)

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

    val (schemaId, bin) = AvroSingleObjectEncoding.AVRO_OFFICIAL.decode(encoded)
    val binarySerializer = new GenericRecordBinarySerializer

    schemaId should be (AvroUtils.calcFingerprint(schemaV1))
    bin should be (binarySerializer.serialize(record))
  }

  object AvroUtils {
    def calcFingerprint(schema: Schema): SchemaId = {
      SchemaId(SchemaNormalization.parsingFingerprint64(schema))
    }
  }
}
