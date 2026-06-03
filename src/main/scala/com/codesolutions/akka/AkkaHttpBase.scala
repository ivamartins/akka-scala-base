package com.codesolutions.akka

import akka.actor.typed.ActorSystem
import akka.actor.typed.scaladsl.Behaviors
import akka.http.scaladsl.Http
import akka.http.scaladsl.model._
import akka.http.scaladsl.server.Directives._
import akka.http.scaladsl.marshallers.sprayjson.SprayJsonSupport._
import spray.json._

/**
 * Akka Scala Base - Functional Portfolio Example
 *
 * Demonstrates Akka Typed + Akka HTTP for building scalable, resilient services.
 * 
 * Ties directly to stack: "Scala/Akka"
 *
 * This is a minimal, functional base you can extend with:
 * - Persistence (Akka Persistence for event sourcing, great for legacy modernization)
 * - Clustering
 * - Integration with Kafka (combine with flink-kafka-scala-base)
 * - Calling AI agents for smart endpoints
 * - Legacy system proxies (wrap old Java/Play services)
 *
 * Run: sbt run
 * Then curl http://localhost:8080/hello
 *
 * See full portfolio: https://ivamartins.github.io/code-solutions-site/
 *
 * PT: Demonstra Akka Typed + Akka HTTP para serviços escaláveis e resilientes.
 * Base mínima funcional para estender com Persistence (ótimo para modernização de legados),
 * Clustering, integração com Kafka/Flink, chamadas de agentes IA, proxies para sistemas legados.
 */
object AkkaHttpBase extends App with DefaultJsonProtocol {

  implicit val system: ActorSystem[Nothing] = ActorSystem(Behaviors.empty, "akka-http-base")

  implicit val ec = system.executionContext

  // Simple JSON model (extend for your domain, e.g. legacy order)
  final case class Message(text: String)
  implicit val messageFormat = jsonFormat1(Message)

  val route =
    path("hello") {
      get {
        complete(Message("Hello from Akka HTTP base - Code Solutions example"))
      }
    } ~
    path("legacy" / Segment) { id =>
      get {
        // Example: proxy/enrich legacy data (in real: call old system or AI agent)
        complete(Message(s"Enriched legacy data for id=$id | processed by Akka"))
      }
    }

  Http().newServerAt("0.0.0.0", 8080).bind(route)

  println("Server online at http://localhost:8080/")
  println("Try: curl http://localhost:8080/hello")
  println("Or: curl http://localhost:8080/legacy/123")
}
