package com.hexabank.transfer.infrastructure.messaging;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.hexabank.shared.events.DebitRequested;
import com.hexabank.shared.events.TransferCompleted;
import com.hexabank.transfer.infrastructure.persistence.OutboxJpaEntity;
import com.hexabank.transfer.infrastructure.persistence.OutboxJpaRepository;
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
 * Verifica que el adaptador Outbox no envía a Kafka, sino que persiste una fila con el topic, la clave
 * y el payload correctos según el tipo de evento.
 */
@ExtendWith(MockitoExtension.class)
class OutboxEventPublisherTest {

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Mock
    private OutboxJpaRepository outboxRepository;

    @Test
    void persistsCommandRowRoutedToAccountCommandsKeyedByAccount() {
        OutboxEventPublisher publisher = new OutboxEventPublisher(outboxRepository, objectMapper);
        UUID accountId = UUID.randomUUID();
        DebitRequested event = new DebitRequested(
                UUID.randomUUID(), UUID.randomUUID(), accountId, new BigDecimal("100.00"));

        publisher.publish(event);

        ArgumentCaptor<OutboxJpaEntity> captor = ArgumentCaptor.forClass(OutboxJpaEntity.class);
        verify(outboxRepository).save(captor.capture());
        OutboxJpaEntity row = captor.getValue();
        assertThat(row.getEventId()).isEqualTo(event.eventId());
        assertThat(row.getTopic()).isEqualTo(KafkaTopics.ACCOUNT_COMMANDS);
        assertThat(row.getPartitionKey()).isEqualTo(accountId.toString());
        assertThat(row.getEventType()).isEqualTo(DebitRequested.class.getName());
        assertThat(row.getPayload()).contains(accountId.toString());
        assertThat(row.getPublishedAt()).isNull();
    }

    @Test
    void persistsTerminalRowRoutedToTransferEventsKeyedByTransfer() {
        OutboxEventPublisher publisher = new OutboxEventPublisher(outboxRepository, objectMapper);
        UUID transferId = UUID.randomUUID();
        TransferCompleted event = new TransferCompleted(UUID.randomUUID(), transferId);

        publisher.publish(event);

        ArgumentCaptor<OutboxJpaEntity> captor = ArgumentCaptor.forClass(OutboxJpaEntity.class);
        verify(outboxRepository).save(captor.capture());
        OutboxJpaEntity row = captor.getValue();
        assertThat(row.getTopic()).isEqualTo(KafkaTopics.TRANSFER_EVENTS);
        assertThat(row.getPartitionKey()).isEqualTo(transferId.toString());
        assertThat(row.getEventType()).isEqualTo(TransferCompleted.class.getName());
    }
}
