package com.codesolutions.akka.persistence

import com.codesolutions.akka.domain.Order

import scala.concurrent.Future

/**
 * Abstraction over the order store so HTTP routes can be tested with a
 * pure in-memory implementation (no ActorSystem, no journal).
 */
trait OrderService {
  def create(orderId: String, customerId: String, amount: Double): Future[OrderResponse]
  def changeStatus(orderId: String, newStatus: String): Future[OrderResponse]
  def get(orderId: String): Future[OrderResponse]
}

/**
 * In-memory implementation backed by a Map. Used by tests and as a
 * fallback for the default AkkaHttpBase server when no journal is configured.
 *
 * Production: replace with an Akka HTTP ask-pattern binding to
 * OrderPersistentActor (see OrderRoutes.akkaRoutes).
 */
class InMemoryOrderService extends OrderService {
  private val store = scala.collection.mutable.Map.empty[String, Order]

  def create(orderId: String, customerId: String, amount: Double): Future[OrderResponse] = Future.successful {
    store.synchronized {
      if (store.contains(orderId)) OrderRejected(s"Order $orderId already exists")
      else if (amount <= 0) OrderRejected("amount must be > 0")
      else {
        val o = Order(orderId, customerId, amount, "CREATED", java.time.Instant.now())
        store += (orderId -> o)
        OrderCreatedResp(o)
      }
    }
  }

  def changeStatus(orderId: String, newStatus: String): Future[OrderResponse] = Future.successful {
    store.synchronized {
      store.get(orderId) match {
        case Some(o) =>
          val updated = o.copy(status = newStatus)
          store += (orderId -> updated)
          OrderFound(updated)
        case None => OrderNotFound(orderId)
      }
    }
  }

  def get(orderId: String): Future[OrderResponse] = Future.successful {
    store.get(orderId) match {
      case Some(o) => OrderFound(o)
      case None    => OrderNotFound(orderId)
    }
  }
}
