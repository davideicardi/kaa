package com.davideicardi.kaa

import java.util.{Collections, Properties, Optional}
import org.apache.kafka.clients.admin.{AdminClient, AdminClientConfig, NewTopic}
import org.apache.kafka.common.config.TopicConfig

import com.davideicardi.kaa.utils.CollectionConverters

class KaaSchemaRegistryAdmin(
  brokers: String,
  topic: String = KaaSchemaRegistry.DEFAULT_TOPIC_NAME
) {
  val props = new Properties()
  props.put(AdminClientConfig.BOOTSTRAP_SERVERS_CONFIG, brokers)
  props.put("delete.enable.topic", "true")
  private val adminClient = AdminClient.create(props)

  def createTopic(): Unit = {
    val NUMBER_OF_PARTITIONS = 1
    val newTopic = new NewTopic(
      topic,
      Optional.of[java.lang.Integer](NUMBER_OF_PARTITIONS),
      Optional.empty[java.lang.Short]()
    )
    val newTopicsConfigs = Map (
      TopicConfig.CLEANUP_POLICY_CONFIG -> TopicConfig.CLEANUP_POLICY_COMPACT
    )
    newTopic.configs(CollectionConverters.mapAsJava(newTopicsConfigs))

    val _ = adminClient.createTopics(Collections.singletonList(newTopic)).all().get()
  }

  def topicExists(): Boolean = {
    val names = adminClient.listTopics.names.get
    names.contains(topic)
  }

  def deleteTopic(): Unit = {
    val _ = adminClient.deleteTopics(Collections.singletonList(topic)).all().get()
  }
}