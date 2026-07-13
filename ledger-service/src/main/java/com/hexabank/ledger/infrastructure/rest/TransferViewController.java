package com.hexabank.ledger.infrastructure.rest;

import com.hexabank.ledger.application.port.in.GetTransferViewUseCase;
import com.hexabank.ledger.infrastructure.rest.dto.TransferViewResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

/**
 * Adaptador de entrada REST que expone la proyección de lectura de una transferencia.
 */
@RestController
@RequestMapping("/v1/transfers")
@Tag(name = "Transferencias (proyeccion CQRS)",
        description = "Vista de lectura de una transferencia servida por el ledger (via nginx /api/projections).")
public class TransferViewController {

    private final GetTransferViewUseCase getTransferView;

    public TransferViewController(GetTransferViewUseCase getTransferView) {
        this.getTransferView = getTransferView;
    }

    @Operation(summary = "Vista de lectura de una transferencia (CQRS)",
            description = "Proyeccion de lectura del ledger. Visible para las partes involucradas y STAFF; 404 para terceros.")
    @GetMapping("/{id}")
    public TransferViewResponse getById(@PathVariable("id") UUID id, Authentication authentication) {
        return TransferViewResponse.from(getTransferView.getTransfer(id, CallerContexts.from(authentication)));
    }
}
