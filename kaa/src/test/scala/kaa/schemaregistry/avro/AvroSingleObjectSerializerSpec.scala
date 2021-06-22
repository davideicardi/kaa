package kaa.schemaregistry.avro

import com.sksamuel.avro4s.AvroSchema
import org.apache.avro.{Schema, SchemaNormalization}
import org.scalatest._
import flatspec._
import kaa.schemaregistry.SchemaId
import kaa.schemaregistry.test.TestSchemaRegistry
import matchers._

import java.util.UUID

case class Pokemon(name: String, mainType: String, offType: Option[String], level: Int)

class AvroSingleObjectSerializerSpec extends AnyFlatSpec with should.Matchers {

  val registry = new TestSchemaRegistry

  "AvroSingleObjectSerializer" should "serialize and deserialize a case class" in {
    val target = new AvroSingleObjectSerializer[Pokemon](registry)

    val expected = Pokemon("Dragonite", "Dragon", None, 100)
    val encoded = target.serialize(expected)
    val decoded = target.deserialize(encoded)

    decoded.equals(expected) should be (true)
  }

  it should "serialize a class with a single object encoding" in {
    val target = new AvroSingleObjectSerializer[Pokemon](registry)

    val expected = Pokemon("Dragonite", "Dragon", None, 100)
    val encoded = target.serialize(expected)

    val (schemaId, bin) = AvroSingleObjectEncoding.AVRO_OFFICIAL.decode(encoded)
    val binarySerializer = new AvroBinarySerializer[Pokemon]

    schemaId should be (AvroUtils.calcFingerprint(AvroSchema[Pokemon]))
    bin should be (binarySerializer.write(expected))
  }

  it should "serialize and deserialize a primitive type: Long" in {
    val target = new AvroSingleObjectSerializer[Long](registry)

    val expected = 81L
    val encoded = target.serialize(expected)
    val decoded = target.deserialize(encoded)
    decoded should be (expected)

    val (schemaId, _) = AvroSingleObjectEncoding.AVRO_OFFICIAL.decode(encoded)
    schemaId should be (SchemaId(-3434872931120570953L))
    schemaId should be (AvroUtils.calcFingerprint(AvroSchema[Long]))
  }

  it should "serialize and deserialize a primitive type: String" in {
    val target = new AvroSingleObjectSerializer[String](registry)

    val expected = "hello world!"
    val encoded = target.serialize(expected)
    val decoded = target.deserialize(encoded)
    decoded should be (expected)

    val (schemaId, _) = AvroSingleObjectEncoding.AVRO_OFFICIAL.decode(encoded)
    schemaId should be (SchemaId(-8142146995180207161L))
    schemaId should be (AvroUtils.calcFingerprint(AvroSchema[String]))
  }

  it should "serialize and deserialize a primitive type: UUID" in {
    val target = new AvroSingleObjectSerializer[UUID](registry)

    val expected = UUID.randomUUID()
    val encoded = target.serialize(expected)
    val decoded = target.deserialize(encoded)
    decoded should be (expected)

    val (schemaId, _) = AvroSingleObjectEncoding.AVRO_OFFICIAL.decode(encoded)
    schemaId should be (SchemaId(-8142146995180207161L))
    schemaId should be (AvroUtils.calcFingerprint(AvroSchema[UUID]))
  }

  object AvroUtils {
    def calcFingerprint(schema: Schema): SchemaId = {
      SchemaId(SchemaNormalization.parsingFingerprint64(schema))
    }
  }
}
