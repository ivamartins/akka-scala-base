package com.codesolutions.akka.http

import akka.http.scaladsl.marshallers.sprayjson.SprayJsonSupport
import akka.http.scaladsl.marshallers.sprayjson.SprayJsonSupport._
import akka.http.scaladsl.model.StatusCodes
import akka.http.scaladsl.server.Directives._
import akka.http.scaladsl.server.Route
import spray.json.{DefaultJsonProtocol, RootJsonFormat, deserializationError}

import com.codesolutions.akka.domain.Order
import com.codesolutions.akka.persistence._

import java.time.Instant
import scala.concurrent.ExecutionContext

/**
 * HTTP layer for the Order domain.
 *
 * Designed around a pluggable OrderService:
 *   - InMemoryOrderService (default, used by tests and the simple dev server)
 *   - AkkaOrderService (production, ask-pattern binding to OrderPersistentActor)
 *
 * Demonstrates the textbook "thin HTTP layer" pattern that the Senior role
 * asks for: "designing well-defined RESTful APIs" and "high-availability,
 * reliable solutions".
 */
class OrderRoutes(service: OrderService) {

  import JsonProtocols._

  def route: Route = {
    pathPrefix("orders") {
      path(Segment) { orderId =>
        get {
          onSuccess(service.get(orderId)) {
            case OrderFound(o)    => complete(o)
            case OrderNotFound(_) => complete(StatusCodes.NotFound -> s"order $orderId not found")
            case OrderRejected(r) => complete(StatusCodes.BadRequest -> r)
            case other            => complete(StatusCodes.InternalServerError -> other.toString)
          }
        } ~
        put {
          entity(as[OrderUpdateRequest]) { body =>
            onSuccess(service.changeStatus(orderId, body.status)) {
              case OrderFound(o)    => complete(o)
              case OrderNotFound(_) => complete(StatusCodes.NotFound -> s"order $orderId not found")
              case OrderRejected(r) => complete(StatusCodes.BadRequest -> r)
              case other            => complete(StatusCodes.InternalServerError -> other.toString)
            }
          }
        }
      } ~
      post {
        entity(as[CreateOrderRequest]) { body =>
          onSuccess(service.create(body.orderId, body.customerId, body.amount)) {
            case OrderCreatedResp(o) => complete(StatusCodes.Created -> o)
            case OrderRejected(r)    => complete(StatusCodes.BadRequest -> r)
            case other               => complete(StatusCodes.InternalServerError -> other.toString)
          }
        }
      }
    }
  }
}

object OrderRoutes {
  def apply(): OrderRoutes = new OrderRoutes(new InMemoryOrderService())
  def apply(service: OrderService): OrderRoutes = new OrderRoutes(service)
}

final case class CreateOrderRequest(orderId: String, customerId: String, amount: Double)
final case class OrderUpdateRequest(status: String)

object JsonProtocols extends DefaultJsonProtocol {
  // Custom Instant format: serialize as ISO-8601 string.
  implicit val instantFormat: RootJsonFormat[Instant] = new RootJsonFormat[Instant] {
    def write(i: Instant) = spray.json.JsString(i.toString)
    def read(json: spray.json.JsValue) = json match {
      case spray.json.JsString(s) => Instant.parse(s)
      case other => deserializationError("Instant expected as ISO-8601 string, got " + other)
    }
  }

  implicit val orderFormat: RootJsonFormat[Order] = jsonFormat5(Order)
  implicit val createReqFormat: RootJsonFormat[CreateOrderRequest] = jsonFormat3(CreateOrderRequest)
  implicit val updateReqFormat: RootJsonFormat[OrderUpdateRequest] = jsonFormat1(OrderUpdateRequest)
}
