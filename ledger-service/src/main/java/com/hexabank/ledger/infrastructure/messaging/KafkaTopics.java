package com.hexabank.ledger.infrastructure.messaging;

import org.apache.kafka.clients.admin.NewTopic;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.config.TopicBuilder;

/**
 * Topics que consume ledger-service y sus Dead Letter Topics. {@code KafkaAdmin} los crea al arrancar
 * si no existen, de forma idéntica a los demás servicios (mismas particiones, no se depende del
 * autocreado del broker).
 */
@Configuration
public class KafkaTopics {

    /** Eventos de cuenta (movimientos de dinero) que el ledger materializa como asientos. */
    public static final String ACCOUNT_EVENTS = "account-events";
    /** Eventos terminales de la transferencia que actualizan la proyección. */
    public static final String TRANSFER_EVENTS = "transfer-events";

    private static final int PARTITIONS = 3;
    private static final short REPLICAS = 1;

    @Bean
    NewTopic accountEventsTopic() {
        return TopicBuilder.name(ACCOUNT_EVENTS).partitions(PARTITIONS).replicas(REPLICAS).build();
    }

    @Bean
    NewTopic transferEventsTopic() {
        return TopicBuilder.name(TRANSFER_EVENTS).partitions(PARTITIONS).replicas(REPLICAS).build();
    }

    @Bean
    NewTopic accountEventsLedgerDltTopic() {
        return TopicBuilder.name(ACCOUNT_EVENTS + ".DLT").partitions(PARTITIONS).replicas(REPLICAS).build();
    }

    @Bean
    NewTopic transferEventsLedgerDltTopic() {
        return TopicBuilder.name(TRANSFER_EVENTS + ".DLT").partitions(PARTITIONS).replicas(REPLICAS).build();
    }
}
