-- Tabla de cuentas. Fuente de verdad del esquema (Flyway).
CREATE TABLE accounts (
    id         UUID           PRIMARY KEY,
    holder     VARCHAR(120)   NOT NULL,
    balance    NUMERIC(19, 2) NOT NULL,
    -- Contador de bloqueo optimista (@Version).
    version    BIGINT         NOT NULL,
    created_at TIMESTAMPTZ    NOT NULL
);
