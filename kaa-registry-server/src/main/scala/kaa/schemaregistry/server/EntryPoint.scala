package kaa.schemaregistry.server

import java.util.concurrent.CountDownLatch

import akka.actor.ActorSystem
import kaa.schemaregistry.{KaaSchemaRegistry, KaaSchemaRegistryAdmin}

import scala.concurrent.{ExecutionContext, ExecutionContextExecutor}

object EntryPoint extends App {
  println("Kaa Schema Registry Server")

  val brokers = sys.env.getOrElse("KAFKA_BROKERS", "localhost:9092")
  val interface = sys.env.getOrElse("INTERFACE", "localhost")
  val port = sys.env.getOrElse("PORT", "8888").toInt
  val appName = sys.env.getOrElse("APP_NAME", "kaa-registry-server")
  val props = KaaSchemaRegistry.createProps(brokers, appName)

  val admin = new KaaSchemaRegistryAdmin(props)
  try {
    if (!admin.topicExists()) admin.createTopic()
  } finally {
    admin.close()
  }

  implicit val system: ActorSystem = ActorSystem(appName)
  implicit val ec: ExecutionContextExecutor = ExecutionContext.global

  val doneSignal = new CountDownLatch(1)

  val schemaRegistry = new KaaSchemaRegistry(brokers)
  try {
    val controller = new KaaController(schemaRegistry)

    val httpServer = new KaaHttpServer(
      interface,
      port,
      Seq(controller)
    )

    httpServer.start()

    Runtime.getRuntime.addShutdownHook(new Thread(() => {
      doneSignal.countDown()
      httpServer.stop()
    }))

    doneSignal.await()
  } finally {
    schemaRegistry.close()
  }
}
