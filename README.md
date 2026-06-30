# HexaBank

[![CI](https://github.com/MrFlackstone/hexabank/actions/workflows/ci.yml/badge.svg)](https://github.com/MrFlackstone/hexabank/actions/workflows/ci.yml)

Demo de **microservicios hexagonales** en Java 21 + Spring Boot 3 que ejecutan una **saga de
transferencias bancarias** sobre Apache Kafka, con **idempotencia**, **compensación** y **Dead Letter
Topic**. Incluye un **lado de lectura CQRS** (libro mayor + proyección con actualizaciones en vivo por
SSE), un **frontend React** para operarlo y **observabilidad** (Actuator + Prometheus + Grafana). Pensado
como showcase técnico de arquitectura hexagonal, comunicación event-driven y concurrencia.

> Toda la demo se levanta con un único `docker compose up --build -d` y se opera desde el navegador en
> **http://localhost:3000**.

## Características clave

- **Arquitectura hexagonal (puertos y adaptadores)** en cada servicio: el dominio es Java puro (sin
  Spring/JPA/Kafka); la infraestructura implementa los puertos. La observabilidad entra como un
  adaptador de salida más (`TransferMetricsPort`), sin ensuciar el núcleo.
- **Saga de orquestación** sobre Kafka: una transferencia es un proceso de varios pasos sobre dos
  cuentas que pueden vivir en servicios y bases de datos distintas, coordinado por eventos.
- **Compensación**: si el dinero sale del origen pero no puede entrar en el destino, se emite un
  reembolso (movimiento de negocio nuevo, no un rollback distribuido).
- **CQRS-lite**: `ledger-service` materializa un libro mayor append-only y una proyección de lectura a
  partir de los eventos, y empuja actualizaciones en vivo al frontend por **SSE**.
- **Robustez del consumidor**: idempotencia (tabla `processed_events`) frente a la entrega
  _at-least-once_, reintentos con backoff y reubicación a `*.DLT` de los mensajes irrecuperables.
- **Concurrencia**: bloqueo optimista (`@Version`) y orden por clave de partición (`accountId`).
- **Observabilidad**: métricas técnicas (latencia HTTP, consumer lag de Kafka) y de negocio
  (transferencias completadas/fallidas) en Prometheus, visualizadas en un dashboard de Grafana.

## Stack

- Java 21 (LTS) · Spring Boot 3.5 · Maven multi-módulo (con wrapper, sin instalación global)
- Apache Kafka 3.9 en modo **KRaft** (sin Zookeeper) · PostgreSQL 16 (una base por servicio)
- Frontend: React 18 + Vite + TypeScript + TanStack Query, servido por **Nginx** (reverse-proxy `/api`)
- Observabilidad: Spring Boot Actuator + Micrometer + Prometheus + Grafana (provisionado)
- Tests: JUnit 5 · Mockito · AssertJ · **Testcontainers** (Kafka + Postgres) · Awaitility
- Docker Compose para levantar toda la demo en local

## Arquitectura

```
                    http://localhost:3000
                  ┌────────────────────────┐
                  │   frontend (React)      │   Nginx sirve el SPA y hace
                  │   + reverse-proxy /api  │   reverse-proxy a los 3 servicios
                  └───────────┬────────────┘
        POST /transfers       │  GET /accounts · GET /transfers/{id} · SSE /ledger/stream
                              ▼
        ┌──────────────┐  account-commands   ┌──────────────┐
        │   transfer   │ ──────────────────► │   account    │
        │   service    │ ◄────────────────── │   service    │
        └──────┬───────┘   account-events    └──────┬───────┘
               │ transfer-events                    │ account-events
               └──────────────┬─────────────────────┘
                              ▼
                      ┌──────────────┐      consume ambos topics → libro mayor
                      │   ledger     │      + proyección de lectura (CQRS)
                      │   service    │ ──── SSE ───► frontend (feed en vivo)
                      └──────┬───────┘
                             ▼
                  Prometheus (scrape /actuator/prometheus) → Grafana
```

- **account-service** (`:8081`) — fuente de verdad de cuentas y saldos. Consume comandos
  (`DebitRequested`/`CreditRequested`/`RefundRequested`), aplica el cambio de saldo de forma idempotente
  y publica el resultado (`MoneyDebited`/`DebitFailed`/`MoneyCredited`/`CreditFailed`/`MoneyRefunded`).
- **transfer-service** (`:8082`) — orquestador de la saga. Expone `POST /transfers`, posee la máquina de
  estados `PENDING → DEBITED → COMPLETED | COMPENSATING → FAILED` y decide cada paso publicando comandos
  y los eventos terminales `TransferCompleted`/`TransferFailed`.
- **ledger-service** (`:8083`) — lado de lectura (CQRS). Consume `account-events` + `transfer-events`,
  materializa un libro mayor append-only y una proyección de transferencias, los expone por REST
  (`GET /ledger`, `GET /transfers/{id}`) y emite un feed en vivo por SSE (`GET /ledger/stream`).
- **frontend** (`:3000`) — SPA en React para crear cuentas, lanzar transferencias y ver el ledger en
  tiempo real; muestra los dos lados de CQRS (estado del orquestador vs proyección de lectura).
- **shared-events** — contratos de eventos compartidos (records sellados), sin dependencias de framework.
- **e2e-tests** — módulo de test que levanta account + transfer juntos y verifica la saga de extremo a extremo.

### Topics de Kafka

| Topic              | Produce          | Consume                          | Clave        |
| ------------------ | ---------------- | -------------------------------- | ------------ |
| `account-commands` | transfer-service | account-service                  | `accountId`  |
| `account-events`   | account-service  | transfer-service, ledger-service | `accountId`  |
| `transfer-events`  | transfer-service | ledger-service                   | `transferId` |
| `*.DLT`            | error handler    | inspección manual                | —            |

## Requisitos

- **Docker** en ejecución (la demo, la infraestructura y los tests de integración usan contenedores).
- **JDK 21** (solo si compilas/ejecutas con Maven fuera de Docker). El build usa el Maven Wrapper
  (`./mvnw`), no hace falta Maven global.

> En Windows, exporta `JAVA_HOME` apuntando al JDK 21 si tu `PATH` tiene otra versión:
> `JAVA_HOME="C:\Program Files\Java\jdk-21.x" ./mvnw ...`

## Arrancar

### Opción A — demo completa (recomendada)

Levanta los 3 servicios, sus Postgres, Kafka, el frontend y la observabilidad:

```bash
docker compose up --build -d
docker compose ps                      # todos los contenedores arriba
```

- **App**: http://localhost:3000 — crear cuentas, transferir y ver el ledger en vivo.
- **Grafana**: http://localhost:3001 — dashboard _HexaBank — Observabilidad_ (throughput, consumer lag,
  latencia HTTP). Acceso anónimo de solo lectura.
- **Prometheus**: http://localhost:9090 — _Status → Targets_ (3 servicios `UP`).
- **Kafka UI**: http://localhost:8080 — topics, particiones, consumer-groups y `*.DLT`.

> Grafana y Prometheus se publican solo en `127.0.0.1` (no alcanzables desde la red). Parar y limpiar:
> `docker compose down -v`.

### Opción B — desarrollo con Maven

```bash
# 1. Solo infraestructura (Kafka KRaft + Kafka UI + un Postgres por servicio)
docker compose up -d kafka kafka-ui postgres-account postgres-transfer postgres-ledger

# 2. Build + tests de todo el monorepo (unit + integración con Testcontainers)
./mvnw verify

# 3. Arrancar los servicios (cada uno en su terminal)
./mvnw -pl account-service  spring-boot:run    # http://localhost:8081
./mvnw -pl transfer-service spring-boot:run    # http://localhost:8082
./mvnw -pl ledger-service   spring-boot:run    # http://localhost:8083
```

### Probar una transferencia por API

```bash
# Crear dos cuentas
curl -X POST localhost:8081/accounts -H 'Content-Type: application/json' \
     -d '{"holder":"Ana","initialBalance":100.00}'
curl -X POST localhost:8081/accounts -H 'Content-Type: application/json' \
     -d '{"holder":"Luis","initialBalance":0.00}'

# Lanzar la transferencia (usa los ids devueltos arriba)
curl -X POST localhost:8082/transfers -H 'Content-Type: application/json' \
     -d '{"sourceAccountId":"<id-ana>","destinationAccountId":"<id-luis>","amount":40.00}'

# Ver el estado de la saga, los saldos y el libro mayor
curl localhost:8082/transfers/<id-transfer>     # status: COMPLETED
curl localhost:8081/accounts/<id-ana>           # balance: 60.00
curl localhost:8083/ledger?accountId=<id-ana>   # asientos DEBIT/CREDIT
```

> Con la demo completa (Opción A) las llamadas van por Nginx en `localhost:3000/api`. Una transferencia
> por importe mayor que el saldo termina en `FAILED` con los saldos intactos.

## Puertos

| Servicio                             | Puerto host            |
| ------------------------------------ | ---------------------- |
| frontend (Nginx, entrada de la demo) | 3000                   |
| account-service                      | 8081                   |
| transfer-service                     | 8082                   |
| ledger-service                       | 8083                   |
| Kafka (host)                         | 29092                  |
| Kafka UI                             | 8080                   |
| Prometheus                           | 127.0.0.1:9090         |
| Grafana                              | 127.0.0.1:3001         |
| postgres-account                     | 5442 (db `accountdb`)  |
| postgres-transfer                    | 5443 (db `transferdb`) |
| postgres-ledger                      | 5444 (db `ledgerdb`)   |

> Los Postgres usan el bloque de puertos host **544x** para no chocar con un PostgreSQL nativo en 5432.
> Credenciales locales: `hexabank` / `hexabank`.

## Observabilidad

Cada servicio expone `/actuator/prometheus` (modelo _pull_: Prometheus lo raspa cada 5s). El dashboard
de Grafana se provisiona automáticamente (datasource + paneles) desde `infra/grafana/`:

- **Throughput de transferencias** — contadores de negocio `hexabank_transfers_completed_total` /
  `hexabank_transfers_failed_total`, instrumentados vía el puerto de salida `TransferMetricsPort`.
- **Consumer lag de Kafka** — `kafka_consumer_fetch_manager_records_lag_max` por servicio.
- **Latencia HTTP p95** — quantiles a partir de los histogramas de `http_server_requests`.

## Estructura

```
hexabank/
  pom.xml             parent POM (Java 21, BOM Spring Boot 3.5)
  shared-events/      contratos de eventos de la saga (records sellados, Java puro)
  account-service/    cuentas y saldos (hexagonal: domain / application / infrastructure)
  transfer-service/   orquestador de la saga (máquina de estados + compensación)
  ledger-service/     lado de lectura CQRS (libro mayor + proyección + SSE)
  e2e-tests/          test de la saga de extremo a extremo (account + transfer)
  frontend/           SPA React (Vite + TS), servida por Nginx con reverse-proxy /api
  infra/              prometheus.yml + provisioning de Grafana (datasource + dashboard)
  docker-compose.yml  Kafka KRaft + Kafka UI + 3 Postgres + 3 servicios + frontend + Prometheus + Grafana
```

Cada servicio sigue la misma estructura hexagonal:

```
<service>/src/main/java/com/hexabank/<service>/
  domain/            entidades y reglas de negocio (sin framework)
  application/
    port/in          puertos de entrada (casos de uso)
    port/out         puertos de salida (repositorios, publicador de eventos, métricas)
    service          implementación de los casos de uso
  infrastructure/
    rest             adaptadores REST (y SSE en ledger-service)
    messaging        adaptadores Kafka (listener + publicador)
    persistence      adaptadores JPA
    metrics          adaptador de métricas Micrometer (transfer-service)
  config/            cableado de Spring
```

## Tests

```bash
./mvnw verify                          # todo el monorepo (requiere Docker)
./mvnw -pl account-service  -am verify # un servicio
./mvnw -pl e2e-tests        -am verify # saga de extremo a extremo
```

Los tests unitarios (`*Test`) corren sin contenedores; los de integración (`*IT`) levantan Kafka y
Postgres con Testcontainers.

## Integración continua

Cada push y cada Pull Request disparan el workflow [`.github/workflows/ci.yml`](.github/workflows/ci.yml)
en GitHub Actions (dos jobs en paralelo):

- **backend** — `./mvnw -B verify` sobre `ubuntu-latest`: tests unitarios + integración con
  **Testcontainers** (Kafka + Postgres reales) + e2e. El runner trae Docker, así que los `*IT`
  corren igual que en local, sin configurar servicios externos.
- **frontend** — `npm ci && npm run build` (type-check con `tsc` + bundle con Vite).

El badge de arriba refleja el estado de la rama `main`.

## Mapeo requisito → implementación

Cómo cada requisito de la oferta se materializa en el código:

| Requisito | Dónde se demuestra |
| --- | --- |
| **Java + Spring Boot** | 3 servicios en Java 21 / Spring Boot 3.5 (`account-service`, `transfer-service`, `ledger-service`) |
| **APIs REST** | Controllers en `infrastructure/rest` con DTOs + Bean Validation; errores como `ProblemDetail` (`RestExceptionHandler`) |
| **Arquitectura hexagonal + SOLID** | `domain`/`application` sin framework; puertos segregados en `port/in` y `port/out`; adaptadores en `infrastructure`; wiring explícito en `config/BeanConfiguration` (inversión de dependencias real) |
| **Kafka / event-driven** | Saga de orquestación sobre topics particionados (`account-commands`/`account-events`/`transfer-events`); `@KafkaListener` + `KafkaTemplate`; máquina de estados con **compensación** (`RefundRequested`) |
| **Alta concurrencia** | Bloqueo optimista `@Version` + `saveAndFlush`; consumidores **idempotentes** (`processed_events` por `eventId`) frente al at-least-once; orden por clave de partición; servicios stateless |
| **Robustez del consumidor** | `DefaultErrorHandler` + `DeadLetterPublishingRecoverer` con backoff → `*.DLT` para mensajes irrecuperables |
| **CI + revisión de código** | GitHub Actions (`ci.yml`) en cada push/PR; el trabajo entra por Pull Requests |
| **Testing** | JUnit 5 / Mockito / AssertJ (dominio) + **Testcontainers** Kafka+Postgres (integración) + Awaitility (saga e2e) |
| **Observabilidad** | Actuator + Micrometer → Prometheus (`/actuator/prometheus`); métrica de negocio vía puerto `TransferMetricsPort`; dashboard de Grafana provisionado |

## Roadmap

- **Autenticación / autorización** — JWT resource-server por servicio.
