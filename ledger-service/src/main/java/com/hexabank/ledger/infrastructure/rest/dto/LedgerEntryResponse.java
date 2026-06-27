package com.hexabank.ledger.infrastructure.rest.dto;

import com.hexabank.ledger.domain.LedgerEntry;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.UUID;

/**
 * Representación de transporte de un asiento del libro mayor.
 */
public record LedgerEntryResponse(
        UUID entryId,
        UUID transferId,
        UUID accountId,
        String type,
        BigDecimal amount,
        OffsetDateTime createdAt) {

    public static LedgerEntryResponse from(LedgerEntry entry) {
        return new LedgerEntryResponse(
                entry.entryId(),
                entry.transferId(),
                entry.accountId(),
                entry.type().name(),
                entry.amount(),
                entry.createdAt());
    }
}
