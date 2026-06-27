package com.hexabank.ledger.domain;

import java.math.BigDecimal;
import java.util.UUID;

/**
 * Proyección de lectura de una transferencia (modelo de consulta materializado). Se construye de forma
 * incremental a partir de los eventos de la saga, que pueden llegar en cualquier orden y por topics
 * distintos: cada evento rellena la parte que conoce.
 *
 * <p>El estado se guarda como texto ({@code PENDING}/{@code COMPLETED}/{@code FAILED}) para no acoplar
 * este servicio al enum del dominio de transfer-service.</p>
 */
public class TransferView {

    public static final String PENDING = "PENDING";
    public static final String COMPLETED = "COMPLETED";
    public static final String FAILED = "FAILED";

    private final UUID transferId;
    private UUID sourceAccountId;
    private UUID destinationAccountId;
    private BigDecimal amount;
    private String status;

    public TransferView(UUID transferId, UUID sourceAccountId, UUID destinationAccountId,
                        BigDecimal amount, String status) {
        this.transferId = transferId;
        this.sourceAccountId = sourceAccountId;
        this.destinationAccountId = destinationAccountId;
        this.amount = amount;
        this.status = status;
    }

    /** Proyección recién observada, todavía sin desenlace conocido. */
    public static TransferView pending(UUID transferId) {
        return new TransferView(transferId, null, null, null, PENDING);
    }

    /** El débito confirmó la cuenta origen y el importe de la transferencia. */
    public void recordDebit(UUID sourceAccountId, BigDecimal amount) {
        this.sourceAccountId = sourceAccountId;
        this.amount = amount;
    }

    /** El crédito confirmó la cuenta destino. */
    public void recordCredit(UUID destinationAccountId) {
        this.destinationAccountId = destinationAccountId;
    }

    public void markCompleted() {
        this.status = COMPLETED;
    }

    public void markFailed() {
        this.status = FAILED;
    }

    public UUID transferId() {
        return transferId;
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

    public String status() {
        return status;
    }
}
