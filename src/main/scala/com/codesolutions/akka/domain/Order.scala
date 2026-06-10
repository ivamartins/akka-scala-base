package com.codesolutions.akka.domain

import java.time.Instant

/**
 * Order domain — used by the Persistence + Akka HTTP example.
 *
 * Demonstrates a typical "modernize a legacy order entity" use case:
 *   - immutable case class
 *   - JSON-ready (spray-json formats in JsonProtocols)
 */
final case class Order(
    id: String,
    customerId: String,
    amount: Double,
    status: String,
    createdAt: Instant
)
