package com.hexabank.ledger.infrastructure.messaging;

import com.hexabank.ledger.application.port.in.RecordLedgerUseCase;
import com.hexabank.shared.events.DomainEvent;
import com.hexabank.shared.events.MoneyCredited;
import com.hexabank.shared.events.MoneyDebited;
import com.hexabank.shared.events.MoneyRefunded;
import com.hexabank.shared.events.TransferCompleted;
import com.hexabank.shared.events.TransferFailed;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

/**
 * Adaptador de entrada que consume los eventos de la saga desde {@code account-events} y
 * {@code transfer-events} y los materializa en el ledger.
 *
 * <p>El {@code switch} sobre la {@code sealed interface} es exhaustivo en el conjunto que interesa al
 * ledger; el resto de eventos (comandos, {@code DebitFailed}, {@code CreditFailed}) no son movimientos
 * de dinero y el desenlace ya llega por {@code transfer-events}, así que se ignoran.</p>
 */
@Component
public class LedgerEventListener {

    private static final Logger log = LoggerFactory.getLogger(LedgerEventListener.class);

    private final RecordLedgerUseCase recordLedger;

    public LedgerEventListener(RecordLedgerUseCase recordLedger) {
        this.recordLedger = recordLedger;
    }

    @KafkaListener(
            topics = {KafkaTopics.ACCOUNT_EVENTS, KafkaTopics.TRANSFER_EVENTS},
            containerFactory = "kafkaListenerContainerFactory")
    public void onEvent(DomainEvent event) {
        switch (event) {
            case MoneyDebited e -> recordLedger.onMoneyDebited(e.eventId(), e.transferId(), e.accountId(), e.amount());
            case MoneyCredited e -> recordLedger.onMoneyCredited(e.eventId(), e.transferId(), e.accountId(), e.amount());
            case MoneyRefunded e -> recordLedger.onMoneyRefunded(e.eventId(), e.transferId(), e.accountId(), e.amount());
            case TransferCompleted e -> recordLedger.onTransferCompleted(e.eventId(), e.transferId());
            case TransferFailed e -> recordLedger.onTransferFailed(e.eventId(), e.transferId());
            default -> log.debug("Evento ignorado en el ledger: {}", event.getClass().getSimpleName());
        }
    }
}
