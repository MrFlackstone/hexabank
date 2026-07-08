package com.hexabank.transfer.infrastructure.messaging;

import com.hexabank.shared.events.CreditRequested;
import com.hexabank.shared.events.DebitRequested;
import com.hexabank.shared.events.DomainEvent;
import com.hexabank.shared.events.RefundRequested;
import com.hexabank.shared.events.TransferCompleted;
import com.hexabank.shared.events.TransferFailed;

/**
 * Enrutado de los eventos que produce transfer-service: los comandos van a {@code account-commands}
 * (clave = accountId) y los eventos terminales a {@code transfer-events} (clave = transferId). El topic
 * y la clave se eligen por pattern matching del record.
 *
 * <p>Lo usa el {@code OutboxEventPublisher} para calcular topic y clave al insertar la fila; el relay
 * los reutiliza tal cual al publicar, sin volver a decidir.</p>
 */
final class EventRouting {

    private EventRouting() {
    }

    static String topicFor(DomainEvent event) {
        return switch (event) {
            case DebitRequested ignored -> KafkaTopics.ACCOUNT_COMMANDS;
            case CreditRequested ignored -> KafkaTopics.ACCOUNT_COMMANDS;
            case RefundRequested ignored -> KafkaTopics.ACCOUNT_COMMANDS;
            case TransferCompleted ignored -> KafkaTopics.TRANSFER_EVENTS;
            case TransferFailed ignored -> KafkaTopics.TRANSFER_EVENTS;
            default -> throw new IllegalArgumentException(
                    "transfer-service no produce este tipo de evento: " + event.getClass().getSimpleName());
        };
    }

    static String keyFor(DomainEvent event) {
        return switch (event) {
            case DebitRequested e -> e.accountId().toString();
            case CreditRequested e -> e.accountId().toString();
            case RefundRequested e -> e.accountId().toString();
            case TransferCompleted e -> e.transferId().toString();
            case TransferFailed e -> e.transferId().toString();
            default -> throw new IllegalArgumentException(
                    "transfer-service no produce este tipo de evento: " + event.getClass().getSimpleName());
        };
    }
}
