package com.davideicardi.kaa.darwin

import com.sksamuel.avro4s.AvroSchema
import org.apache.avro.{Schema, SchemaNormalization}
import org.scalatest._
import flatspec._
import com.davideicardi.kaa.avro.{AvroBinarySerializer, AvroSingleObjectEncoding, AvroSingleObjectSerializer}
import com.davideicardi.kaa.SchemaId
import com.davideicardi.kaa.test.TestSchemaRegistry
import matchers._

case class Pokemon(name: String, mainType: String, offType: Option[String], level: Int)

class AvroSingleObjectSerializerSpec extends AnyFlatSpec with should.Matchers {

  val registry = new TestSchemaRegistry
  val dragonite = Pokemon("Dragonite", "Dragon", None, 100)

  val singleObjectSerializer = new AvroSingleObjectSerializer[Pokemon](registry)

  "AvroSingleObjectSerializer" should "serialize and deserialize a case class" in {
    val encoded = singleObjectSerializer.serialize(dragonite)
    val decoded = singleObjectSerializer.deserialize(encoded)

    decoded.equals(dragonite) should be (true)
  }

  it should "serialize a class with a single object encoding" in {
    val encoded = singleObjectSerializer.serialize(dragonite)

    val (schemaId, bin) = AvroSingleObjectEncoding.default.decode(encoded)
    val binarySerializer = new AvroBinarySerializer[Pokemon]

    schemaId should be (AvroUtils.calcFingerprint(AvroSchema[Pokemon]))
    bin should be (binarySerializer.write(dragonite))
  }

  object AvroUtils {
    def calcFingerprint(schema: Schema): SchemaId = {
      SchemaId(SchemaNormalization.parsingFingerprint64(schema))
    }
  }
}
