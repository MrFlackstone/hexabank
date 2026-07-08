-- Outbox transaccional: cada fila es un evento de dominio pendiente de publicar. Se inserta en la
-- MISMA transacción que el cambio de saldo (cierra el dual-write evento+BD); un relay lo publica a
-- Kafka tras el commit y marca published_at. event_id es único para que el relay sea idempotente.
CREATE TABLE outbox (
    id            UUID         PRIMARY KEY,
    event_id      UUID         NOT NULL UNIQUE,
    topic         VARCHAR(100) NOT NULL,
    partition_key VARCHAR(100),
    event_type    VARCHAR(255) NOT NULL,   -- FQN del record de evento (para deserializar en el relay)
    payload       TEXT         NOT NULL,   -- evento serializado a JSON
    created_at    TIMESTAMPTZ  NOT NULL DEFAULT now(),
    published_at  TIMESTAMPTZ              -- NULL = pendiente de publicar
);

-- Índice parcial: el relay solo consulta las filas no publicadas, en orden de creación.
CREATE INDEX idx_outbox_unpublished ON outbox (created_at) WHERE published_at IS NULL;
