package com.hexabank.transfer.infrastructure.persistence;

import com.hexabank.transfer.domain.TransferStatus;
import org.springframework.data.jpa.domain.Specification;

import java.time.OffsetDateTime;
import java.util.Set;
import java.util.UUID;

/**
 * Specifications de JPA para el listado de transferencias: cada filtro es un predicado componible
 * que Spring Data traduce a SQL. Los filtros nulos devuelven una specification neutra (sin cláusula).
 */
final class TransferSpecifications {

    private TransferSpecifications() {
    }

    static Specification<TransferJpaEntity> statusIs(TransferStatus status) {
        return (root, query, cb) ->
                status == null ? cb.conjunction() : cb.equal(root.get("status"), status.name());
    }

    static Specification<TransferJpaEntity> createdAtOrAfter(OffsetDateTime from) {
        return (root, query, cb) ->
                from == null ? cb.conjunction() : cb.greaterThanOrEqualTo(root.get("createdAt"), from);
    }

    static Specification<TransferJpaEntity> createdAtOrBefore(OffsetDateTime to) {
        return (root, query, cb) ->
                to == null ? cb.conjunction() : cb.lessThanOrEqualTo(root.get("createdAt"), to);
    }

    /** Predicado de visibilidad: la transferencia involucra (origen o destino) una cuenta del conjunto. */
    static Specification<TransferJpaEntity> involvesAccounts(Set<UUID> accountIds) {
        return (root, query, cb) -> accountIds == null
                ? cb.conjunction()
                : cb.or(root.get("sourceAccountId").in(accountIds),
                        root.get("destinationAccountId").in(accountIds));
    }
}
