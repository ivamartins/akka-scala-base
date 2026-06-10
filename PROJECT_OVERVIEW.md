# akka-scala-base — Overview & flow

Functional base in **Scala 2.13 + Akka (Typed + HTTP + Persistence)**. Scala, Akka (Streams, Actors, HTTP, Persistence), REST APIs, high availability, architectural patterns (MVC, Microservices, Event-driven).

## Stack (with versions)

- **Scala 2.13.14**
- **sbt** (version in `project/build.properties`)
- **Akka 2.8.5** (Actor Typed, Stream, Persistence Typed, Serialization Jackson, TestKit Typed, Persistence TestKit)
- **Akka HTTP 10.5.3** (Core, Spray-JSON, TestKit)
- **Logback 1.4.14**
- **ScalaTest 3.2.17**

---

## Main flow

### 1. Bootstrap
`AkkaHttpBase.main` creates an `ActorSystem` (Akka Typed, `Behaviors.empty` named `akka-http-base`) and starts an **Akka HTTP** server on `0.0.0.0:8080` with the `createRoute()` route.

### 2. Routes (`OrderRoutes`)
- `GET /hello` — healthcheck/hello world.
- `GET /legacy/{id}` — legacy data proxy/enrichment.
- `POST /orders` — creates order (delegated to `OrderService`).
- `GET /orders/{orderId}` — query order.
- `PUT /orders/{orderId}` — change status (e.g., `PAID`).
- 404 and 400 errors with JSON via `Spray-JSON`.

### 3. Service layer
By default, `InMemoryOrderService` (no external dependency, `sbt run` works standalone). Switching to `AkkaOrderService` makes the backend an `EventSourcedBehavior` (Akka Persistence Typed) with Cassandra as the journal in production.

### 4. Event Sourcing (`OrderPersistentActor`)
- Each `OrderPersistentActor` instance handles ONE `orderId` (`PersistenceId("Order", orderId)`).
- **Commands** (`OrderProtocol.OrderCommand`): `CreateOrder`, `ChangeOrderStatus`, `GetOrder`.
- **Events** (`OrderProtocol.OrderEvent`): `OrderCreated`, `OrderStatusChanged`.
- **Responses**: `OrderCreatedResp`, `OrderFound`, `OrderNotFound`, `OrderRejected`.
- **Validation in commandHandler**:
  - rejects `CreateOrder` if it already exists or if `amount <= 0`;
  - rejects `ChangeOrderStatus` if the order doesn't exist.
- **EventHandler** is pure: applies `OrderCreated` (creates state) and `OrderStatusChanged` (changes status).
- **Snapshot every 100 events** (`RetentionCriteria.snapshotEvery(100)`) to limit replay time.
- **Tagger**: `Set("order-event")` to enable queries/reads-by-tag.

### 5. Persistence
Dev (in-memory). Prod: Cassandra via `akka-persistence-cassandra` — config in `application.conf` (`akka.persistence.journal.plugin`, `akka.persistence.snapshot-store.plugin`).

---

## Endpoints

| Method | Path                    | Description                              |
|--------|-------------------------|------------------------------------------|
| GET    | `/hello`                | Hello world / healthcheck                |
| GET    | `/legacy/{id}`          | Legacy data enrichment                   |
| POST   | `/orders`               | Create order                             |
| GET    | `/orders/{orderId}`     | Get order                                |
| PUT    | `/orders/{orderId}`     | Change order status                      |

---

## What's in each subfolder

### Root
- `build.sbt` — Scala 2.13.14, Akka 2.8.5, Akka HTTP 10.5.3, ScalaTest 3.2.17.
- `project/build.properties` — sbt version.
- `README.md` — quickstart, endpoints.
- `.gitignore` — Scala/sbt/IDE.
- `src/main/resources/application.conf` — Akka config (journal, snapshot-store, loglevel).

### `src/main/scala/com/codesolutions/akka/`
- `AkkaHttpBase.scala` — `main`, creates ActorSystem, starts Akka HTTP on 8080, defines `Message` JSON and assembles the routes.

### `src/main/scala/com/codesolutions/akka/domain/`
- `Order.scala` — domain model `case class Order(orderId, customerId, amount, status, createdAt)`.

### `src/main/scala/com/codesolutions/akka/persistence/`
- `OrderProtocol.scala` — ADTs `OrderCommand`, `OrderEvent`, responses (`OrderCreatedResp`, `OrderFound`, `OrderNotFound`, `OrderRejected`).
- `OrderPersistentActor.scala` — `EventSourcedBehavior` with snapshot every 100 events, tagger `order-event`, validations.
- `OrderService.scala` — trait `OrderService` + impl `InMemoryOrderService` (default) + `AkkaOrderService` (binds to actor via ask-pattern).

### `src/main/scala/com/codesolutions/akka/http/`
- `OrderRoutes.scala` — REST routes (`/orders`) + JSON via Spray-JSON, 400/404 error handling.

### `src/test/scala/com/codesolutions/akka/`
- `AkkaHttpBaseSpec.scala` — base tests (hello, legacy).
- `http/OrderRoutesSpec.scala` — full HTTP REST lifecycle (POST, GET, PUT, 404, 400).
- `persistence/OrderPersistentActorSpec.scala` — state machine + event sourcing validation.

---

## How to run

```bash
sbt run
```

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

## How to test

```bash
sbt test
```

## Migration to real persistence (production)

1. `docker run -p 9042:9042 cassandra:4.1`
2. In `application.conf`, change:
   ```hocon
   akka.persistence.journal.plugin = "akka.persistence.cassandra.journal"
   akka.persistence.snapshot-store.plugin = "akka.persistence.cassandra.snapshot"
   ```
3. Add `akka-persistence-cassandra` to `build.sbt`.
4. Bind `OrderRoutes` to `OrderPersistentActor` via ask-pattern (full example in `scala-akka-aws-microservice`).
