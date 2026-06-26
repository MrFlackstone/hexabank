package com.hexabank.transfer.infrastructure.messaging;

import com.hexabank.shared.events.CreditFailed;
import com.hexabank.shared.events.DebitFailed;
import com.hexabank.shared.events.DomainEvent;
import com.hexabank.shared.events.MoneyCredited;
import com.hexabank.shared.events.MoneyDebited;
import com.hexabank.shared.events.MoneyRefunded;
import com.hexabank.transfer.application.port.in.TransferSagaUseCase;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

/**
 * Adaptador de entrada que consume los eventos de account-service desde {@code account-events} y los
 * delega al orquestador de la saga.
 */
@Component
public class AccountEventListener {

    private static final Logger log = LoggerFactory.getLogger(AccountEventListener.class);

    private final TransferSagaUseCase transferSaga;

    public AccountEventListener(TransferSagaUseCase transferSaga) {
        this.transferSaga = transferSaga;
    }

    @KafkaListener(topics = KafkaTopics.ACCOUNT_EVENTS, containerFactory = "kafkaListenerContainerFactory")
    public void onEvent(DomainEvent event) {
        switch (event) {
            case MoneyDebited e -> transferSaga.onMoneyDebited(e.eventId(), e.transferId());
            case DebitFailed e -> transferSaga.onDebitFailed(e.eventId(), e.transferId(), e.reason());
            case MoneyCredited e -> transferSaga.onMoneyCredited(e.eventId(), e.transferId());
            case CreditFailed e -> transferSaga.onCreditFailed(e.eventId(), e.transferId(), e.reason());
            case MoneyRefunded e -> transferSaga.onMoneyRefunded(e.eventId(), e.transferId());
            default -> log.warn("Evento ignorado en account-events: {}", event.getClass().getSimpleName());
        }
    }
}
