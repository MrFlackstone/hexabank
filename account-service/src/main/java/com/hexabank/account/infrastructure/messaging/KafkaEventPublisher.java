package com.hexabank.account.infrastructure.messaging;

import com.hexabank.account.application.port.out.EventPublisherPort;
import com.hexabank.shared.events.CreditFailed;
import com.hexabank.shared.events.DebitFailed;
import com.hexabank.shared.events.DomainEvent;
import com.hexabank.shared.events.MoneyCredited;
import com.hexabank.shared.events.MoneyDebited;
import com.hexabank.shared.events.MoneyRefunded;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

/**
 * Adaptador de salida que publica los eventos de resultado en {@code account-events}, usando el
 * {@code accountId} del evento como clave de partición.
 */
@Component
public class KafkaEventPublisher implements EventPublisherPort {

    private final KafkaTemplate<String, Object> kafkaTemplate;

    public KafkaEventPublisher(KafkaTemplate<String, Object> kafkaTemplate) {
        this.kafkaTemplate = kafkaTemplate;
    }

    @Override
    public void publish(DomainEvent event) {
        kafkaTemplate.send(KafkaTopics.ACCOUNT_EVENTS, partitionKey(event), event);
    }

    private String partitionKey(DomainEvent event) {
        return switch (event) {
            case MoneyDebited e -> e.accountId().toString();
            case DebitFailed e -> e.accountId().toString();
            case MoneyCredited e -> e.accountId().toString();
            case CreditFailed e -> e.accountId().toString();
            case MoneyRefunded e -> e.accountId().toString();
            default -> throw new IllegalArgumentException(
                    "account-service no produce este tipo de evento: " + event.getClass().getSimpleName());
        };
    }
}
