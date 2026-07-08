-- Idempotency-Key de cliente sobre POST /transfers: mapea una clave de idempotencia (cabecera
-- HTTP) al transfer creado. Un reintento con la misma clave devuelve el mismo transfer en lugar de
-- crear uno nuevo. request_hash detecta el reúso de la clave con un payload distinto (→ 422).
-- Es distinto de processed_events (idempotencia interna del consumidor Kafka por eventId).
CREATE TABLE idempotency_keys (
    idempotency_key VARCHAR(255) PRIMARY KEY,
    request_hash    VARCHAR(64)  NOT NULL,   -- SHA-256 (hex) del payload de la petición
    transfer_id     UUID         NOT NULL,
    created_at      TIMESTAMPTZ  NOT NULL DEFAULT now()
);
