import kaa.schemaregistry.{KaaSchemaRegistry, KaaSchemaRegistryAdmin}
import kaa.schemaregistry.avro.{AvroSingleObjectEncoding, SingleObjectSerializer}
import kaa.schemaregistry.kafka.KaaSerde
import org.apache.kafka.clients.CommonClientConfigs
import org.apache.kafka.clients.admin.{AdminClient, NewTopic}
import org.apache.kafka.clients.producer.{KafkaProducer, ProducerRecord}

import java.util.{Collections, Optional, Properties}
import scala.concurrent.ExecutionContext.Implicits.global

object SampleApp {

    private val brokers = sys.env.getOrElse("KAFKA_BROKERS", "localhost:9092")

    def main(args: Array[String]): Unit = {
        println("KaaSchemaRegistry SampleApp")

        val admin = new KaaSchemaRegistryAdmin(brokers)
        try {
            if (!admin.topicExists()) admin.createTopic()
        } finally {
            admin.close()
        }

        val schemaRegistry = new KaaSchemaRegistry(brokers)
        try {
            schemaRegistry.start(e => println(e))

            testSchemaRegistry(schemaRegistry)

            testKafkaSerde(schemaRegistry)

        } finally {
            schemaRegistry.close()
        }
    }

    private def testSchemaRegistry(schemaRegistry: KaaSchemaRegistry): Unit = {
        val serializerV1 = new SingleObjectSerializer[SuperheroV1](schemaRegistry)
        val serializerV2 = new SingleObjectSerializer[SuperheroV2](schemaRegistry)

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
    }

    private def testKafkaSerde(schemaRegistry: KaaSchemaRegistry): Unit = {
        val topic = "kaa-sample-superheroes"
        val adminProps = new Properties()
        adminProps.put(CommonClientConfigs.BOOTSTRAP_SERVERS_CONFIG, brokers)
        val adminClient = AdminClient.create(adminProps)
        try {
            val names = adminClient.listTopics().names().get()
            if (!names.contains(topic)) {
                println("Creating sample topic ...")
                val NUMBER_OF_PARTITIONS = 1
                val newTopic = new NewTopic(
                    topic,
                    Optional.of[java.lang.Integer](NUMBER_OF_PARTITIONS),
                    Optional.empty[java.lang.Short]()
                )
                val _ = adminClient.createTopics(Collections.singletonList(newTopic)).all().get()
            }
        } finally {
            adminClient.close()
        }


        println("Writing kafka record...")
        val producerProps = new Properties()
        producerProps.put(CommonClientConfigs.BOOTSTRAP_SERVERS_CONFIG, brokers)
        producerProps.put(CommonClientConfigs.CLIENT_ID_CONFIG, "sample-kaa")
        val producer = new KafkaProducer(producerProps, new KaaSerde[Long](schemaRegistry), new KaaSerde[SuperheroV2](schemaRegistry))
        try {
            val record = new ProducerRecord[Long, SuperheroV2](topic, 0, SuperheroV2("spider", "Parker"))
            producer.send(record).get()
            println("Kafka record written")
        } finally {
            producer.close()
        }
    }
}

case class SuperheroV1(name: String)
case class SuperheroV2(name: String, realName: String = "")
