# akka-scala-base

Minimal, functional Akka (Typed + HTTP) base in Scala.

**This is a core framework example for building resilient, scalable services with Akka.**

## Why this base?
- Demonstrates "Scala/Akka" part of the stack.
- Functional starting point for:
  - High-performance HTTP APIs
  - Event sourcing / persistence (Akka Persistence - perfect for legacy modernization)
  - Clustering and sharding
  - Integration with Kafka/Flink streams
  - AI agent calls from actors/routes
  - Wrapping legacy Java/Play systems

## Quick Start (Functional)

```bash
sbt run
```

Server runs on http://localhost:8080

Examples:
- curl http://localhost:8080/hello
- curl http://localhost:8080/legacy/ORDER-456

## Extend for Real Projects

- Add Akka Persistence for event-sourced aggregates (great for migrating legacy stateful systems).
- Add Kafka consumer/producer (see flink-kafka-scala-base).
- Call external AI agents (see whatsapp-grok-bot) for intelligent routing or enrichment.
- Add clustering for scale.
- Proxy legacy endpoints (simulate calling old monolith and modernizing the response).

## Portfolio Mapping

This base supports claims around Scala/Akka for event-driven and resilient backends, often combined with Flink/Kafka for full modern architectures on top of legacy.

See the complete set:
https://ivamartins.github.io/code-solutions-site/

Company: https://www.linkedin.com/company/code-solutions-it/

Clone, add your logic, use as proof or starting point for Akka-based services.
