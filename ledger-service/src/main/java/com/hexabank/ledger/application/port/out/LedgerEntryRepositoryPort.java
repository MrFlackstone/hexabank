package com.hexabank.ledger.application.port.out;

import com.hexabank.ledger.domain.LedgerEntry;

import java.util.List;
import java.util.UUID;

/**
 * Puerto de salida de persistencia del libro mayor append-only.
 */
public interface LedgerEntryRepositoryPort {

    void append(LedgerEntry entry);

    List<LedgerEntry> findRecent(int limit);

    List<LedgerEntry> findByAccount(UUID accountId, int limit);
}
