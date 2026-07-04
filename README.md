# akka-scala-base

[![CI](https://github.com/ivamartins/akka-scala-base/actions/workflows/ci.yml/badge.svg)](https://github.com/ivamartins/akka-scala-base/actions/workflows/ci.yml)
[![License: MIT](https://img.shields.io/badge/License-MIT-yellow.svg)](LICENSE)

> Part of the **Code Solutions Event-Driven & Streaming Toolkit** product line. Resilient services with Akka Typed, clustering, persistence (event-sourcing), and integrations for modern backends.

Minimal, functional Akka Typed base for building resilient services.

## Why this base

- **Akka Typed** + HTTP, ready for clustering, persistence (event-sourcing), and modern integrations
- **Real-world foundation** — extracted from the same patterns used to build the event-driven stack at **Sicredi Digital** (PFM crawler → full event-driven reference architecture)
- **Composable** with Flink/Kafka streams (see `flink-kafka-scala-base`) and Akka HTTP

## Quick start

**Prerequisites:** Java + sbt.

```bash
# Run the example
sbt run
```

It will start an Akka HTTP server with a sample actor (event-sourced counter) and a health endpoint.

## Run the tests

```bash
sbt test
```

## Extend for real use

- Add your domain actors with `EventSourcedBehavior` for event-sourcing
- Configure Akka Cluster for horizontal scalability
- Add Akka Streams / Alpakka for Kafka integration
- Add Akka HTTP routes for your endpoints
- Add observability with Kamon or OpenTelemetry

## Tech stack

- Scala 2.12
- Akka Typed 2.5.x
- Akka HTTP
- Akka Persistence (event-sourcing)
- sbt build tool
- ScalaTest (unit tests)

> **Português?** Veja [`README.pt-BR.md`](./README.pt-BR.md).

## See also

- **Related base**: [scala-akka-aws-microservice](https://github.com/ivamartins/scala-akka-aws-microservice), [flink-kafka-scala-base](https://github.com/ivamartins/flink-kafka-scala-base)
- **Product line**: [Event-Driven & Streaming Toolkit](https://ivamartins.github.io/code-solutions-site/#produtos)
- **Code Solutions on LinkedIn**: [linkedin.com/company/code-solutions-it](https://www.linkedin.com/company/code-solutions-it/)
- **All Code Solutions open source**: [github.com/ivamartins](https://github.com/ivamartins)

## License

MIT — see `LICENSE`.
