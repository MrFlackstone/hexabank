package com.hexabank.shared.events;

import java.util.UUID;

/**
 * Evento que participa en la saga de transferencia: siempre está asociado a una transferencia
 * concreta identificada por {@link #transferId()}.
 */
public sealed interface SagaEvent extends DomainEvent
        permits DebitRequested, CreditRequested, RefundRequested,
                MoneyDebited, DebitFailed, MoneyCredited, CreditFailed, MoneyRefunded,
                TransferCompleted, TransferFailed {

    /** Identificador de la transferencia a la que pertenece el evento. */
    UUID transferId();
}
