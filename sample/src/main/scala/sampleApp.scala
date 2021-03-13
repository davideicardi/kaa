import kaa.schemaregistry.avro.{AvroSingleObjectEncoding, AvroSingleObjectSerializer}
import kaa.schemaregistry.KaaSchemaRegistry
import kaa.schemaregistry.KaaSchemaRegistryAdmin

object SampleApp {
    
    def main(args: Array[String]): Unit = {
        println("KaaSchemaRegistry SampleApp")

        val brokers = sys.env.getOrElse("KAFKA_BROKERS", "localhost:9092")

        val admin = new KaaSchemaRegistryAdmin(brokers)
        try {
            if (!admin.topicExists()) admin.createTopic()
        } finally {
            admin.close()
        }

        val schemaRegistry = new KaaSchemaRegistry(brokers)
        try {
            val serializerV1 = new AvroSingleObjectSerializer[SuperheroV1](schemaRegistry)
            val serializerV2 = new AvroSingleObjectSerializer[SuperheroV2](schemaRegistry)

            val bytesV1 = serializerV1.serialize(SuperheroV1("Spiderman"))
            val bytesV2 = serializerV2.serialize(SuperheroV2("Spiderman", "Peter Parker"))

            // v1 schema
            val (schemaIdv1, _) = AvroSingleObjectEncoding.AVRO_OFFICIAL.decode(bytesV1)
            println(s"v1 $schemaIdv1")
            val (schemaIdv2, _) = AvroSingleObjectEncoding.AVRO_OFFICIAL.decode(bytesV2)
            println(s"v2 $schemaIdv2")

            // normal deserialization
            val resultV1V1 = serializerV1.deserialize(bytesV1)
            println(s"V1 -> V1 $resultV1V1")
            val resultV2V2 = serializerV2.deserialize(bytesV2)
            println(s"V2 -> V2 $resultV2V2")
            // forward compatibility
            val resultV1V2 = serializerV1.deserialize(bytesV2)
            println(s"V2 -> V1 $resultV1V2")
            // backward compatibility
            val resultV2V1 = serializerV2.deserialize(bytesV1)
            println(s"V1 -> V2 $resultV2V1")

        } finally {
            schemaRegistry.close()
        }
    }
}

case class SuperheroV1(name: String)
case class SuperheroV2(name: String, realName: String = "")
