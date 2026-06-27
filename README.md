# HexaBank

Demo de **microservicios hexagonales** en Java 21 + Spring Boot 3 que ejecutan una **saga de
transferencias bancarias** sobre Apache Kafka, con **idempotencia**, **compensación** y **Dead Letter
Topic**. Pensado como showcase técnico de arquitectura hexagonal, comunicación event-driven y
concurrencia.

## Qué demuestra

- **Arquitectura hexagonal (puertos y adaptadores)** en cada servicio: el dominio es Java puro (sin
  Spring/JPA/Kafka); la infraestructura implementa los puertos.
- **Saga de orquestación** sobre Kafka: una transferencia es un proceso de varios pasos sobre dos
  cuentas que pueden vivir en servicios y bases de datos distintas, coordinado por eventos.
- **Compensación**: si el dinero sale del origen pero no puede entrar en el destino, se emite un
  reembolso (movimiento de negocio nuevo, no un rollback distribuido).
- **Robustez del consumidor**: idempotencia (tabla `processed_events`) frente a la entrega
  *at-least-once*, reintentos con backoff y reubicación a `*.DLT` de los mensajes irrecuperables.
- **Concurrencia**: bloqueo optimista (`@Version`) y orden por clave de partición (`accountId`).

## Stack

- Java 21 (LTS) · Spring Boot 3.5 · Maven multi-módulo (con wrapper, sin instalación global)
- Apache Kafka 3.9 en modo **KRaft** (sin Zookeeper) · PostgreSQL 16 (una base por servicio)
- Tests: JUnit 5 · Mockito · AssertJ · **Testcontainers** (Kafka + Postgres) · Awaitility
- Docker Compose para la infraestructura local

## Arquitectura

```
                 POST /transfers
                       │
                       ▼
                ┌──────────────┐   account-commands    ┌──────────────┐
                │   transfer   │ ────────────────────► │   account    │
                │   service    │ ◄──────────────────── │   service    │
                └──────┬───────┘    account-events     └──────────────┘
                       │  transfer-events
                       ▼
                 (ledger-service, futuro)
```

- **account-service** (`:8081`) — fuente de verdad de cuentas y saldos. Consume comandos
  (`DebitRequested`/`CreditRequested`/`RefundRequested`), aplica el cambio de saldo de forma idempotente
  y publica el resultado (`MoneyDebited`/`DebitFailed`/`MoneyCredited`/`CreditFailed`/`MoneyRefunded`).
- **transfer-service** (`:8082`) — orquestador de la saga. Expone `POST /transfers`, posee la máquina de
  estados `PENDING → DEBITED → COMPLETED | COMPENSATING → FAILED` y decide cada paso publicando comandos
  y los eventos terminales `TransferCompleted`/`TransferFailed`.
- **shared-events** — contratos de eventos compartidos (records sellados), sin dependencias de framework.
- **e2e-tests** — módulo de test que levanta ambos servicios juntos y verifica la saga de extremo a
  extremo.

### Topics de Kafka

| Topic | Produce | Consume | Clave |
|---|---|---|---|
| `account-commands` | transfer-service | account-service | `accountId` |
| `account-events` | account-service | transfer-service | `accountId` |
| `transfer-events` | transfer-service | (ledger, futuro) | `transferId` |
| `*.DLT` | error handler | inspección manual | — |

## Requisitos

- **Docker** en ejecución (la infraestructura y los tests de integración usan contenedores).
- **JDK 21**. El build usa el Maven Wrapper (`./mvnw`), no hace falta Maven global.

> En Windows, exporta `JAVA_HOME` apuntando al JDK 21 si tu `PATH` tiene otra versión:
> `JAVA_HOME="C:\Program Files\Java\jdk-21.x" ./mvnw ...`

## Arrancar

```bash
# 1. Infraestructura (Kafka KRaft + Kafka UI + un Postgres por servicio)
docker compose up -d
docker compose ps                      # todos healthy

# 2. Build + tests de todo el monorepo (unit + integración con Testcontainers)
./mvnw verify

# 3. Arrancar los servicios (en dos terminales)
./mvnw -pl account-service  spring-boot:run    # http://localhost:8081
./mvnw -pl transfer-service spring-boot:run    # http://localhost:8082
```

### Probar una transferencia

```bash
# Crear dos cuentas
curl -X POST localhost:8081/accounts -H 'Content-Type: application/json' \
     -d '{"holder":"Ana","initialBalance":100.00}'
curl -X POST localhost:8081/accounts -H 'Content-Type: application/json' \
     -d '{"holder":"Luis","initialBalance":0.00}'

# Lanzar la transferencia (usa los ids devueltos arriba)
curl -X POST localhost:8082/transfers -H 'Content-Type: application/json' \
     -d '{"sourceAccountId":"<id-ana>","destinationAccountId":"<id-luis>","amount":40.00}'

# Ver el estado de la saga y los saldos resultantes
curl localhost:8082/transfers/<id-transfer>     # status: COMPLETED
curl localhost:8081/accounts/<id-ana>           # balance: 60.00
curl localhost:8081/accounts/<id-luis>          # balance: 40.00
```

Una transferencia por importe mayor que el saldo termina en `FAILED` con los saldos intactos.

### Inspeccionar y limpiar

- **Kafka UI**: http://localhost:8080 (topics, particiones, consumer-groups y `*.DLT`).
- Parar y borrar volúmenes: `docker compose down -v`.

## Puertos

| Servicio | Puerto |
|---|---|
| account-service | 8081 |
| transfer-service | 8082 |
| Kafka (host) | 29092 |
| Kafka UI | 8080 |
| postgres-account | 5442 (db `accountdb`) |
| postgres-transfer | 5443 (db `transferdb`) |

> Los Postgres usan el bloque de puertos host **544x** para no chocar con un PostgreSQL nativo en 5432.
> Credenciales locales: `hexabank` / `hexabank`.

## Estructura

```
hexabank/
  pom.xml             parent POM (Java 21, BOM Spring Boot 3.5)
  shared-events/      contratos de eventos de la saga (records sellados, Java puro)
  account-service/    cuentas y saldos (hexagonal: domain / application / infrastructure)
  transfer-service/   orquestador de la saga (máquina de estados + compensación)
  e2e-tests/          test de la saga de extremo a extremo (ambos servicios)
  docker-compose.yml  Kafka KRaft + Kafka UI + postgres-account + postgres-transfer
```

Cada servicio sigue la misma estructura hexagonal:

```
<service>/src/main/java/com/hexabank/<service>/
  domain/            entidades y reglas de negocio (sin framework)
  application/
    port/in          puertos de entrada (casos de uso)
    port/out         puertos de salida (repositorios, publicador de eventos)
    service          implementación de los casos de uso
  infrastructure/
    rest             adaptadores REST
    messaging        adaptadores Kafka (listener + publicador)
    persistence      adaptadores JPA
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

## Roadmap

- **ledger-service** — libro mayor append-only + proyección de lectura, con actualizaciones en vivo (SSE).
- **Frontend** (React) para operar cuentas y transferencias y ver el ledger en tiempo real.
- **Observabilidad** (Actuator + Prometheus + Grafana) e **integración continua**.
