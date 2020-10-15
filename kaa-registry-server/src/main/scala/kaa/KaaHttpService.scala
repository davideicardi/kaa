package kaa

import akka.actor.ActorSystem
import akka.http.scaladsl.Http
import akka.http.scaladsl.server.Directives._
import akka.http.scaladsl.server.Route

import scala.concurrent._

trait RouteController {
  def createRoute()(implicit executionContext: ExecutionContext): Route
}

class KaaHttpServer(
                      host: String,
                      port: Int,
                      controllers: Seq[RouteController],
                    )
                    (implicit system: ActorSystem, executionContext: ExecutionContext){
  private var bindingFuture: Option[Future[Http.ServerBinding]] = None

  def start(): Unit = {
    val route = concat(controllers.map(_.createRoute()):_*)

    bindingFuture = Some {
      Http().newServerAt(host, port)
        .bindFlow(route)
    }
    println(s"Server online at http://${host}:${port}/\n")

    Runtime.getRuntime.addShutdownHook(new Thread(() => {
      stop()
    }))
  }

  def stop(): Unit = {
    bindingFuture
      .foreach(_
        .flatMap(_.unbind()) // trigger unbinding from the port
        .onComplete(_ => system.terminate()) // and shutdown when done
      )
    bindingFuture = None
  }
}
