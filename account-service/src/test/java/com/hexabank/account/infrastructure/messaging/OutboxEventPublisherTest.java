package com.hexabank.account.infrastructure.messaging;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.hexabank.account.infrastructure.persistence.OutboxJpaEntity;
import com.hexabank.account.infrastructure.persistence.OutboxJpaRepository;
import com.hexabank.shared.events.MoneyDebited;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;

/**
 * Verifica que el adaptador Outbox persiste una fila hacia {@code account-events} con la cuenta como
 * clave, en lugar de enviar directamente a Kafka.
 */
@ExtendWith(MockitoExtension.class)
class OutboxEventPublisherTest {

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Mock
    private OutboxJpaRepository outboxRepository;

    @Test
    void persistsEventRowRoutedToAccountEventsKeyedByAccount() {
        OutboxEventPublisher publisher = new OutboxEventPublisher(outboxRepository, objectMapper);
        UUID accountId = UUID.randomUUID();
        MoneyDebited event = new MoneyDebited(
                UUID.randomUUID(), UUID.randomUUID(), accountId, new BigDecimal("50.00"));

        publisher.publish(event);

        ArgumentCaptor<OutboxJpaEntity> captor = ArgumentCaptor.forClass(OutboxJpaEntity.class);
        verify(outboxRepository).save(captor.capture());
        OutboxJpaEntity row = captor.getValue();
        assertThat(row.getEventId()).isEqualTo(event.eventId());
        assertThat(row.getTopic()).isEqualTo(KafkaTopics.ACCOUNT_EVENTS);
        assertThat(row.getPartitionKey()).isEqualTo(accountId.toString());
        assertThat(row.getEventType()).isEqualTo(MoneyDebited.class.getName());
        assertThat(row.getPublishedAt()).isNull();
    }
}
