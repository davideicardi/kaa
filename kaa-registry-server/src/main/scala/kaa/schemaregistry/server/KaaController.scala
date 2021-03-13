package kaa.schemaregistry.server

import akka.http.scaladsl.model.StatusCodes
import akka.http.scaladsl.server.Directives._
import akka.http.scaladsl.server.Route
import kaa.schemaregistry.{SchemaId, SchemaRegistry}

import scala.concurrent.ExecutionContext

class KaaController(schemaRegistry: SchemaRegistry) extends RouteController {
  override def createRoute()(implicit executionContext: ExecutionContext): Route = {
    concat(
      get {
        path("") {
          complete("Kaa Registry Server")
        }
      },
      get {
        path("schemas" / "ids" / Segment) { id =>
          id.toLongOption.flatMap { idAsLong =>
            schemaRegistry.get(SchemaId(idAsLong))
              .map(schema => complete(schema.toString(true)))
          }.getOrElse(complete(StatusCodes.NotFound))
        }
      }
    )
  }
}
