package com.hexabank.account.infrastructure.messaging;

import org.apache.kafka.clients.admin.NewTopic;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.config.TopicBuilder;

/**
 * Declaración de los topics que usa account-service y sus Dead Letter Topics. {@code KafkaAdmin} los
 * crea al arrancar si no existen.
 */
@Configuration
public class KafkaTopics {

    /** Comandos que account-service consume. */
    public static final String ACCOUNT_COMMANDS = "account-commands";
    /** Eventos que account-service produce con el resultado de cada comando. */
    public static final String ACCOUNT_EVENTS = "account-events";

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
    NewTopic accountCommandsDltTopic() {
        return TopicBuilder.name(ACCOUNT_COMMANDS + ".DLT").partitions(PARTITIONS).replicas(REPLICAS).build();
    }

    @Bean
    NewTopic accountEventsDltTopic() {
        return TopicBuilder.name(ACCOUNT_EVENTS + ".DLT").partitions(PARTITIONS).replicas(REPLICAS).build();
    }
}
