package com.hexabank.ledger.infrastructure.rest;

import com.hexabank.ledger.application.port.in.QueryLedgerUseCase;
import com.hexabank.ledger.infrastructure.rest.dto.LedgerEntryResponse;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

/**
 * Adaptador de entrada REST de consulta del libro mayor. Solo traduce HTTP &harr; casos de uso de
 * lectura; sin reglas de negocio.
 *
 * <p>El handler se divide por la presencia del parámetro {@code accountId}: el libro completo es una
 * puerta pura de rol (STAFF, method security → 403; en una colección sin id no hay existencia que
 * ocultar) y la consulta por cuenta se autoriza por propiedad en la aplicación (404 si ajena).</p>
 */
@RestController
@RequestMapping("/v1/ledger")
public class LedgerController {

    private final QueryLedgerUseCase queryLedger;

    public LedgerController(QueryLedgerUseCase queryLedger) {
        this.queryLedger = queryLedger;
    }

    /** Libro completo (todas las cuentas): solo personal del banco. */
    @GetMapping(params = "!accountId")
    @PreAuthorize("hasRole('STAFF')")
    public List<LedgerEntryResponse> listAll() {
        return queryLedger.recentEntries().stream().map(LedgerEntryResponse::from).toList();
    }

    /** Asientos de una cuenta concreta: su propietario o STAFF. */
    @GetMapping(params = "accountId")
    public List<LedgerEntryResponse> listByAccount(@RequestParam("accountId") UUID accountId,
                                                   Authentication authentication) {
        return queryLedger.entriesForAccount(accountId, CallerContexts.from(authentication)).stream()
                .map(LedgerEntryResponse::from)
                .toList();
    }
}
