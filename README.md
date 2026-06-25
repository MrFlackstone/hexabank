# HexaBank

Demo de **microservicios hexagonales** comunicados por una **saga Kafka**, en el dominio de
banca/pagos (transferencias). Construido como showcase técnico: arquitectura hexagonal,
event-driven con Kafka y alta concurrencia.

> Estado: **Fase 0 — Scaffolding**. Infraestructura base (Kafka KRaft + Postgres) y esqueleto
> del monorepo Maven. Los servicios se construyen en las fases siguientes.

## Stack
- Java 21 (LTS) · Spring Boot 3.5.x · Maven multi-módulo (con wrapper, sin instalación global)
- Apache Kafka 3.9 (modo **KRaft**, sin Zookeeper) · PostgreSQL 16
- Docker Compose · (Testcontainers, CI y observabilidad en fases posteriores)

## Arquitectura (objetivo)
Tres servicios hexagonales (`account-service`, `transfer-service`, `ledger-service`) + frontend
React, coordinados por una **saga de orquestación** sobre Kafka con compensación. Detalle en
`../plans/current/hexabank-minsait-plan.md`.

## Arrancar la infraestructura (Fase 0)
```bash
docker compose up -d            # Kafka (KRaft) + Postgres + Kafka UI
docker compose ps               # todos healthy
```
- Kafka UI: http://localhost:8080
- Kafka (host): `localhost:29092`  ·  Postgres: `localhost:5432` (hexabank/hexabank)

Probar el broker creando un topic:
```bash
docker exec kafka /opt/kafka/bin/kafka-topics.sh --create --topic test --bootstrap-server localhost:9092
docker exec kafka /opt/kafka/bin/kafka-topics.sh --list   --bootstrap-server localhost:9092
```

Build del monorepo (sin Maven global, vía wrapper):
```bash
./mvnw validate
```

Limpiar:
```bash
docker compose down -v
```

## Estructura
```
hexabank/
  pom.xml            parent POM (Java 21, BOM Spring Boot 3.5.x)
  shared-events/     contratos de eventos compartidos (Java puro)
  docker-compose.yml Kafka KRaft + Postgres + Kafka UI
  infra/             reservado (Prometheus/Grafana, Fase 5)
```
