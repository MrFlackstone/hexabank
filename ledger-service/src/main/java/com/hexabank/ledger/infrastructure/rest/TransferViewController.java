package com.hexabank.ledger.infrastructure.rest;

import com.hexabank.ledger.application.port.in.GetTransferViewUseCase;
import com.hexabank.ledger.infrastructure.rest.dto.TransferViewResponse;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

/**
 * Adaptador de entrada REST que expone la proyección de lectura de una transferencia.
 */
@RestController
@RequestMapping("/transfers")
public class TransferViewController {

    private final GetTransferViewUseCase getTransferView;

    public TransferViewController(GetTransferViewUseCase getTransferView) {
        this.getTransferView = getTransferView;
    }

    @GetMapping("/{id}")
    public TransferViewResponse getById(@PathVariable("id") UUID id) {
        return TransferViewResponse.from(getTransferView.getTransfer(id));
    }
}
