# Fluxo de interação entre classes — akka-scala-base

Visualização rápida de como uma requisição HTTP atravessa o sistema até o actor de persistência.

## 1. Bootstrap

```
AkkaHttpBase.main()                [AkkaHttpBase.scala]
  └─> ActorSystem(Behaviors.empty, "akka-http-base")
        └─> Http().newServerAt(0.0.0.0, 8080).bind(createRoute())
              └─> OrderRoutes (instanciado com InMemoryOrderService)
```

## 2. Criar pedido — `POST /orders`

```
Cliente HTTP (curl)
  └─> OrderRoutes.post()                 [http/OrderRoutes]
        └─> OrderService.create()        [persistence/OrderService — InMemoryOrderService]
              └─> Map.put(orderId, order)
```

**Caminho resumido:**
`AkkaHttpBase → OrderRoutes → InMemoryOrderService (Map em memória)`

> Em produção, `InMemoryOrderService` é trocado por `AkkaOrderService`, que faz `actorRef.ask(...)` para o `OrderPersistentActor`.

## 3. Buscar pedido — `GET /orders/{orderId}`

```
Cliente HTTP (curl)
  └─> OrderRoutes.get()                  [http/OrderRoutes]
        └─> OrderService.get(orderId)    [persistence/OrderService — InMemoryOrderService]
              └─> Map.get(orderId)
```

## 4. Mudar status — `PUT /orders/{orderId}`

```
Cliente HTTP (curl)
  └─> OrderRoutes.put()                  [http/OrderRoutes]
        └─> OrderService.changeStatus()  [persistence/OrderService — InMemoryOrderService]
              └─> Map.update(orderId, newStatus)
```

## 5. Modo produção (Akka Persistence + Cassandra)

```
Cliente HTTP (curl)
  └─> OrderRoutes                        [http/OrderRoutes]
        └─> AkkaOrderService             [persistence/OrderService]
              └─> actorRef.ask(CreateOrder)  ──> OrderPersistentActor
                                                  └─> EventSourcedBehavior
                                                        ├─> persist(OrderCreated)    ──> Cassandra journal
                                                        └─> snapshotEvery(100)       ──> Cassandra snapshot
```

**Caminho resumido (prod):**
`AkkaHttpBase → OrderRoutes → AkkaOrderService → OrderPersistentActor (EventSourcedBehavior) → Cassandra`

## 6. Endpoints auxiliares

```
Cliente HTTP
  ├─> GET /hello       ──> AkkaHttpBase.createRoute() (resposta fixa)
  └─> GET /legacy/:id  ──> AkkaHttpBase.createRoute() (resposta fixa)
```

## Mapa de pacotes

```
com.codesolutions.akka
├── AkkaHttpBase.scala              ← main + roteamento
├── domain/
│   └── Order.scala                 ← case class
├── persistence/
│   ├── OrderProtocol.scala          ← Commands, Events, Responses (sealed traits)
│   ├── OrderPersistentActor.scala   ← EventSourcedBehavior
│   └── OrderService.scala           ← trait + InMemory + Akka impls
└── http/
    └── OrderRoutes.scala            ← Akka HTTP routes
```

## Erros

`OrderRoutes` (em `http/`) responde 404 quando o orderId não existe e 400 quando o payload é inválido (JSON malformado), via `Spray-JSON` `deserializationError`.
