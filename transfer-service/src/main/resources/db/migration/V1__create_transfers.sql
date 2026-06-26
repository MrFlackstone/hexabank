-- Tabla de transferencias. Fuente de verdad del esquema (Flyway).
CREATE TABLE transfers (
    id                     UUID           PRIMARY KEY,
    source_account_id      UUID           NOT NULL,
    destination_account_id UUID           NOT NULL,
    amount                 NUMERIC(19, 2) NOT NULL,
    status                 VARCHAR(20)    NOT NULL,
    -- Contador de bloqueo optimista (@Version).
    version                BIGINT         NOT NULL,
    created_at             TIMESTAMPTZ    NOT NULL
);
