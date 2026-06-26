package com.hexabank.transfer.domain;

/**
 * Estados por los que pasa una transferencia en la saga.
 *
 * <ul>
 *   <li>{@code PENDING} — creada; se ha solicitado el débito del origen.</li>
 *   <li>{@code DEBITED} — el origen se debitó; se ha solicitado el crédito al destino.</li>
 *   <li>{@code COMPLETED} — débito y crédito firmes; estado terminal de éxito.</li>
 *   <li>{@code COMPENSATING} — el crédito falló; se ha solicitado el reembolso del origen.</li>
 *   <li>{@code FAILED} — estado terminal de fallo (débito rechazado, o crédito rechazado ya compensado).</li>
 * </ul>
 */
public enum TransferStatus {
    PENDING,
    DEBITED,
    COMPLETED,
    COMPENSATING,
    FAILED
}
