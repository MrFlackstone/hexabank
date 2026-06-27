package com.hexabank.ledger.domain;

/**
 * Naturaleza de un asiento del libro mayor: salida (débito), entrada (crédito) o devolución
 * (reembolso de una compensación).
 */
public enum EntryType {
    DEBIT,
    CREDIT,
    REFUND
}
