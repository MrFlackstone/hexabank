package com.hexabank.ledger.domain;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.UUID;

/**
 * Asiento inmutable del libro mayor append-only. Una vez registrado no se modifica ni se borra: el
 * historial es la fuente de verdad de la lectura.
 *
 * <p>{@code entryId} reutiliza el {@code eventId} del movimiento que lo originó, de modo que reprocesar
 * el mismo evento no puede crear un asiento duplicado (idempotencia anclada en la clave primaria).</p>
 */
public record LedgerEntry(
        UUID entryId,
        UUID transferId,
        UUID accountId,
        EntryType type,
        BigDecimal amount,
        OffsetDateTime createdAt) {

    public LedgerEntry {
        if (entryId == null || transferId == null || accountId == null || type == null) {
            throw new IllegalArgumentException("LedgerEntry requiere entryId, transferId, accountId y type");
        }
        if (amount == null || amount.signum() <= 0) {
            throw new IllegalArgumentException("El importe del asiento debe ser positivo");
        }
    }

    /** Crea un asiento con la marca de tiempo de registro en el libro. */
    public static LedgerEntry of(UUID eventId, UUID transferId, UUID accountId, EntryType type, BigDecimal amount) {
        return new LedgerEntry(eventId, transferId, accountId, type, amount, OffsetDateTime.now());
    }
}
