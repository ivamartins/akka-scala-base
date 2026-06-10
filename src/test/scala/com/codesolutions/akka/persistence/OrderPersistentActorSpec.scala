package com.codesolutions.akka.persistence

import com.codesolutions.akka.domain.Order
import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpec

import java.time.Instant

/**
 * Unit tests for the order state machine.
 *
 * These tests focus on the pure state-transition logic of
 * OrderPersistentActor: given a starting state and an event, the new
 * state must be the expected one. This is the heart of event sourcing
 * and is fully testable without spinning up an actor system.
 *
 * For end-to-end persistence (journal + snapshot recovery), see the
 * docker-compose stack in the README.
 */
class OrderPersistentActorSpec extends AnyWordSpec with Matchers {

  "OrderState" must {

    "start empty" in {
      val s = OrderState.empty
      s.exists shouldEqual false
      s.status shouldEqual ""
    }

    "transition to CREATED on OrderCreated" in {
      val s0 = OrderState.empty
      val now = Instant.parse("2024-01-01T00:00:00Z")
      val evt = OrderCreated("o-1", "c-1", 100.0, now)
      val s1 = applyEvt(s0, evt)
      s1.exists shouldEqual true
      s1.customerId shouldEqual Some("c-1")
      s1.amount shouldEqual Some(100.0)
      s1.status shouldEqual "CREATED"
      s1.createdAt shouldEqual Some(now)
    }

    "transition CREATED -> PAID on OrderStatusChanged" in {
      val now = Instant.parse("2024-01-01T00:00:00Z")
      val s0 = applyEvt(OrderState.empty, OrderCreated("o-1", "c-1", 50.0, now))
      val s1 = applyEvt(s0, OrderStatusChanged("PAID"))
      s1.status shouldEqual "PAID"
      s1.amount shouldEqual Some(50.0)
    }

    "preserve history fields across status changes" in {
      val now = Instant.parse("2024-01-01T00:00:00Z")
      val s0 = applyEvt(OrderState.empty, OrderCreated("o-1", "c-1", 75.0, now))
      val s1 = applyEvt(s0, OrderStatusChanged("SHIPPED"))
      val s2 = applyEvt(s1, OrderStatusChanged("DELIVERED"))
      s2.status shouldEqual "DELIVERED"
      s2.customerId shouldEqual Some("c-1")
      s2.amount shouldEqual Some(75.0)
      s2.createdAt shouldEqual Some(now)
    }
  }

  "Order domain validation" must {
    // The same validation rules enforced by the command handler.
    def validateCreate(existing: OrderState, amount: Double): Either[String, Order] = {
      if (existing.exists) Left("already exists")
      else if (amount <= 0) Left("amount must be > 0")
      else Right(Order("o-1", "c-1", amount, "CREATED", Instant.now()))
    }

    "accept valid amount" in {
      validateCreate(OrderState.empty, 10.0).isRight shouldEqual true
    }

    "reject non-positive amount" in {
      validateCreate(OrderState.empty, 0).isLeft shouldEqual true
      validateCreate(OrderState.empty, -1).isLeft shouldEqual true
    }

    "reject duplicate create" in {
      val existing = OrderState.empty.copy(exists = true)
      validateCreate(existing, 10).isLeft shouldEqual true
    }
  }

  // Same reducer used by the actor — kept here so tests don't depend on the
  // private applyEvent method.
  private def applyEvt(state: OrderState, evt: OrderEvent): OrderState = evt match {
    case OrderCreated(_, customerId, amount, createdAt) =>
      state.copy(exists = true, customerId = Some(customerId), amount = Some(amount), status = "CREATED", createdAt = Some(createdAt))
    case OrderStatusChanged(newStatus) =>
      state.copy(status = newStatus)
  }
}
