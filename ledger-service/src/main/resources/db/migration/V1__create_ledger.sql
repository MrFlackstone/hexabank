-- Libro mayor append-only: una fila inmutable por cada movimiento de dinero confirmado.
-- 'id' = eventId del movimiento que la origino (ancla la idempotencia de forma natural).
CREATE TABLE ledger_entries (
    id           UUID           PRIMARY KEY,
    transfer_id  UUID           NOT NULL,
    account_id   UUID           NOT NULL,
    entry_type   VARCHAR(10)    NOT NULL,   -- DEBIT | CREDIT | REFUND
    amount       NUMERIC(19, 2) NOT NULL,
    created_at   TIMESTAMPTZ    NOT NULL
);
CREATE INDEX idx_ledger_account ON ledger_entries (account_id);
CREATE INDEX idx_ledger_transfer ON ledger_entries (transfer_id);

-- Proyeccion de lectura por transferencia (modelo de consulta materializado).
-- Se rellena de forma incremental a partir de los eventos de la saga.
CREATE TABLE transfer_projection (
    transfer_id            UUID           PRIMARY KEY,
    source_account_id      UUID,
    destination_account_id UUID,
    amount                 NUMERIC(19, 2),
    status                 VARCHAR(20)    NOT NULL,   -- PENDING | COMPLETED | FAILED
    updated_at             TIMESTAMPTZ    NOT NULL
);
