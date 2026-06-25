package com.hexabank.account.infrastructure.rest;

import com.hexabank.account.application.port.in.CreateAccountCommand;
import com.hexabank.account.application.port.in.CreateAccountUseCase;
import com.hexabank.account.application.port.in.GetAccountUseCase;
import com.hexabank.account.domain.Account;
import com.hexabank.account.domain.AccountId;
import com.hexabank.account.domain.Money;
import com.hexabank.account.infrastructure.rest.dto.AccountResponse;
import com.hexabank.account.infrastructure.rest.dto.CreateAccountRequest;
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
 * Adaptador de entrada REST: expone los casos de uso de cuentas por HTTP.
 *
 * <p>Depende de los puertos de entrada ({@link CreateAccountUseCase}, {@link GetAccountUseCase}),
 * nunca de la implementación. Su única responsabilidad es traducir HTTP &harr; comandos/respuestas de
 * la aplicación; no contiene reglas de negocio.</p>
 */
@RestController
@RequestMapping("/accounts")
public class AccountController {

    private final CreateAccountUseCase createAccountUseCase;
    private final GetAccountUseCase getAccountUseCase;

    public AccountController(CreateAccountUseCase createAccountUseCase, GetAccountUseCase getAccountUseCase) {
        this.createAccountUseCase = createAccountUseCase;
        this.getAccountUseCase = getAccountUseCase;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public AccountResponse create(@Valid @RequestBody CreateAccountRequest request) {
        Account account = createAccountUseCase.createAccount(
                new CreateAccountCommand(request.holder(), Money.of(request.initialBalance())));
        return AccountResponse.from(account);
    }

    @GetMapping("/{id}")
    public ResponseEntity<AccountResponse> getById(@PathVariable("id") String id) {
        Account account = getAccountUseCase.getAccount(AccountId.of(id));
        return ResponseEntity.ok(AccountResponse.from(account));
    }
}
