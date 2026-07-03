package com.hexabank.ledger.application.port.in;

import com.hexabank.ledger.domain.LedgerEntry;

import java.util.List;
import java.util.UUID;

/**
 * Puerto de entrada de consulta del libro mayor.
 *
 * <p>El libro completo es de uso interno del banco (STAFF, gate de rol en el adaptador REST); la
 * consulta por cuenta está autorizada por propiedad y la denegación responde como "cuenta
 * inexistente" (404) para no revelar existencia.</p>
 */
public interface QueryLedgerUseCase {

    /** Asientos más recientes del libro (todas las cuentas). Solo STAFF. */
    List<LedgerEntry> recentEntries();

    /** Asientos más recientes que afectan a una cuenta concreta del llamante (o cualquiera si STAFF). */
    List<LedgerEntry> entriesForAccount(UUID accountId, CallerContext caller);
}
