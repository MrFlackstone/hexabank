package com.hexabank.transfer.domain;

import com.hexabank.transfer.domain.exception.InvalidTransferStateException;

import java.math.BigDecimal;
import java.util.Objects;
import java.util.UUID;

/**
 * Agregado raíz del dominio: una transferencia entre dos cuentas y su máquina de estados.
 *
 * <p>Concentra las transiciones válidas de la saga y no depende de ningún framework. Cada método de
 * transición valida el estado de origen y lanza {@link InvalidTransferStateException} si no es legal.</p>
 *
 * <p>El campo {@code version} es el contador de bloqueo optimista; el adaptador JPA lo marca con
 * {@code @Version}.</p>
 */
public class Transfer {

    private final TransferId id;
    private final UUID sourceAccountId;
    private final UUID destinationAccountId;
    private final BigDecimal amount;
    private TransferStatus status;
    private long version;

    /** Constructor de reconstrucción (lo usa el adaptador de persistencia al cargar de la BD). */
    public Transfer(TransferId id, UUID sourceAccountId, UUID destinationAccountId,
                    BigDecimal amount, TransferStatus status, long version) {
        this.id = Objects.requireNonNull(id, "id");
        this.sourceAccountId = Objects.requireNonNull(sourceAccountId, "sourceAccountId");
        this.destinationAccountId = Objects.requireNonNull(destinationAccountId, "destinationAccountId");
        this.amount = Objects.requireNonNull(amount, "amount");
        this.status = Objects.requireNonNull(status, "status");
        this.version = version;
    }

    /** Factoría de creación de una transferencia nueva en estado {@link TransferStatus#PENDING}. */
    public static Transfer request(UUID sourceAccountId, UUID destinationAccountId, BigDecimal amount) {
        requireDistinctAccounts(sourceAccountId, destinationAccountId);
        requirePositive(amount);
        return new Transfer(TransferId.newId(), sourceAccountId, destinationAccountId, amount,
                TransferStatus.PENDING, 0L);
    }

    /** {@code PENDING -> DEBITED}: el origen se debitó con éxito. */
    public void markDebited() {
        transition(TransferStatus.PENDING, TransferStatus.DEBITED);
    }

    /** {@code PENDING -> FAILED}: el débito del origen fue rechazado (no hay nada que compensar). */
    public void markDebitFailed() {
        transition(TransferStatus.PENDING, TransferStatus.FAILED);
    }

    /** {@code DEBITED -> COMPLETED}: el crédito al destino se aplicó; la saga termina con éxito. */
    public void markCompleted() {
        transition(TransferStatus.DEBITED, TransferStatus.COMPLETED);
    }

    /** {@code DEBITED -> COMPENSATING}: el crédito falló; se solicitará el reembolso del origen. */
    public void markCompensating() {
        transition(TransferStatus.DEBITED, TransferStatus.COMPENSATING);
    }

    /** {@code COMPENSATING -> FAILED}: el reembolso del origen se aplicó; la saga termina en fallo. */
    public void markRefunded() {
        transition(TransferStatus.COMPENSATING, TransferStatus.FAILED);
    }

    private void transition(TransferStatus expected, TransferStatus target) {
        if (this.status != expected) {
            throw new InvalidTransferStateException(this.status, target);
        }
        this.status = target;
    }

    private static void requireDistinctAccounts(UUID source, UUID destination) {
        if (Objects.equals(source, destination)) {
            throw new IllegalArgumentException("La cuenta origen y destino no pueden ser la misma");
        }
    }

    private static void requirePositive(BigDecimal amount) {
        if (amount == null || amount.signum() <= 0) {
            throw new IllegalArgumentException("El importe de la transferencia debe ser positivo");
        }
    }

    public TransferId id() {
        return id;
    }

    public UUID sourceAccountId() {
        return sourceAccountId;
    }

    public UUID destinationAccountId() {
        return destinationAccountId;
    }

    public BigDecimal amount() {
        return amount;
    }

    public TransferStatus status() {
        return status;
    }

    public long version() {
        return version;
    }
}
