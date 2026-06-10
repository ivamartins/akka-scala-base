package com.codesolutions.akka

import akka.http.scaladsl.marshallers.sprayjson.SprayJsonSupport._
import akka.http.scaladsl.model.StatusCodes
import akka.http.scaladsl.testkit.ScalatestRouteTest
import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpec

class AkkaHttpBaseSpec extends AnyWordSpec with Matchers with ScalatestRouteTest {

  // Build a route from the default (in-memory) service.
  // We pass an explicit route so we don't need a typed ActorSystem here.
  import com.codesolutions.akka.http.{JsonProtocols, OrderRoutes}
  import com.codesolutions.akka.persistence.InMemoryOrderService
  import akka.http.scaladsl.server.Directives._
  import JsonProtocols._

  val routes = path("hello") {
    get { complete(AkkaHttpBase.Message("Hello from Akka HTTP base - Code Solutions example")) }
  } ~ path("legacy" / Segment) { id =>
    get { complete(AkkaHttpBase.Message(s"Enriched legacy data for id=$id | processed by Akka")) }
  } ~ new OrderRoutes(new InMemoryOrderService()).route

  "AkkaHttpBase" should {
    "return hello message" in {
      Get("/hello") ~> routes ~> check {
        status shouldEqual StatusCodes.OK
        responseAs[String] should include ("Hello from Akka HTTP base")
      }
    }

    "return enriched legacy data" in {
      Get("/legacy/123") ~> routes ~> check {
        status shouldEqual StatusCodes.OK
        responseAs[String] should include ("Enriched legacy data for id=123")
      }
    }
  }
}
