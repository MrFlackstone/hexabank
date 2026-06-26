package com.hexabank.transfer.infrastructure.rest;

import com.hexabank.transfer.application.port.in.GetTransferUseCase;
import com.hexabank.transfer.application.port.in.RequestTransferCommand;
import com.hexabank.transfer.application.port.in.RequestTransferUseCase;
import com.hexabank.transfer.domain.Transfer;
import com.hexabank.transfer.domain.TransferId;
import com.hexabank.transfer.infrastructure.rest.dto.CreateTransferRequest;
import com.hexabank.transfer.infrastructure.rest.dto.TransferResponse;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

/**
 * Adaptador de entrada REST: expone los casos de uso de transferencias por HTTP.
 *
 * <p>Depende de los puertos de entrada, nunca de la implementación. Solo traduce HTTP &harr;
 * comandos/respuestas de la aplicación; no contiene reglas de negocio.</p>
 */
@RestController
@RequestMapping("/transfers")
public class TransferController {

    private final RequestTransferUseCase requestTransferUseCase;
    private final GetTransferUseCase getTransferUseCase;

    public TransferController(RequestTransferUseCase requestTransferUseCase, GetTransferUseCase getTransferUseCase) {
        this.requestTransferUseCase = requestTransferUseCase;
        this.getTransferUseCase = getTransferUseCase;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public TransferResponse create(@Valid @RequestBody CreateTransferRequest request) {
        Transfer transfer = requestTransferUseCase.requestTransfer(new RequestTransferCommand(
                request.sourceAccountId(), request.destinationAccountId(), request.amount()));
        return TransferResponse.from(transfer);
    }

    @GetMapping("/{id}")
    public ResponseEntity<TransferResponse> getById(@PathVariable("id") String id) {
        Transfer transfer = getTransferUseCase.getTransfer(TransferId.of(id));
        return ResponseEntity.ok(TransferResponse.from(transfer));
    }
}
