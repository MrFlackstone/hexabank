package com.hexabank.transfer.infrastructure.messaging;

import com.hexabank.transfer.config.TestSecurityConfig;
import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.clients.consumer.ConsumerRecords;
import org.apache.kafka.clients.consumer.KafkaConsumer;
import org.apache.kafka.clients.producer.KafkaProducer;
import org.apache.kafka.clients.producer.ProducerConfig;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.apache.kafka.common.serialization.StringSerializer;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.DockerImageName;

import java.time.Duration;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CopyOnWriteArrayList;

import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;

/**
 * Test de integración del Dead Letter Topic: comprueba que un mensaje "veneno" (que el consumidor no
 * puede deserializar) no bloquea la partición ni se reintenta indefinidamente, sino que el
 * {@code DefaultErrorHandler} + {@code DeadLetterPublishingRecoverer} lo reubican en
 * {@code account-events.DLT}. Requiere Docker.
 */
@SpringBootTest
@Testcontainers
@ActiveProfiles("test")
@Import(TestSecurityConfig.class)
class KafkaDeadLetterIT {

    @Container
    @ServiceConnection
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:16-alpine");

    @Container
    static org.testcontainers.kafka.KafkaContainer kafka =
            new org.testcontainers.kafka.KafkaContainer(DockerImageName.parse("apache/kafka:3.9.1"));

    @DynamicPropertySource
    static void kafkaProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.kafka.bootstrap-servers", kafka::getBootstrapServers);
    }

    private KafkaConsumer<String, String> dltConsumer;

    @AfterEach
    void closeConsumer() {
        if (dltConsumer != null) {
            dltConsumer.close();
        }
    }

    @Test
    @DisplayName("Un mensaje que no se puede deserializar acaba en account-events.DLT")
    void poisonMessageIsRoutedToDeadLetterTopic() {
        String poison = "esto-no-es-un-evento-de-dominio-valido-" + UUID.randomUUID();
        List<String> deadLetters = subscribeToDeadLetterTopic();

        publishPoison(poison);

        await().atMost(Duration.ofSeconds(30)).untilAsserted(() -> {
            drainInto(deadLetters);
            assertThat(deadLetters).contains(poison);
        });
    }

    private void publishPoison(String poison) {
        Map<String, Object> props = new HashMap<>();
        props.put(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, kafka.getBootstrapServers());
        try (KafkaProducer<String, String> producer =
                     new KafkaProducer<>(props, new StringSerializer(), new StringSerializer())) {
            producer.send(new ProducerRecord<>(KafkaTopics.ACCOUNT_EVENTS, UUID.randomUUID().toString(), poison));
            producer.flush();
        }
    }

    private List<String> subscribeToDeadLetterTopic() {
        Map<String, Object> props = new HashMap<>();
        props.put(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, kafka.getBootstrapServers());
        props.put(ConsumerConfig.GROUP_ID_CONFIG, "it-dlt-verifier-" + UUID.randomUUID());
        props.put(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "earliest");
        dltConsumer = new KafkaConsumer<>(props, new StringDeserializer(), new StringDeserializer());
        dltConsumer.subscribe(List.of(KafkaTopics.ACCOUNT_EVENTS + ".DLT"));
        return new CopyOnWriteArrayList<>();
    }

    private void drainInto(List<String> sink) {
        ConsumerRecords<String, String> records = dltConsumer.poll(Duration.ofMillis(500));
        for (ConsumerRecord<String, String> record : records) {
            sink.add(record.value());
        }
    }
}
