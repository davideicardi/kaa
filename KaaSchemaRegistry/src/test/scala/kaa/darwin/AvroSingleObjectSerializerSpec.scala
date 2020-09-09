package kaa.darwin

import com.sksamuel.avro4s.AvroSchema
import org.apache.avro.{Schema, SchemaNormalization}
import org.scalatest._
import flatspec._
import kaa.avro.{AvroBinarySerializer, AvroSingleObjectEncoding, AvroSingleObjectSerializer}
import kaa.{SchemaId, SchemaRegistry}
import matchers._

import scala.collection.mutable

case class Pokemon(name: String, mainType: String, offType: Option[String], level: Int)

class AvroSingleObjectSerializerSpec extends AnyFlatSpec with should.Matchers {

  val registry = new SchemaRegistryFake
  val dragonite = Pokemon("Dragonite", "Dragon", None, 100)

  val singleObjectSerializer = new AvroSingleObjectSerializer[Pokemon](registry)

  "AvroSingleObjectSerializer" should "serialize and deserialize a case class" in {
    val encoded = singleObjectSerializer.serialize(dragonite)
    val decoded = singleObjectSerializer.deserialize(encoded)

    decoded.equals(dragonite) should be (true)
  }

  it should "serialize a class with a single object encoding" in {
    val encoded = singleObjectSerializer.serialize(dragonite)

    val (schemaId, bin) = AvroSingleObjectEncoding.decode(encoded)
    val binarySerializer = new AvroBinarySerializer[Pokemon]

    schemaId should be (AvroUtils.calcFingerprint(AvroSchema[Pokemon]))
    bin should be (binarySerializer.write(dragonite))
  }

  // TODO add tests to verify that it is backward and forward compatibile
  // add tests to verify what's happening when schema is not found

  object AvroUtils {
    def calcFingerprint(schema: Schema): SchemaId = {
      SchemaId(SchemaNormalization.parsingFingerprint64(schema))
    }
  }
}

class SchemaRegistryFake extends SchemaRegistry {
  val _schemas = new mutable.HashMap[SchemaId, Schema]

  override def put(schema: Schema): SchemaId = {
    val id = SchemaId(
      SchemaNormalization.parsingFingerprint64(schema)
    )
    _schemas.put(id, schema)

    id
  }

  override def get(id: SchemaId): Option[Schema] = {
    _schemas.get(id)
  }
}