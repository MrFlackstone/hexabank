package com.hexabank.account.infrastructure.persistence;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.Version;
import org.hibernate.annotations.CreationTimestamp;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.UUID;

/**
 * Entidad JPA: representación de una cuenta en la tabla {@code accounts}.
 *
 * <p>Vive en la capa de infraestructura, separada del agregado de dominio {@link com.hexabank.account.domain.Account}.
 * El adaptador de persistencia mapea entre ambos. Esta separación evita contaminar el dominio con
 * anotaciones de JPA y permite que el modelo de BD y el de negocio evolucionen por separado.</p>
 *
 * <p>El campo {@link #version} lleva {@code @Version}: Hibernate lo incrementa en cada update y compara
 * el valor en el {@code WHERE}. Si otra transacción modificó la fila entretanto, el update afecta a 0
 * filas y se lanza una excepción de bloqueo optimista — el mecanismo central de concurrencia.</p>
 */
@Entity
@Table(name = "accounts")
public class AccountJpaEntity {

    @Id
    private UUID id;

    @Column(nullable = false)
    private String holder;

    @Column(nullable = false, precision = 19, scale = 2)
    private BigDecimal balance;

    @Version
    @Column(nullable = false)
    private long version;

    /** Lo rellena Hibernate en el INSERT; no se toca en updates. */
    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private OffsetDateTime createdAt;

    /** Constructor requerido por JPA. */
    protected AccountJpaEntity() {
    }

    public AccountJpaEntity(UUID id, String holder, BigDecimal balance, long version) {
        this.id = id;
        this.holder = holder;
        this.balance = balance;
        this.version = version;
    }

    public UUID getId() {
        return id;
    }

    public String getHolder() {
        return holder;
    }

    public BigDecimal getBalance() {
        return balance;
    }

    public long getVersion() {
        return version;
    }

    public OffsetDateTime getCreatedAt() {
        return createdAt;
    }
}
