-- Registro de idempotencia del consumidor Kafka: cada fila es un eventId ya procesado.
CREATE TABLE processed_events (
    event_id     UUID        PRIMARY KEY,
    processed_at TIMESTAMPTZ NOT NULL DEFAULT now()
);
