package com.hexabank.transfer.infrastructure.messaging;

import com.hexabank.shared.events.CreditRequested;
import com.hexabank.shared.events.DebitRequested;
import com.hexabank.shared.events.DomainEvent;
import com.hexabank.shared.events.RefundRequested;
import com.hexabank.shared.events.TransferCompleted;
import com.hexabank.shared.events.TransferFailed;
import com.hexabank.transfer.application.port.out.EventPublisherPort;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

/**
 * Adaptador de salida que publica los eventos de la saga: los comandos van a {@code account-commands}
 * (clave = accountId) y los eventos terminales a {@code transfer-events} (clave = transferId). El topic
 * y la clave se eligen por pattern matching del record.
 */
@Component
public class KafkaEventPublisher implements EventPublisherPort {

    private final KafkaTemplate<String, Object> kafkaTemplate;

    public KafkaEventPublisher(KafkaTemplate<String, Object> kafkaTemplate) {
        this.kafkaTemplate = kafkaTemplate;
    }

    @Override
    public void publish(DomainEvent event) {
        switch (event) {
            case DebitRequested e -> send(KafkaTopics.ACCOUNT_COMMANDS, e.accountId().toString(), e);
            case CreditRequested e -> send(KafkaTopics.ACCOUNT_COMMANDS, e.accountId().toString(), e);
            case RefundRequested e -> send(KafkaTopics.ACCOUNT_COMMANDS, e.accountId().toString(), e);
            case TransferCompleted e -> send(KafkaTopics.TRANSFER_EVENTS, e.transferId().toString(), e);
            case TransferFailed e -> send(KafkaTopics.TRANSFER_EVENTS, e.transferId().toString(), e);
            default -> throw new IllegalArgumentException(
                    "transfer-service no produce este tipo de evento: " + event.getClass().getSimpleName());
        }
    }

    private void send(String topic, String key, DomainEvent event) {
        kafkaTemplate.send(topic, key, event);
    }
}
