package com.hexabank.account.infrastructure.persistence;

import com.hexabank.account.application.port.out.LoadAccountPort;
import com.hexabank.account.application.port.out.SaveAccountPort;
import com.hexabank.account.domain.Account;
import com.hexabank.account.domain.AccountId;
import com.hexabank.account.domain.Money;
import org.springframework.stereotype.Component;

import java.util.Optional;

/**
 * Adaptador de salida que implementa los puertos de persistencia del dominio sobre Spring Data JPA.
 *
 * <p>Traduce en ambos sentidos entre el agregado de dominio {@link Account} y la entidad JPA
 * {@link AccountJpaEntity}. El dominio nunca ve JPA; este adaptador es el único punto de contacto.</p>
 */
@Component
public class AccountPersistenceAdapter implements LoadAccountPort, SaveAccountPort {

    private final AccountJpaRepository repository;

    public AccountPersistenceAdapter(AccountJpaRepository repository) {
        this.repository = repository;
    }

    @Override
    public Optional<Account> findById(AccountId id) {
        return repository.findById(id.value()).map(this::toDomain);
    }

    @Override
    public Account save(Account account) {
        AccountJpaEntity saved = repository.save(toEntity(account));
        return toDomain(saved);
    }

    private Account toDomain(AccountJpaEntity entity) {
        return new Account(
                new AccountId(entity.getId()),
                entity.getHolder(),
                Money.of(entity.getBalance()),
                entity.getVersion());
    }

    private AccountJpaEntity toEntity(Account account) {
        return new AccountJpaEntity(
                account.id().value(),
                account.holder(),
                account.balance().amount(),
                account.version());
    }
}
