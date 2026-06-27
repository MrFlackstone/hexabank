package com.hexabank.ledger.infrastructure.rest;

import com.hexabank.ledger.application.port.in.QueryLedgerUseCase;
import com.hexabank.ledger.infrastructure.rest.dto.LedgerEntryResponse;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

/**
 * Adaptador de entrada REST de consulta del libro mayor. Solo traduce HTTP &harr; casos de uso de
 * lectura; sin reglas de negocio.
 */
@RestController
@RequestMapping("/ledger")
public class LedgerController {

    private final QueryLedgerUseCase queryLedger;

    public LedgerController(QueryLedgerUseCase queryLedger) {
        this.queryLedger = queryLedger;
    }

    /** Asientos recientes; con {@code ?accountId=} se filtran por cuenta. */
    @GetMapping
    public List<LedgerEntryResponse> list(@RequestParam(value = "accountId", required = false) UUID accountId) {
        List<com.hexabank.ledger.domain.LedgerEntry> entries = (accountId == null)
                ? queryLedger.recentEntries()
                : queryLedger.entriesForAccount(accountId);
        return entries.stream().map(LedgerEntryResponse::from).toList();
    }
}
