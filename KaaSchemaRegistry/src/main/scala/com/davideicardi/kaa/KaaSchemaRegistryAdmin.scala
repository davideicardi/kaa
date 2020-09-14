package com.davideicardi.kaa

import org.apache.avro.Schema
import org.apache.kafka.clients.producer.{KafkaProducer, ProducerRecord}
import org.apache.avro.SchemaNormalization
import org.apache.kafka.clients.consumer.KafkaConsumer
import java.util.{Collections, Properties, UUID, Optional}
import java.util.concurrent.Executors
import java.util.concurrent.ExecutorService
import scala.jdk.CollectionConverters._
import com.github.blemale.scaffeine.{ Cache, Scaffeine }
import org.apache.kafka.clients.producer.ProducerConfig
import org.apache.kafka.clients.consumer.ConsumerConfig
import org.apache.kafka.common.serialization.{LongDeserializer, StringDeserializer}
import org.apache.kafka.common.serialization.{LongSerializer, StringSerializer}
import org.apache.kafka.clients.admin.{AdminClient, AdminClientConfig, NewTopic}
import org.apache.kafka.common.config.TopicConfig

class KaaSchemaRegistryAdmin(
  brokers: String,
  topic: String = KaaSchemaRegistry.DEFAULT_TOPIC_NAME
) {
  val props = new Properties()
  props.put(AdminClientConfig.BOOTSTRAP_SERVERS_CONFIG, brokers)
  props.put("delete.enable.topic", "true")
  private val adminClient = AdminClient.create(props)

  def createTopic(): Unit = {
    val newTopic = new NewTopic(topic, Optional.empty[java.lang.Integer](), Optional.empty[java.lang.Short]())
    val newTopicsConfigs = Map (
      TopicConfig.CLEANUP_POLICY_CONFIG -> TopicConfig.CLEANUP_POLICY_COMPACT
    )
    newTopic.configs(newTopicsConfigs.asJava)

    adminClient.createTopics(Collections.singletonList(newTopic)).all().get()
  }

  def topicExists(): Boolean = {
    val names = adminClient.listTopics.names.get.asScala
    names.contains(topic)
  }

  def deleteTopic(): Unit = {
    adminClient.deleteTopics(Collections.singletonList(topic)).all().get()
  }
}