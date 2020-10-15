package kaa

import java.util.concurrent.CountDownLatch

import akka.actor.ActorSystem
import com.davideicardi.kaa.{KaaSchemaRegistry, KaaSchemaRegistryAdmin}

import scala.concurrent.{ExecutionContext, ExecutionContextExecutor}

object EntryPoint extends App {
  println("Kaa Schema Registry Server")

  val brokers = "localhost:9092"
  val host = "localhost"
  val appName = "kaa-registry-server"
  val port = 8888

  val admin = new KaaSchemaRegistryAdmin(brokers)
  if (!admin.topicExists()) admin.createTopic()

  implicit val system: ActorSystem = ActorSystem(appName)
  implicit val ec: ExecutionContextExecutor = ExecutionContext.global

  val doneSignal = new CountDownLatch(1)

  val schemaRegistry = new KaaSchemaRegistry(brokers)
  try {
    val controller = new KaaController(schemaRegistry)

    val httpServer = new KaaHttpServer(
      host,
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
    schemaRegistry.shutdown()
  }
}
