package com.codesolutions.akka.http

import akka.http.scaladsl.model.{ContentTypes, HttpEntity, HttpMethods, HttpRequest, StatusCodes}
import akka.http.scaladsl.testkit.ScalatestRouteTest
import com.codesolutions.akka.persistence.InMemoryOrderService
import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpec

class OrderRoutesSpec
    extends AnyWordSpec
    with Matchers
    with ScalatestRouteTest {

  import JsonProtocols._

  val service = new InMemoryOrderService()
  val routes = new OrderRoutes(service).route

  "OrderRoutes" should {

    "POST /orders creates an order" in {
      val req = HttpRequest(
        method = HttpMethods.POST,
        uri = "/orders",
        entity = HttpEntity(ContentTypes.`application/json`,
          """{"orderId":"o-1","customerId":"c-1","amount":99.9}""")
      )
      req ~> routes ~> check {
        status shouldEqual StatusCodes.Created
        responseAs[String] should include ("\"status\":\"CREATED\"")
      }
    }

    "GET /orders/:id returns 200 when found" in {
      Post("/orders", HttpEntity(ContentTypes.`application/json`,
        """{"orderId":"o-2","customerId":"c-2","amount":10.0}""")) ~> routes ~> check {
        status shouldEqual StatusCodes.Created
      }
      Get("/orders/o-2") ~> routes ~> check {
        status shouldEqual StatusCodes.OK
        responseAs[String] should include ("\"id\":\"o-2\"")
      }
    }

    "PUT /orders/:id updates status" in {
      Post("/orders", HttpEntity(ContentTypes.`application/json`,
        """{"orderId":"o-3","customerId":"c-3","amount":42.0}""")) ~> routes ~> check {
        status shouldEqual StatusCodes.Created
      }
      Put("/orders/o-3", HttpEntity(ContentTypes.`application/json`,
        """{"status":"PAID"}""")) ~> routes ~> check {
        status shouldEqual StatusCodes.OK
        responseAs[String] should include ("\"status\":\"PAID\"")
      }
    }

    "GET /orders/:id returns 404 when not found" in {
      Get("/orders/does-not-exist") ~> routes ~> check {
        status shouldEqual StatusCodes.NotFound
      }
    }

    "POST /orders rejects duplicate" in {
      Post("/orders", HttpEntity(ContentTypes.`application/json`,
        """{"orderId":"o-dup","customerId":"c","amount":1.0}""")) ~> routes ~> check {
        status shouldEqual StatusCodes.Created
      }
      Post("/orders", HttpEntity(ContentTypes.`application/json`,
        """{"orderId":"o-dup","customerId":"c","amount":1.0}""")) ~> routes ~> check {
        status shouldEqual StatusCodes.BadRequest
      }
    }

    "POST /orders rejects amount <= 0" in {
      Post("/orders", HttpEntity(ContentTypes.`application/json`,
        """{"orderId":"o-neg","customerId":"c","amount":-1.0}""")) ~> routes ~> check {
        status shouldEqual StatusCodes.BadRequest
      }
    }
  }
}
