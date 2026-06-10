# akka-scala-base

Base funcional em Akka (Typed + HTTP + Persistence) com Scala.

**Português:**
Framework principal para construir serviços resilientes, escaláveis e event-sourced com Akka. Inclui exemplo de Order domain com Event Sourcing (Akka Persistence Typed), HTTP REST API (Akka HTTP) e cobertura de testes completa.

**English:**
Core framework for building resilient, scalable, event-sourced services with Akka. Includes an Order domain example with Event Sourcing (Akka Persistence Typed), HTTP REST API (Akka HTTP) and full test coverage.

## Why this base?

- Demonstrates the "Scala/Akka" part of the stack.
- Functional starting point for:
  - High-performance HTTP APIs
  - Event sourcing / persistence (Akka Persistence — perfect for legacy modernization)
  - Clustering, sharding, persistence queries
  - Integration with Kafka / Flink (combine with `flink-kafka-scala-base`)
  - AI agent endpoints
  - Legacy system proxies (wrap old Java / Play services)

## What's in this repo

```
akka-scala-base/
├── src/main/scala/com/codesolutions/akka/
│   ├── AkkaHttpBase.scala              # main() — boots the HTTP server
│   ├── domain/Order.scala              # domain model
│   ├── persistence/
│   │   ├── OrderProtocol.scala         # Commands, Events, Responses
│   │   ├── OrderPersistentActor.scala  # EventSourcedBehavior
│   │   └── OrderService.scala          # service trait + InMemoryOrderService
│   └── http/OrderRoutes.scala          # Akka HTTP routes (REST)
├── src/main/resources/application.conf
├── src/test/scala/com/codesolutions/akka/
│   ├── AkkaHttpBaseSpec.scala
│   ├── http/OrderRoutesSpec.scala
│   └── persistence/OrderPersistentActorSpec.scala
└── build.sbt
```

## How to run

```bash
sbt run
```

Then in another terminal:

```bash
curl http://localhost:8080/hello
curl -X POST http://localhost:8080/orders \
  -H 'Content-Type: application/json' \
  -d '{"orderId":"o1","customerId":"c1","amount":99.9}'
curl http://localhost:8080/orders/o1
curl -X PUT http://localhost:8080/orders/o1 \
  -H 'Content-Type: application/json' \
  -d '{"status":"PAID"}'
```

## Run the tests

```bash
sbt test
```

Coverage:
- `OrderPersistentActorSpec` — state machine + validation (event sourcing)
- `OrderRoutesSpec` — full HTTP REST lifecycle (POST, GET, PUT, 404, 400)
- `AkkaHttpBaseSpec` — base endpoints

## How it maps to the Caterpillar JD (Senior Software Engineer)

| JD requirement | Where in this repo |
|---|---|
| 2+ years Scala | entire codebase |
| 2+ years Akka (Streams, Actors, HTTP, Persistence) | `OrderPersistentActor` (Persistence), `OrderRoutes` (HTTP), Akka Typed Actors |
| Designing well-defined RESTful APIs | `OrderRoutes` — versionable, OpenAPI-ready |
| High-availability, reliable solutions | Event Sourcing + Snapshotting + In-Memory journal swap to Cassandra for prod |
| Application architectural patterns (MVC, Microservices, Event-driven) | EventSourcedBehavior + CQRS-ready |
| PostgreSQL/NoSQL, AWS, CI/CD | swap `InMemoryOrderService` for `AkkaOrderService` + DB; see `scala-akka-aws-microservice` for the full AWS deploy |

## Production notes

The default `main` uses `InMemoryOrderService` so you can `sbt run` with no external dependencies. To switch to Akka Persistence:

1. Start Cassandra: `docker run -p 9042:9042 cassandra:4.1`
2. Replace the journal plugin in `application.conf`:
   ```hocon
   akka.persistence.journal.plugin = "akka.persistence.cassandra.journal"
   akka.persistence.snapshot-store.plugin = "akka.persistence.cassandra.snapshot"
   ```
3. Add `akka-persistence-cassandra` to `build.sbt`.
4. Bind `OrderRoutes` to `OrderPersistentActor` via ask-pattern (see `scala-akka-aws-microservice` for a full example).

See portfolio: https://ivamartins.github.io/code-solutions-site/
