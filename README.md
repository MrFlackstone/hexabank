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
- **Consistencia y resiliencia de la saga**: **patrón outbox** (publicación transaccional, cierra el
  dual-write), **Idempotency-Key** opcional en `POST /v1/transfers` (deduplica reintentos de cliente) y un
  **reaper** que recupera las sagas atascadas (timeout → compensación / fallo).
- **Concurrencia**: bloqueo optimista (`@Version`) y orden por clave de partición (`accountId`);
  **virtual threads** (Java 21) para escalar peticiones concurrentes, **rate limiting** por principal
  (Bucket4j) en `POST /v1/transfers` y una **prueba de carga k6** que verifica cero sobregiro bajo ráfaga.
- **Observabilidad**: métricas técnicas (latencia HTTP, consumer lag de Kafka) y de negocio
  (transferencias completadas/fallidas) en Prometheus, visualizadas en un dashboard de Grafana;
  **tracing distribuido** (Micrometer Tracing + OpenTelemetry → Tempo): una transferencia produce una
  única traza que cruza los 3 servicios y el broker Kafka, propagando el contexto a través del outbox.

## Stack

- Java 21 (LTS) · Spring Boot 3.5 · Maven multi-módulo (con wrapper, sin instalación global)
- Apache Kafka 3.9 en modo **KRaft** (sin Zookeeper) · PostgreSQL 16 (una base por servicio)
- Frontend: React 18 + Vite + TypeScript + TanStack Query + react-i18next (ES/EN), servido por **Nginx** (reverse-proxy `/api`)
- Observabilidad: Spring Boot Actuator + Micrometer (métricas) + Prometheus + Grafana (provisionado); **Micrometer Tracing + OpenTelemetry (OTLP) → Grafana Tempo** (trazas)
- Tests: JUnit 5 · Mockito · AssertJ · **Testcontainers** (Kafka + Postgres) · Awaitility
- Docker Compose para levantar toda la demo en local

## Arquitectura

```
                    http://localhost:3000
                  ┌────────────────────────┐
                  │   frontend (React)      │   Nginx sirve el SPA y hace
                  │   + reverse-proxy /api  │   reverse-proxy a los 3 servicios
                  └───────────┬────────────┘
     POST /v1/transfers       │  GET /v1/accounts · GET /v1/transfers/{id} · SSE /v1/ledger/stream
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
          Prometheus (scrape /actuator/prometheus) ─┐
          Tempo (trazas OTLP de los 3 servicios) ───┴──► Grafana
```

- **account-service** (`:8081`) — fuente de verdad de cuentas y saldos. Consume comandos
  (`DebitRequested`/`CreditRequested`/`RefundRequested`), aplica el cambio de saldo de forma idempotente
  y publica el resultado (`MoneyDebited`/`DebitFailed`/`MoneyCredited`/`CreditFailed`/`MoneyRefunded`).
- **transfer-service** (`:8082`) — orquestador de la saga. Expone `POST /v1/transfers`, posee la máquina de
  estados `PENDING → DEBITED → COMPLETED | COMPENSATING → FAILED` y decide cada paso publicando comandos
  y los eventos terminales `TransferCompleted`/`TransferFailed`.
- **ledger-service** (`:8083`) — lado de lectura (CQRS). Consume `account-events` + `transfer-events`,
  materializa un libro mayor append-only y una proyección de transferencias, los expone por REST
  (`GET /v1/ledger`, `GET /v1/transfers/{id}`) y emite un feed en vivo por SSE (`GET /v1/ledger/stream`).
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

- **App**: http://localhost:3000 — **requiere login** (OIDC contra Keycloak). Usuarios de demo:
  `alice` / `bob` (rol `USER`, solo sus cuentas) y `staff` (rol `STAFF`, todas); la contraseña
  coincide con el usuario. Tras entrar: crear cuentas, transferir y ver el ledger en vivo.
- **Keycloak**: http://localhost:8180 — consola admin (`admin`/`admin`), realm `hexabank`.
- **Grafana**: http://localhost:3001 — dashboard _HexaBank — Observabilidad_ (throughput, consumer lag,
  latencia HTTP). Acceso anónimo de solo lectura.
- **Prometheus**: http://localhost:9090 — _Status → Targets_ (3 servicios `UP`).
- **Trazas (Tempo)**: en Grafana → _Explore_ → datasource **Tempo**; busca una traza `http post /transfers`
  y verás los spans encadenados `transfer → account → ledger` cruzando Kafka.
- **Kafka UI**: http://localhost:8080 — topics, particiones, consumer-groups y `*.DLT`.

> Grafana y Prometheus se publican solo en `127.0.0.1` (no alcanzables desde la red). Parar y limpiar:
> `docker compose down -v`.

### Opción B — desarrollo con Maven

```bash
# 1. Infraestructura (Kafka KRaft + Kafka UI + un Postgres por servicio + Keycloak y su Postgres)
docker compose up -d kafka kafka-ui postgres-account postgres-transfer postgres-ledger \
                     postgres-keycloak keycloak

# 2. Build + tests de todo el monorepo (unit + integración con Testcontainers)
./mvnw verify

# 3. Arrancar los servicios (cada uno en su terminal). Al correr en el host, apunta el emisor a
#    localhost:8180 (los servicios no alcanzan el nombre interno keycloak:8080 fuera de compose):
export SPRING_SECURITY_OAUTH2_RESOURCESERVER_JWT_ISSUER_URI=http://localhost:8180/realms/hexabank
./mvnw -pl account-service  spring-boot:run    # http://localhost:8081
./mvnw -pl transfer-service spring-boot:run    # http://localhost:8082
./mvnw -pl ledger-service   spring-boot:run    # http://localhost:8083
```

### Probar una transferencia por API

Todos los endpoints exigen un JWT válido (sin token → 401). Se obtiene un access token de Keycloak
por password grant (habilitado en el client de demo) y se manda en `Authorization: Bearer`:

```bash
# Token de alice (rol USER). La cuenta origen debe pertenecer al llamante (se deriva del sub).
TOKEN=$(curl -s localhost:8180/realms/hexabank/protocol/openid-connect/token \
     -d grant_type=password -d client_id=hexabank-frontend \
     -d username=alice -d password=alice | jq -r .access_token)
AUTH="Authorization: Bearer $TOKEN"

# Crear dos cuentas (quedan a nombre de alice, su dueña)
curl -X POST localhost:8081/v1/accounts -H "$AUTH" -H 'Content-Type: application/json' \
     -d '{"holder":"Ana","initialBalance":100.00}'
curl -X POST localhost:8081/v1/accounts -H "$AUTH" -H 'Content-Type: application/json' \
     -d '{"holder":"Luis","initialBalance":0.00}'

# Lanzar la transferencia (usa los ids devueltos arriba; el origen ha de ser de alice)
curl -X POST localhost:8082/v1/transfers -H "$AUTH" -H 'Content-Type: application/json' \
     -d '{"sourceAccountId":"<id-ana>","destinationAccountId":"<id-luis>","amount":40.00}'

# Ver el estado de la saga, los saldos y el libro mayor
curl localhost:8082/v1/transfers/<id-transfer> -H "$AUTH"    # status: COMPLETED
curl localhost:8081/v1/accounts/<id-ana>       -H "$AUTH"    # balance: 60.00
curl "localhost:8083/v1/ledger?accountId=<id-ana>" -H "$AUTH" # asientos DEBIT/CREDIT
```

> Con la demo completa (Opción A) las llamadas van por Nginx en `localhost:3000/api`. Una transferencia
> por importe mayor que el saldo termina en `FAILED` con los saldos intactos.

### Documentación de la API (Swagger UI)

Cada servicio publica su **OpenAPI 3** y una **Swagger UI** navegable (springdoc):

| Servicio         | Swagger UI                            | OpenAPI JSON                      |
| ---------------- | ------------------------------------- | --------------------------------- |
| account-service  | http://localhost:8081/swagger-ui.html | http://localhost:8081/v3/api-docs |
| transfer-service | http://localhost:8082/swagger-ui.html | http://localhost:8082/v3/api-docs |
| ledger-service   | http://localhost:8083/swagger-ui.html | http://localhost:8083/v3/api-docs |

Pulsa **Authorize** para iniciar sesión por **OAuth2 authorization-code + PKCE** contra Keycloak
(realm `hexabank`, client `hexabank-frontend`); tras el login Swagger adjunta el Bearer a cada
petición y el _Try it out_ funciona autenticado. El endpoint SSE `GET /v1/ledger/stream` queda
documentado pero no se prueba bien desde Swagger UI (usa el frontend o `curl`).

> Los endpoints están versionados bajo **`/v1`**. En la demo completa el frontend los consume por
> `localhost:3000/api/*`, que Nginx reescribe a `/v1/*` en cada servicio (el frontend no cambia).

## Puertos

| Servicio                             | Puerto host            |
| ------------------------------------ | ---------------------- |
| frontend (Nginx, entrada de la demo) | 3000                   |
| account-service                      | 8081                   |
| transfer-service                     | 8082                   |
| ledger-service                       | 8083                   |
| Keycloak (IdP OIDC)                  | 8180                   |
| Kafka (host)                         | 29092                  |
| Kafka UI                             | 8080                   |
| Prometheus                           | 127.0.0.1:9090         |
| Grafana                              | 127.0.0.1:3001         |
| Tempo (trazas, API de consulta)      | 127.0.0.1:3200         |
| postgres-account                     | 5442 (db `accountdb`)  |
| postgres-transfer                    | 5443 (db `transferdb`) |
| postgres-ledger                      | 5444 (db `ledgerdb`)   |
| postgres-keycloak                    | 5445 (db `keycloakdb`) |

> Los Postgres usan el bloque de puertos host **544x** para no chocar con un PostgreSQL nativo en 5432.
> Credenciales locales: `hexabank` / `hexabank`.

## Observabilidad

Cada servicio expone `/actuator/prometheus` (modelo _pull_: Prometheus lo raspa cada 5s). El dashboard
de Grafana se provisiona automáticamente (datasource + paneles) desde `infra/grafana/`:

- **Throughput de transferencias** — contadores de negocio `hexabank_transfers_completed_total` /
  `hexabank_transfers_failed_total`, instrumentados vía el puerto de salida `TransferMetricsPort`.
- **Consumer lag de Kafka** — `kafka_consumer_fetch_manager_records_lag_max` por servicio.
- **Latencia HTTP p95** — quantiles a partir de los histogramas de `http_server_requests`.

### Tracing distribuido (Tempo)

Los 3 servicios exportan trazas por **OTLP** a **Grafana Tempo** (Micrometer Tracing + OpenTelemetry,
muestreo al 100%). Una transferencia genera **una única traza** que atraviesa `transfer → account →
ledger` y el broker Kafka. El reto está en el **outbox**: el evento se publica en un hilo distinto (el
relay `@Scheduled`), donde ya no hay contexto de traza. Se resuelve **persistiendo el `traceparent` W3C
en la fila del outbox** al encolar y **reinyectándolo como cabecera Kafka** en el relay; el consumidor
continúa la misma traza. En Grafana, _Explore → Tempo_; los exemplars enlazan cada métrica de Prometheus
con su traza.

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
  infra/              prometheus.yml + tempo.yaml + provisioning de Grafana (datasources + dashboard)
  docker-compose.yml  Kafka KRaft + Kafka UI + 3 Postgres + 3 servicios + frontend + Prometheus + Grafana + Tempo
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
# Build en dos fases: la primera instala los 'stubs' jar de contrato en el repo local; la
# segunda ejecuta toda la suite (los tests de contrato del consumidor los resuelven de ahi).
./mvnw -B -DskipTests clean install    # instala artefactos y stubs de contrato
./mvnw -B verify                       # todo el monorepo (requiere Docker)
./mvnw -pl account-service  -am verify # un servicio
```

Los tests unitarios (`*Test`) corren sin contenedores; los de integración (`*IT`) levantan Kafka y
Postgres con Testcontainers. Los **tests de contrato** (Spring Cloud Contract, ver abajo) también son
`*IT` con Testcontainers.

### Contract testing de eventos (Spring Cloud Contract)

Cada productor de eventos define **contratos** (Groovy) de los mensajes que publica; SCC genera un
test que verifica que el productor los emite tal cual (body JSON + cabecera `__TypeId__`) y publica un
`stubs` jar. Cada consumidor reproduce esos stubs con **stub-runner** y comprueba que su
`@KafkaListener` real procesa el mensaje y produce el efecto de la saga. Cobertura del mesh de eventos:

| Topic | Productor | Consumidor(es) verificados |
| --- | --- | --- |
| `account-events` | account-service | transfer-service, ledger-service |
| `account-commands` | transfer-service | account-service |
| `transfer-events` | transfer-service | ledger-service |

Una ruptura de esquema de un evento rompe el test del productor **y** el del consumidor antes de
llegar a producción. (`CreditFailed` no tiene contrato: el código actual nunca lo emite.)

## Integración continua

Cada push y cada Pull Request disparan el workflow [`.github/workflows/ci.yml`](.github/workflows/ci.yml)
en GitHub Actions (dos jobs en paralelo):

- **backend** — sobre `ubuntu-latest`, build en dos fases (`./mvnw -B -DskipTests clean install` para
  publicar los `stubs` jar de contrato, luego `./mvnw -B verify`): tests unitarios + integración con
  **Testcontainers** (Kafka + Postgres reales) + contract testing + e2e. El runner trae Docker, así
  que los `*IT` corren igual que en local, sin configurar servicios externos.
- **frontend** — `npm ci && npm run build` (type-check con `tsc` + bundle con Vite).

Además, un tercer job **loadtest** (prueba de carga k6) **no corre en cada PR**: solo a demanda
(`workflow_dispatch`) o al integrar en `main`, porque levanta el stack completo por Compose. Los PRs
siguen cubiertos por los Testcontainers del job _backend_.

El badge de arriba refleja el estado de la rama `main`.

## Benchmark: virtual threads

Los 3 servicios corren con **virtual threads** (`spring.threads.virtual.enabled=true`, Java 21). Para
medir su efecto, la prueba de carga k6 (`loadtest/concurrent-transfers.js`) dispara **300 transferencias
concurrentes** sobre la misma cuenta (saldo suficiente para que todas completen) con y sin virtual
threads. Se compara la latencia de las peticiones HTTP (`http_req_duration`, dominada por los 300
`POST /v1/transfers` simultáneos). El rate limiting se desactiva en el benchmark para no estrangular la
ráfaga y aislar el efecto de los hilos.

| Configuración                 | media  | mediana | p95    | máx    |
| ----------------------------- | ------ | ------- | ------ | ------ |
| Sin virtual threads (platform) | 465 ms | 551 ms  | 635 ms | 680 ms |
| Con virtual threads            | 135 ms | 148 ms  | 212 ms | 268 ms |

Con 300 peticiones simultáneas —por encima del pool de 200 hilos de plataforma de Tomcat— los virtual
threads recortan la latencia p95 **~3×**: sin ellos, las peticiones que exceden el pool esperan a que se
libere un hilo; con ellos, cada petición corre en su propio hilo virtual. En ambos casos las 300
transferencias liquidan con **cero sobregiro** y saldo final exacto (garantía de concurrencia:
bloqueo optimista + consumidores idempotentes). Medido en local (Docker Compose); cifras orientativas.

> Reproducir: `docker compose -f docker-compose.yml -f loadtest/docker-compose.loadtest.yml up -d --build`
> y `docker run --rm --network host -v "$PWD/loadtest:/loadtest" grafana/k6 run /loadtest/concurrent-transfers.js`
> (con `N=300 INITIAL_BALANCE=3000`). Para la comparativa, recrear los servicios con
> `SPRING_THREADS_VIRTUAL_ENABLED=false`.

## Mapeo requisito → implementación

Cómo cada requisito de la oferta se materializa en el código:

| Requisito | Dónde se demuestra |
| --- | --- |
| **Java + Spring Boot** | 3 servicios en Java 21 / Spring Boot 3.5 (`account-service`, `transfer-service`, `ledger-service`) |
| **APIs REST** | Controllers en `infrastructure/rest` con DTOs + Bean Validation; errores como `ProblemDetail` (`RestExceptionHandler`); endpoints versionados `/v1` |
| **Contrato de API** | **OpenAPI 3 + Swagger UI** por servicio (springdoc) con login **OAuth2 authorization-code + PKCE** contra Keycloak; DTOs y endpoints anotados (`@Operation`/`@ApiResponses`/`@Schema`) |
| **Contract testing** | **Spring Cloud Contract** sobre los eventos Kafka: contratos por productor (account/transfer) verificados en CI + verificación del consumidor con **stub-runner** en los 3 servicios (`account`↔`transfer`↔`ledger`) |
| **Arquitectura hexagonal + SOLID** | `domain`/`application` sin framework; puertos segregados en `port/in` y `port/out`; adaptadores en `infrastructure`; wiring explícito en `config/BeanConfiguration` (inversión de dependencias real) |
| **Kafka / event-driven** | Saga de orquestación sobre topics particionados (`account-commands`/`account-events`/`transfer-events`); `@KafkaListener` + `KafkaTemplate`; máquina de estados con **compensación** (`RefundRequested`) |
| **Alta concurrencia** | Bloqueo optimista `@Version` + `saveAndFlush`; consumidores **idempotentes** (`processed_events` por `eventId`) frente al at-least-once; orden por clave de partición; servicios stateless; **virtual threads** (Java 21) en los 3 servicios; **rate limiting** por principal (Bucket4j) en `POST /v1/transfers`; **prueba de carga k6** (`loadtest/`) de N transferencias concurrentes con aserción de cero sobregiro |
| **Robustez del consumidor** | `DefaultErrorHandler` + `DeadLetterPublishingRecoverer` con backoff → `*.DLT` para mensajes irrecuperables |
| **Resiliencia de la saga** | **Outbox** transaccional (relay `@Scheduled`, cierra el dual-write) en transfer + account; **Idempotency-Key** opcional en `POST /v1/transfers`; **reaper** `@Scheduled` de sagas atascadas (`PENDING`/`DEBITED`/`COMPENSATING` → transición segura) con reembolso idempotente por `transferId` |
| **CI + revisión de código** | GitHub Actions (`ci.yml`) en cada push/PR; el trabajo entra por Pull Requests |
| **Testing** | JUnit 5 / Mockito / AssertJ (dominio) + **Testcontainers** Kafka+Postgres (integración) + Awaitility (saga e2e) |
| **Observabilidad** | Actuator + Micrometer → Prometheus (`/actuator/prometheus`); métrica de negocio vía puerto `TransferMetricsPort`; dashboard de Grafana provisionado; **tracing distribuido** (Micrometer Tracing + OTLP → Tempo) con propagación del contexto a través del outbox/Kafka |
| **Autenticación / autorización** | OIDC con **Keycloak** (realm-as-code); los 3 servicios son OAuth2 Resource Server (JWT Bearer, validación de firma/`iss`/`aud`); autorización por **propiedad** (proyección CQRS de ownership) + roles `USER`/`STAFF` (`@PreAuthorize`), **404 anti existence-oracle**; **login OIDC** (Authorization Code + PKCE) en el frontend React, con SSE autenticado por cabecera Bearer |

## Roadmap

Auth end-to-end (Keycloak OIDC, ownership + roles, login del frontend), **resiliencia de la saga**
(patrón outbox, Idempotency-Key, DLT con retry/backoff y reaper), **concurrencia demostrable**
(virtual threads, rate limiting, prueba de carga k6) y **tracing distribuido** (Micrometer Tracing +
OTLP → Tempo, con propagación del contexto a través del outbox/Kafka), el **contrato de API**
(OpenAPI/Swagger UI con OAuth2, endpoints versionados `/v1`) y el **contract testing de eventos**
(Spring Cloud Contract: contratos por productor + verificación del consumidor con stub-runner en los
tres servicios) ya implementados.

- El _Schema Registry_ (Avro) se evaluó y se descarta: se mantiene **JSON + cabeceras de tipo**, una
  decisión consciente para esta iteración; el contract testing cubre el contrato de eventos con mucho
  menos riesgo.
