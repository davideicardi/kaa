package com.davideicardi.kaa

import java.util.{Collections, Properties, Optional}
import org.apache.kafka.clients.admin.{AdminClient, NewTopic}
import org.apache.kafka.common.config.TopicConfig

import com.davideicardi.kaa.utils.CollectionConverters

class KaaSchemaRegistryAdmin(
  adminProps: Properties,
  topic: String = KaaSchemaRegistry.DEFAULT_TOPIC_NAME
) {
  def this(brokers: String) = {
    this(
      KaaSchemaRegistry.createProps(brokers, KaaSchemaRegistry.DEFAULT_CLIENT_ID),
      topic = KaaSchemaRegistry.DEFAULT_TOPIC_NAME,
    )
  }

  adminProps.putIfAbsent("delete.enable.topic", "true")
  private val adminClient = AdminClient.create(adminProps)

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

  def close(): Unit = {
    adminClient.close()
  }
}