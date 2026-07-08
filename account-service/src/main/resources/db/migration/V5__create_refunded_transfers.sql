-- Idempotencia del reembolso por transferId: garantiza que una transferencia se reembolsa una sola
-- vez aunque llegue más de un RefundRequested (p. ej. la reemisión del reaper de sagas atascadas).
-- Es distinto de processed_events (idempotencia por eventId): aquí la clave es la transferencia.
CREATE TABLE refunded_transfers (
    transfer_id UUID        PRIMARY KEY,
    refunded_at TIMESTAMPTZ NOT NULL DEFAULT now()
);
