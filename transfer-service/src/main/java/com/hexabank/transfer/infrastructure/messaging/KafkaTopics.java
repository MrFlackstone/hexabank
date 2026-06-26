package com.hexabank.transfer.infrastructure.messaging;

import org.apache.kafka.clients.admin.NewTopic;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.config.TopicBuilder;

/**
 * Declaración de los topics que usa transfer-service y sus Dead Letter Topics. {@code KafkaAdmin} los
 * crea al arrancar si no existen, con un número de particiones fijo para no depender del autocreado del
 * broker.
 */
@Configuration
public class KafkaTopics {

    /** Comandos que transfer-service produce hacia account-service. */
    public static final String ACCOUNT_COMMANDS = "account-commands";
    /** Eventos de account-service que transfer-service consume. */
    public static final String ACCOUNT_EVENTS = "account-events";
    /** Eventos terminales de la transferencia que transfer-service produce. */
    public static final String TRANSFER_EVENTS = "transfer-events";

    private static final int PARTITIONS = 3;
    private static final short REPLICAS = 1;

    @Bean
    NewTopic accountCommandsTopic() {
        return TopicBuilder.name(ACCOUNT_COMMANDS).partitions(PARTITIONS).replicas(REPLICAS).build();
    }

    @Bean
    NewTopic accountEventsTopic() {
        return TopicBuilder.name(ACCOUNT_EVENTS).partitions(PARTITIONS).replicas(REPLICAS).build();
    }

    @Bean
    NewTopic transferEventsTopic() {
        return TopicBuilder.name(TRANSFER_EVENTS).partitions(PARTITIONS).replicas(REPLICAS).build();
    }

    @Bean
    NewTopic accountEventsDltTopic() {
        return TopicBuilder.name(ACCOUNT_EVENTS + ".DLT").partitions(PARTITIONS).replicas(REPLICAS).build();
    }

    @Bean
    NewTopic transferEventsDltTopic() {
        return TopicBuilder.name(TRANSFER_EVENTS + ".DLT").partitions(PARTITIONS).replicas(REPLICAS).build();
    }
}
