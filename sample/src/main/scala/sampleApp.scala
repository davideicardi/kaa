import com.davideicardi.kaa.avro.AvroSingleObjectSerializer
import com.davideicardi.kaa.KaaSchemaRegistry
import com.davideicardi.kaa.KaaSchemaRegistryAdmin

object SampleApp {
    
    def main(args: Array[String]): Unit = {
        println("KaaSchemaRegistry SampleApp")

        val brokers = "localhost:9092"

        val admin = new KaaSchemaRegistryAdmin(brokers)
        if (!admin.topicExists()) admin.createTopic()

        val schemaRegistry = new KaaSchemaRegistry(brokers)
        try {
            val serializerV1 = new AvroSingleObjectSerializer[SuperheroV1](schemaRegistry)
            val serializerV2 = new AvroSingleObjectSerializer[SuperheroV2](schemaRegistry)

            val bytesV1 = serializerV1.serialize(SuperheroV1("Spiderman"))
            val bytesV2 = serializerV2.serialize(SuperheroV2("Spiderman", "Peter Parker"))

            // normal deserialization
            val resultV1V1 = serializerV1.deserialize(bytesV1)
            println(resultV1V1)
            val resultV2V2 = serializerV2.deserialize(bytesV2)
            println(resultV2V2)
            // forward compatibility
            val resultV1V2 = serializerV1.deserialize(bytesV2)
            println(resultV1V2)
            // backward compatibility
            val resultV2V1 = serializerV2.deserialize(bytesV1)
            println(resultV2V1)

        } finally {
            schemaRegistry.shutdown()
        }
    }
}

case class SuperheroV1(name: String)
case class SuperheroV2(name: String, realName: String = "")
