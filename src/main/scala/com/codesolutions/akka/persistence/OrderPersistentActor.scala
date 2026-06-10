package com.codesolutions.akka.persistence

import akka.actor.typed.Behavior
import akka.actor.typed.scaladsl.Behaviors
import akka.persistence.typed.PersistenceId
import akka.persistence.typed.scaladsl.{Effect, EventSourcedBehavior, RetentionCriteria}

import java.time.Instant
import scala.concurrent.duration._

import com.codesolutions.akka.domain.Order

/**
 * OrderPersistentActor — a single instance handles one orderId.
 *
 * PersistenceId includes a tag prefix ("Order") so we can later build
 * EventSourcedBehavior[OrderCommand, OrderEvent, OrderState] instances for
 * other aggregates (Customer, Invoice, ...) without collision.
 *
 * Demonstrates:
 *   - EventSourcedBehavior (Akka Persistence Typed)
 *   - Snapshotting every 100 events to bound replay time
 *   - All four Akka building blocks the job description calls out:
 *       Streams, Actors, HTTP, Persistence
 */
object OrderPersistentActor {

  // Event tag — used by Projections / Read-Processors if you later build one.
  val EventTag = "order-event"

  // Snapshot every 100 events to keep replay time bounded.
  private val SnapshotEvery = 100

  def apply(orderId: String): Behavior[OrderCommand] =
    EventSourcedBehavior[OrderCommand, OrderEvent, OrderState](
      persistenceId = PersistenceId("Order", orderId),
      emptyState = OrderState.empty,
      commandHandler = (state, cmd) => handleCommand(orderId, state, cmd),
      eventHandler = (state, evt) => applyEvent(state, evt)
    )
      .withTagger(_ => Set(EventTag))
      .withRetention(RetentionCriteria.snapshotEvery(numberOfEvents = SnapshotEvery))

  private def handleCommand(
      orderId: String,
      state: OrderState,
      cmd: OrderCommand
  ): Effect[OrderEvent, OrderState] = cmd match {
    case CreateOrder(_, customerId, amount, replyTo) =>
      if (state.exists)
        Effect.reply(replyTo)(OrderRejected(s"Order $orderId already exists"))
      else if (amount <= 0)
        Effect.reply(replyTo)(OrderRejected("amount must be > 0"))
      else
        Effect.persist(OrderCreated(orderId, customerId, amount, Instant.now()))
          .thenReply(replyTo)(newState =>
            OrderCreatedResp(
              Order(orderId, customerId, amount, newState.status, Instant.now())
            )
          )

    case ChangeOrderStatus(_, newStatus, replyTo) =>
      if (!state.exists)
        Effect.reply(replyTo)(OrderNotFound(orderId))
      else
        Effect.persist(OrderStatusChanged(newStatus))
          .thenReply(replyTo)(newState =>
            OrderFound(
              Order(orderId, state.customerId.getOrElse("?"), state.amount.getOrElse(0.0), newState.status, state.createdAt.getOrElse(Instant.now()))
            )
          )

    case GetOrder(replyTo) =>
      if (!state.exists)
        Effect.reply(replyTo)(OrderNotFound(orderId))
      else
        Effect.reply(replyTo)(
          OrderFound(
            Order(orderId, state.customerId.getOrElse("?"), state.amount.getOrElse(0.0), state.status, state.createdAt.getOrElse(Instant.now()))
          )
        )
  }

  private def applyEvent(state: OrderState, evt: OrderEvent): OrderState = evt match {
    case OrderCreated(_, customerId, amount, createdAt) =>
      state.copy(exists = true, customerId = Some(customerId), amount = Some(amount), status = "CREATED", createdAt = Some(createdAt))
    case OrderStatusChanged(newStatus) =>
      state.copy(status = newStatus)
  }
}

/**
 * Aggregated state. Kept as a separate case class so event-handler logic
 * is pure and easy to test.
 */
final case class OrderState(
    exists: Boolean,
    customerId: Option[String],
    amount: Option[Double],
    status: String,
    createdAt: Option[Instant]
)

object OrderState {
  val empty: OrderState = OrderState(exists = false, None, None, "", None)
}
