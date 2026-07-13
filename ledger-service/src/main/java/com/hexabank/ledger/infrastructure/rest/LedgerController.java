package com.hexabank.ledger.infrastructure.rest;

import com.hexabank.ledger.application.port.in.QueryLedgerUseCase;
import com.hexabank.ledger.infrastructure.rest.dto.LedgerEntryResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
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
@Tag(name = "Libro mayor", description = "Consulta de asientos contables (libro completo STAFF y por cuenta).")
public class LedgerController {

    private final QueryLedgerUseCase queryLedger;

    public LedgerController(QueryLedgerUseCase queryLedger) {
        this.queryLedger = queryLedger;
    }

    /** Libro completo (todas las cuentas): solo personal del banco. */
    @Operation(summary = "Libro completo (solo STAFF)",
            description = "Devuelve los asientos recientes de todas las cuentas. Puerta pura de rol: 403 si no es STAFF.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Asientos recientes"),
            @ApiResponse(responseCode = "403", description = "El llamante no es STAFF")
    })
    @GetMapping(params = "!accountId")
    @PreAuthorize("hasRole('STAFF')")
    public List<LedgerEntryResponse> listAll() {
        return queryLedger.recentEntries().stream().map(LedgerEntryResponse::from).toList();
    }

    /** Asientos de una cuenta concreta: su propietario o STAFF. */
    @Operation(summary = "Asientos de una cuenta",
            description = "Devuelve los asientos de la cuenta indicada. Se autoriza por propiedad: 404 si es ajena.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Asientos de la cuenta"),
            @ApiResponse(responseCode = "404", description = "Cuenta ajena o inexistente (identico)")
    })
    @GetMapping(params = "accountId")
    public List<LedgerEntryResponse> listByAccount(@RequestParam("accountId") UUID accountId,
                                                   Authentication authentication) {
        return queryLedger.entriesForAccount(accountId, CallerContexts.from(authentication)).stream()
                .map(LedgerEntryResponse::from)
                .toList();
    }
}
