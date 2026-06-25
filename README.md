# HexaBank

Demo de **microservicios hexagonales** comunicados por una **saga Kafka**, en el dominio de
banca/pagos (transferencias). Construido como showcase técnico: arquitectura hexagonal,
event-driven con Kafka y alta concurrencia.

> Estado: **Fase 1 — account-service**. Primer microservicio hexagonal (dominio puro, `@Version`,
> REST + JPA + Flyway, tests con Testcontainers) sobre la infraestructura de la Fase 0.

## Stack
- Java 21 (LTS) · Spring Boot 3.5.x · Maven multi-módulo (con wrapper, sin instalación global)
- Apache Kafka 3.9 (modo **KRaft**, sin Zookeeper) · PostgreSQL 16
- Docker Compose · (Testcontainers, CI y observabilidad en fases posteriores)

## Arquitectura (objetivo)
Tres servicios hexagonales (`account-service`, `transfer-service`, `ledger-service`) + frontend
React, coordinados por una **saga de orquestación** sobre Kafka con compensación. Detalle en
`../plans/current/hexabank-minsait-plan.md`.

## Arrancar la infraestructura
```bash
docker compose up -d            # Kafka (KRaft) + Kafka UI + postgres-account
docker compose ps               # todos healthy
```
- Kafka UI: http://localhost:8080
- Kafka (host): `localhost:29092`
- `postgres-account` (host): `localhost:5442` (hexabank/hexabank, db `accountdb`)

> Nota: los Postgres del proyecto usan el bloque de puertos host **544x** (account=5442;
> transfer=5443, ledger=5444 en sus fases) para no chocar con un PostgreSQL nativo en 5432.

## account-service (Fase 1)
```bash
# Build + tests (unit + integración con Testcontainers); requiere Docker y JDK 21:
JAVA_HOME=".../jdk-21.0.10" ./mvnw -pl account-service verify
# Arrancar el servicio (puerto 8081):
JAVA_HOME=".../jdk-21.0.10" ./mvnw -pl account-service spring-boot:run
```
```bash
# Crear y consultar una cuenta:
curl -X POST localhost:8081/accounts -H "Content-Type: application/json" \
     -d '{"holder":"Diego","initialBalance":100.00}'
curl localhost:8081/accounts/<id>
```

Probar el broker Kafka creando un topic:
```bash
docker exec kafka /opt/kafka/bin/kafka-topics.sh --create --topic test --bootstrap-server localhost:9092
docker exec kafka /opt/kafka/bin/kafka-topics.sh --list   --bootstrap-server localhost:9092
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
  account-service/   microservicio de cuentas (hexagonal: domain/application/infrastructure)
  docker-compose.yml Kafka KRaft + Kafka UI + postgres-account
  infra/             reservado (Prometheus/Grafana, Fase 5)
```
