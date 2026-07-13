package com.hexabank.transfer.infrastructure.rest;

import com.hexabank.transfer.application.port.in.GetTransferUseCase;
import com.hexabank.transfer.application.port.in.PageResult;
import com.hexabank.transfer.application.port.in.RequestTransferCommand;
import com.hexabank.transfer.application.port.in.RequestTransferUseCase;
import com.hexabank.transfer.application.port.in.SearchTransfersUseCase;
import com.hexabank.transfer.application.port.in.TransferSearchQuery;
import com.hexabank.transfer.application.port.in.TransferSort;
import com.hexabank.transfer.domain.Transfer;
import com.hexabank.transfer.domain.TransferId;
import com.hexabank.transfer.domain.TransferStatus;
import com.hexabank.transfer.infrastructure.rest.dto.CreateTransferRequest;
import com.hexabank.transfer.infrastructure.rest.dto.PageResponse;
import com.hexabank.transfer.infrastructure.rest.dto.TransferResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.time.OffsetDateTime;
import java.util.Map;

/**
 * Adaptador de entrada REST: expone los casos de uso de transferencias por HTTP.
 *
 * <p>Depende de los puertos de entrada, nunca de la implementación. Solo traduce HTTP &harr;
 * comandos/respuestas de la aplicación; no contiene reglas de negocio.</p>
 */
@RestController
@RequestMapping("/v1/transfers")
@Tag(name = "Transferencias", description = "Orquestacion de la saga de transferencias y su consulta paginada.")
public class TransferController {

    /** Tope de tamaño de página: protege al servicio de peticiones desmesuradas. */
    private static final int MAX_PAGE_SIZE = 100;

    /** Whitelist de ordenación: nombre de parámetro REST → campo permitido. */
    private static final Map<String, TransferSort> SORT_WHITELIST = Map.of(
            "createdAt", TransferSort.CREATED_AT,
            "amount", TransferSort.AMOUNT,
            "status", TransferSort.STATUS);

    private final RequestTransferUseCase requestTransferUseCase;
    private final GetTransferUseCase getTransferUseCase;
    private final SearchTransfersUseCase searchTransfersUseCase;

    public TransferController(RequestTransferUseCase requestTransferUseCase,
                              GetTransferUseCase getTransferUseCase,
                              SearchTransfersUseCase searchTransfersUseCase) {
        this.requestTransferUseCase = requestTransferUseCase;
        this.getTransferUseCase = getTransferUseCase;
        this.searchTransfersUseCase = searchTransfersUseCase;
    }

    @Operation(summary = "Solicita una transferencia",
            description = "Arranca la saga (queda en PENDING y publica DebitRequested). La cabecera "
                    + "opcional Idempotency-Key hace idempotente el reintento del cliente.")
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Transferencia aceptada (PENDING)"),
            @ApiResponse(responseCode = "400", description = "Datos invalidos (RFC 7807 ProblemDetail)"),
            @ApiResponse(responseCode = "422", description = "Idempotency-Key reutilizada con otro payload")
    })
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public TransferResponse create(@Valid @RequestBody CreateTransferRequest request,
                                   @RequestHeader(name = "Idempotency-Key", required = false) String idempotencyKey,
                                   Authentication authentication) {
        Transfer transfer = requestTransferUseCase.requestTransfer(new RequestTransferCommand(
                request.sourceAccountId(), request.destinationAccountId(), request.amount()),
                CallerContexts.from(authentication), idempotencyKey);
        return TransferResponse.from(transfer);
    }

    @Operation(summary = "Obtiene una transferencia por id",
            description = "Visible para las cuentas origen/destino y STAFF; en caso contrario 404 (anti existence-oracle).")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Transferencia encontrada"),
            @ApiResponse(responseCode = "404", description = "No existe o no es visible para el llamante")
    })
    @GetMapping("/{id}")
    public ResponseEntity<TransferResponse> getById(@PathVariable("id") String id,
                                                    Authentication authentication) {
        Transfer transfer = getTransferUseCase.getTransfer(TransferId.of(id), CallerContexts.from(authentication));
        return ResponseEntity.ok(TransferResponse.from(transfer));
    }

    /**
     * Listado paginado 100 % server-side: el servidor filtra, ordena y pagina en SQL y devuelve solo
     * la página pedida. Un USER ve únicamente transferencias que involucran sus cuentas; STAFF, todas.
     */
    @Operation(summary = "Lista transferencias paginadas",
            description = "Paginacion 100% server-side (filtra/ordena/pagina en SQL). Un USER solo ve las "
                    + "transferencias de sus cuentas; STAFF, todas. sort admite createdAt|amount|status con asc|desc; "
                    + "size tope 100.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Pagina de resultados"),
            @ApiResponse(responseCode = "400", description = "sort fuera de whitelist o size por encima del tope")
    })
    @GetMapping
    public PageResponse<TransferResponse> search(
            @RequestParam(name = "status", required = false) String status,
            @RequestParam(name = "from", required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) OffsetDateTime from,
            @RequestParam(name = "to", required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) OffsetDateTime to,
            @RequestParam(name = "page", defaultValue = "0") int page,
            @RequestParam(name = "size", defaultValue = "20") int size,
            @RequestParam(name = "sort", defaultValue = "createdAt,desc") String sort,
            Authentication authentication) {
        if (size > MAX_PAGE_SIZE) {
            throw new IllegalArgumentException("El tamaño de página máximo es " + MAX_PAGE_SIZE);
        }
        TransferSearchQuery query = new TransferSearchQuery(parseStatus(status), from, to,
                page, size, parseSortField(sort), parseSortAscending(sort));
        PageResult<Transfer> result = searchTransfersUseCase.searchTransfers(
                query, CallerContexts.from(authentication));
        return PageResponse.from(result, TransferResponse::from);
    }

    private TransferStatus parseStatus(String status) {
        if (status == null || status.isBlank()) {
            return null;
        }
        try {
            return TransferStatus.valueOf(status.toUpperCase());
        } catch (IllegalArgumentException ex) {
            throw new IllegalArgumentException("Estado de transferencia desconocido: " + status);
        }
    }

    private TransferSort parseSortField(String sort) {
        String field = sort.split(",")[0].trim();
        TransferSort transferSort = SORT_WHITELIST.get(field);
        if (transferSort == null) {
            throw new IllegalArgumentException(
                    "Orden no permitido: " + field + " (permitidos: " + SORT_WHITELIST.keySet() + ")");
        }
        return transferSort;
    }

    private boolean parseSortAscending(String sort) {
        String[] parts = sort.split(",");
        if (parts.length < 2) {
            return false;
        }
        return switch (parts[1].trim().toLowerCase()) {
            case "asc" -> true;
            case "desc" -> false;
            default -> throw new IllegalArgumentException("Dirección de orden no válida: " + parts[1]);
        };
    }
}
