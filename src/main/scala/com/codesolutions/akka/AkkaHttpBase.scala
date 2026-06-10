package com.codesolutions.akka

import akka.actor.typed.ActorSystem
import akka.actor.typed.scaladsl.Behaviors
import akka.http.scaladsl.Http
import akka.http.scaladsl.marshallers.sprayjson.SprayJsonSupport._
import akka.http.scaladsl.model._
import akka.http.scaladsl.server.Directives._
import akka.http.scaladsl.server.Route
import spray.json._

import com.codesolutions.akka.http.{JsonProtocols, OrderRoutes}
import com.codesolutions.akka.persistence.InMemoryOrderService

/**
 * Akka Scala Base — Code Solutions portfolio example.
 *
 * Demonstrates Akka Typed + Akka HTTP + Akka Persistence Typed for building
 * scalable, resilient, event-sourced services.
 *
 * Covers JD requirements for the Senior Software Engineer role:
 *   - Scala + Akka (Streams, Actors, HTTP, Persistence)
 *   - Designing well-defined RESTful APIs
 *   - High-availability, reliable solutions (event sourcing + CQRS-ready)
 *   - Event-driven architecture
 *
 * Run:  sbt run
 * Then:
 *   curl http://localhost:8080/hello
 *   curl -X POST http://localhost:8080/orders -H 'Content-Type: application/json' \
 *        -d '{"orderId":"o1","customerId":"c1","amount":99.9}'
 *   curl http://localhost:8080/orders/o1
 *   curl -X PUT http://localhost:8080/orders/o1 -H 'Content-Type: application/json' \
 *        -d '{"status":"PAID"}'
 *
 * NOTE: The default dev server uses an in-memory service. To use the
 * Akka Persistence-backed service (event-sourced, restart-safe), swap
 * InMemoryOrderService for AkkaOrderService and start Cassandra — see README.
 */
object AkkaHttpBase extends DefaultJsonProtocol {

  final case class Message(text: String)
  implicit val messageFormat: RootJsonFormat[Message] = jsonFormat1(Message)

  def createRoute(): Route = {
    import JsonProtocols._
    val service = new InMemoryOrderService()
    val orderRoutes = new OrderRoutes(service).route

    path("hello") {
      get { complete(Message("Hello from Akka HTTP base - Code Solutions example")) }
    } ~
    path("legacy" / Segment) { id =>
      get { complete(Message(s"Enriched legacy data for id=$id | processed by Akka")) }
    } ~
    orderRoutes
  }

  def main(args: Array[String]): Unit = {
    implicit val system: ActorSystem[Nothing] = ActorSystem(Behaviors.empty, "akka-http-base")
    implicit val ec = system.executionContext

    Http().newServerAt("0.0.0.0", 8080).bind(createRoute())

    println("Server online at http://localhost:8080/")
    println("Try:")
    println("  curl http://localhost:8080/hello")
    println("  curl http://localhost:8080/legacy/123")
    println("  curl -X POST http://localhost:8080/orders -H 'Content-Type: application/json' \\")
    println("       -d '{\"orderId\":\"o1\",\"customerId\":\"c1\",\"amount\":99.9}'")
    println("  curl http://localhost:8080/orders/o1")
  }
}
