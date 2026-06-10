package com.codesolutions.akka.persistence

import com.codesolutions.akka.domain.Order
import com.fasterxml.jackson.annotation.JsonCreator
import com.fasterxml.jackson.annotation.JsonProperty

import java.time.Instant

/**
 * Events are the source of truth in an Event Sourced system.
 * They are PERSISTED as the journal entries — the actor state is rebuilt
 * by replaying them. The state itself is NEVER persisted directly.
 *
 * The Caterpillar JD Senior role requires "designing well-defined APIs" and
 * "high-availability, reliable solutions" — event sourcing is one of the
 * strongest patterns to deliver both.
 */
sealed trait OrderEvent
final case class OrderCreated @JsonCreator() (
    @JsonProperty("orderId") orderId: String,
    @JsonProperty("customerId") customerId: String,
    @JsonProperty("amount") amount: Double,
    @JsonProperty("createdAt") createdAt: Instant
) extends OrderEvent

final case class OrderStatusChanged @JsonCreator() (
    @JsonProperty("newStatus") newStatus: String
) extends OrderEvent

/**
 * Commands are external requests that the actor validates and translates
 * into events (or rejects with a reply).
 */
sealed trait OrderCommand
final case class CreateOrder(orderId: String, customerId: String, amount: Double, replyTo: akka.actor.typed.ActorRef[OrderResponse]) extends OrderCommand
final case class ChangeOrderStatus(orderId: String, newStatus: String, replyTo: akka.actor.typed.ActorRef[OrderResponse]) extends OrderCommand
final case class GetOrder(replyTo: akka.actor.typed.ActorRef[OrderResponse]) extends OrderCommand

/**
 * Reply ADT — modeled as a sealed trait so the route layer can pattern-match
 * exhaustively (no runtime ClassCastException).
 */
sealed trait OrderResponse
final case class OrderCreatedResp(order: Order) extends OrderResponse
final case class OrderFound(order: Order) extends OrderResponse
final case class OrderNotFound(orderId: String) extends OrderResponse
final case class OrderRejected(reason: String) extends OrderResponse
