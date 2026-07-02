-- Propietario de la cuenta = claim 'sub' del JWT del creador.
-- Nullable: las filas anteriores a esta version no tienen dueno (solo STAFF puede operarlas).
ALTER TABLE accounts ADD COLUMN owner_id VARCHAR(64);

CREATE INDEX idx_accounts_owner ON accounts (owner_id);
