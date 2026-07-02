-- Proyeccion CQRS local: propietario de cada cuenta, materializada desde AccountOpened
-- (topic account-events). Permite autorizar transferencias sin consultas sincronas a account-service.
-- PK = account_id: reprocesar el evento no duplica filas (upsert natural de JPA).
CREATE TABLE account_ownership (
    account_id UUID        PRIMARY KEY,
    owner_id   VARCHAR(64) NOT NULL,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE INDEX idx_account_ownership_owner ON account_ownership (owner_id);
