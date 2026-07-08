package com.hexabank.account.infrastructure.messaging;

import com.hexabank.shared.events.AccountOpened;
import com.hexabank.shared.events.CreditFailed;
import com.hexabank.shared.events.DebitFailed;
import com.hexabank.shared.events.DomainEvent;
import com.hexabank.shared.events.MoneyCredited;
import com.hexabank.shared.events.MoneyDebited;
import com.hexabank.shared.events.MoneyRefunded;

/**
 * Enrutado de los eventos que produce account-service: todos van a {@code account-events} con el
 * {@code accountId} del evento como clave de partición.
 *
 * <p>Lo usa el {@code OutboxEventPublisher} para calcular topic y clave al insertar la fila; el relay
 * los reutiliza tal cual al publicar.</p>
 */
final class EventRouting {

    private EventRouting() {
    }

    static String topicFor(DomainEvent event) {
        return KafkaTopics.ACCOUNT_EVENTS;
    }

    static String keyFor(DomainEvent event) {
        return switch (event) {
            case MoneyDebited e -> e.accountId().toString();
            case DebitFailed e -> e.accountId().toString();
            case MoneyCredited e -> e.accountId().toString();
            case CreditFailed e -> e.accountId().toString();
            case MoneyRefunded e -> e.accountId().toString();
            case AccountOpened e -> e.accountId().toString();
            default -> throw new IllegalArgumentException(
                    "account-service no produce este tipo de evento: " + event.getClass().getSimpleName());
        };
    }
}
