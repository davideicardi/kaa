package kaa

import java.util.concurrent.CountDownLatch

import akka.actor.ActorSystem
import com.davideicardi.kaa.{KaaSchemaRegistry, KaaSchemaRegistryAdmin}

import scala.concurrent.{ExecutionContext, ExecutionContextExecutor}

object EntryPoint extends App {
  println("Kaa Schema Registry Server")

  val brokers = sys.env.getOrElse("KAFKA_BROKERS", "localhost:9092")
  val interface = sys.env.getOrElse("INTERFACE", "localhost")
  val port = sys.env.getOrElse("PORT", "8888").toInt
  val appName = sys.env.getOrElse("CLIENT_ID", "kaa-registry-server")

  val admin = new KaaSchemaRegistryAdmin(brokers)
  if (!admin.topicExists()) admin.createTopic()

  implicit val system: ActorSystem = ActorSystem(appName)
  implicit val ec: ExecutionContextExecutor = ExecutionContext.global

  val doneSignal = new CountDownLatch(1)

  val schemaRegistry = KaaSchemaRegistry.create(brokers, appName)
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
    schemaRegistry.shutdown()
  }
}
