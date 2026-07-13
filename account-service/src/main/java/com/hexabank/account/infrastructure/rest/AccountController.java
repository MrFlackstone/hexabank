package com.hexabank.account.infrastructure.rest;

import com.hexabank.account.application.port.in.CreateAccountCommand;
import com.hexabank.account.application.port.in.CreateAccountUseCase;
import com.hexabank.account.application.port.in.GetAccountUseCase;
import com.hexabank.account.domain.Account;
import com.hexabank.account.domain.AccountId;
import com.hexabank.account.domain.Money;
import com.hexabank.account.domain.OwnerId;
import com.hexabank.account.infrastructure.rest.dto.AccountResponse;
import com.hexabank.account.infrastructure.rest.dto.CreateAccountRequest;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

/**
 * Adaptador de entrada REST: expone los casos de uso de cuentas por HTTP.
 *
 * <p>Depende de los puertos de entrada ({@link CreateAccountUseCase}, {@link GetAccountUseCase}),
 * nunca de la implementación. Su única responsabilidad es traducir HTTP &harr; comandos/respuestas de
 * la aplicación; no contiene reglas de negocio.</p>
 */
@RestController
@RequestMapping("/v1/accounts")
@Tag(name = "Cuentas", description = "Alta y consulta de cuentas y saldos (fuente de verdad).")
public class AccountController {

    private final CreateAccountUseCase createAccountUseCase;
    private final GetAccountUseCase getAccountUseCase;

    public AccountController(CreateAccountUseCase createAccountUseCase, GetAccountUseCase getAccountUseCase) {
        this.createAccountUseCase = createAccountUseCase;
        this.getAccountUseCase = getAccountUseCase;
    }

    @Operation(summary = "Crea una cuenta",
            description = "Registra una cuenta con su saldo inicial. El titular queda asociado al usuario autenticado.")
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Cuenta creada"),
            @ApiResponse(responseCode = "400", description = "Datos invalidos (RFC 7807 ProblemDetail)"),
            @ApiResponse(responseCode = "401", description = "Falta el Bearer o es invalido")
    })
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public AccountResponse create(@Valid @RequestBody CreateAccountRequest request,
                                  Authentication authentication) {
        Account account = createAccountUseCase.createAccount(new CreateAccountCommand(
                request.holder(), Money.of(request.initialBalance()), OwnerId.of(authentication.getName())));
        return AccountResponse.from(account);
    }

    @Operation(summary = "Obtiene una cuenta por id",
            description = "Devuelve la cuenta si el llamante es su titular o STAFF; en caso contrario 404 (anti existence-oracle).")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Cuenta encontrada"),
            @ApiResponse(responseCode = "404", description = "No existe o no es visible para el llamante")
    })
    @GetMapping("/{id}")
    public ResponseEntity<AccountResponse> getById(@PathVariable("id") String id,
                                                   Authentication authentication) {
        Account account = getAccountUseCase.getAccount(AccountId.of(id), CallerContexts.from(authentication));
        return ResponseEntity.ok(AccountResponse.from(account));
    }
}
