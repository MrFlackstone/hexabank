package com.hexabank.account.infrastructure.persistence;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

/**
 * Repositorio Spring Data JPA sobre {@link AccountJpaEntity}.
 *
 * <p>Detalle de infraestructura: NO es el puerto de salida del dominio. El adaptador
 * {@link AccountPersistenceAdapter} lo usa internamente y expone los puertos
 * {@code LoadAccountPort}/{@code SaveAccountPort} a la aplicación.</p>
 */
public interface AccountJpaRepository extends JpaRepository<AccountJpaEntity, UUID> {
}
