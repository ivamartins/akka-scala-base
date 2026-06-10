# akka-scala-base — Visão geral e fluxo

Base funcional em **Scala 2.13 + Akka (Typed + HTTP + Persistence)**. Scala, Akka (Streams, Actors, HTTP, Persistence), REST APIs, alta disponibilidade, padrões arquiteturais (MVC, Microservices, Event-driven).

## Stack (com versões)

- **Scala 2.13.14**
- **sbt** (versão em `project/build.properties`)
- **Akka 2.8.5** (Actor Typed, Stream, Persistence Typed, Serialization Jackson, TestKit Typed, Persistence TestKit)
- **Akka HTTP 10.5.3** (Core, Spray-JSON, TestKit)
- **Logback 1.4.14**
- **ScalaTest 3.2.17**

---

## Fluxo principal

### 1. Bootstrap
`AkkaHttpBase.main` cria um `ActorSystem` (Akka Typed, `Behaviors.empty` chamado `akka-http-base`) e sobe um servidor **Akka HTTP** em `0.0.0.0:8080` com a rota `createRoute()`.

### 2. Rotas (`OrderRoutes`)
- `GET /hello` — healthcheck/hello world.
- `GET /legacy/{id}` — proxy/enriquecimento de dados legados.
- `POST /orders` — cria pedido (delegado ao `OrderService`).
- `GET /orders/{orderId}` — consulta pedido.
- `PUT /orders/{orderId}` — muda status (ex: `PAID`).
- Erros 404 e 400 com JSON via `Spray-JSON`.

### 3. Service layer
Por padrão, `InMemoryOrderService` (sem dependência externa, `sbt run` funciona standalone). Trocar para `AkkaOrderService` faz o backend virar um `EventSourcedBehavior` (Akka Persistence Typed) com Cassandra como journal em produção.

### 4. Event Sourcing (`OrderPersistentActor`)
- Cada instância do `OrderPersistentActor` cuida de UM `orderId` (`PersistenceId("Order", orderId)`).
- **Comandos** (`OrderProtocol.OrderCommand`): `CreateOrder`, `ChangeOrderStatus`, `GetOrder`.
- **Eventos** (`OrderProtocol.OrderEvent`): `OrderCreated`, `OrderStatusChanged`.
- **Respostas**: `OrderCreatedResp`, `OrderFound`, `OrderNotFound`, `OrderRejected`.
- **Validação no commandHandler**:
  - rejeita `CreateOrder` se já existe ou se `amount <= 0`;
  - rejeita `ChangeOrderStatus` se pedido não existe.
- **EventHandler** é puro: aplica `OrderCreated` (cria estado) e `OrderStatusChanged` (muda status).
- **Snapshot a cada 100 eventos** (`RetentionCriteria.snapshotEvery(100)`) para limitar tempo de replay.
- **Tagger**: `Set("order-event")` para habilitar queries/reads-by-tag.

### 5. Persistência
Dev (in-memory). Prod: Cassandra via `akka-persistence-cassandra` — config em `application.conf` (`akka.persistence.journal.plugin`, `akka.persistence.snapshot-store.plugin`).

---

## Endpoints

| Método | Path                    | Descrição                              |
|--------|-------------------------|----------------------------------------|
| GET    | `/hello`                | Hello world / healthcheck              |
| GET    | `/legacy/{id}`          | Enriquecimento de dados legados        |
| POST   | `/orders`               | Criar pedido                           |
| GET    | `/orders/{orderId}`     | Buscar pedido                          |
| PUT    | `/orders/{orderId}`     | Mudar status do pedido                 |

---

## O que tem em cada subpasta

### Raiz
- `build.sbt` — Scala 2.13.14, Akka 2.8.5, Akka HTTP 10.5.3, ScalaTest 3.2.17.
- `project/build.properties` — versão sbt.
- `README.md` — quickstart, endpoints.
- `.gitignore` — Scala/sbt/IDE.
- `src/main/resources/application.conf` — config do Akka (journal, snapshot-store, loglevel).

### `src/main/scala/com/codesolutions/akka/`
- `AkkaHttpBase.scala` — `main`, cria ActorSystem, sobe Akka HTTP em 8080, define `Message` JSON e monta as rotas.

### `src/main/scala/com/codesolutions/akka/domain/`
- `Order.scala` — modelo de domínio `case class Order(orderId, customerId, amount, status, createdAt)`.

### `src/main/scala/com/codesolutions/akka/persistence/`
- `OrderProtocol.scala` — ADTs `OrderCommand`, `OrderEvent`, respostas (`OrderCreatedResp`, `OrderFound`, `OrderNotFound`, `OrderRejected`).
- `OrderPersistentActor.scala` — `EventSourcedBehavior` com snapshot a cada 100 eventos, tagger `order-event`, validações.
- `OrderService.scala` — trait `OrderService` + impl `InMemoryOrderService` (default) + `AkkaOrderService` (bind com actor via ask-pattern).

### `src/main/scala/com/codesolutions/akka/http/`
- `OrderRoutes.scala` — rotas REST (`/orders`) + JSON via Spray-JSON, error handling 400/404.

### `src/test/scala/com/codesolutions/akka/`
- `AkkaHttpBaseSpec.scala` — testes do base (hello, legacy).
- `http/OrderRoutesSpec.scala` — ciclo de vida HTTP completo (POST, GET, PUT, 404, 400).
- `persistence/OrderPersistentActorSpec.scala` — state machine + validação do event sourcing.

---

## Como rodar

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

## Como testar

```bash
sbt test
```

## Migração para persistência real (produção)

1. `docker run -p 9042:9042 cassandra:4.1`
2. Em `application.conf`, mudar:
   ```hocon
   akka.persistence.journal.plugin = "akka.persistence.cassandra.journal"
   akka.persistence.snapshot-store.plugin = "akka.persistence.cassandra.snapshot"
   ```
3. Adicionar `akka-persistence-cassandra` ao `build.sbt`.
4. Bindar `OrderRoutes` ao `OrderPersistentActor` via ask-pattern (exemplo completo em `scala-akka-aws-microservice`).
