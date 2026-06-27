package com.hexabank.ledger.application.port.in;

import com.hexabank.ledger.domain.LedgerEntry;

import java.util.List;
import java.util.UUID;

/**
 * Puerto de entrada de consulta del libro mayor.
 */
public interface QueryLedgerUseCase {

    /** Asientos más recientes del libro (todas las cuentas). */
    List<LedgerEntry> recentEntries();

    /** Asientos más recientes que afectan a una cuenta concreta. */
    List<LedgerEntry> entriesForAccount(UUID accountId);
}
