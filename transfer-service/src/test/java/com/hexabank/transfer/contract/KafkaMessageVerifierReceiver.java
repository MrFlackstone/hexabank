package com.hexabank.transfer.contract;

import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.clients.consumer.ConsumerRecords;
import org.apache.kafka.clients.consumer.KafkaConsumer;
import org.apache.kafka.common.TopicPartition;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.springframework.cloud.contract.verifier.converter.YamlContract;
import org.springframework.cloud.contract.verifier.messaging.MessageVerifierReceiver;
import org.springframework.messaging.Message;
import org.springframework.messaging.support.MessageBuilder;

import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

/**
 * Receptor de mensajes de Kafka para la verificación de contratos del productor transfer-service
 * (Spring Cloud Contract, Fase 13b). Ver la explicación en el equivalente de account-service: SCC 4.x
 * no trae middleware de Kafka, así que el test generado delega en este bean. Cada test hace
 * {@link #seekToEnd(String)} en el {@code @BeforeEach} de la base para aislar el mensaje del trigger.
 * Sirve para varios topics (account-commands y transfer-events); {@link #receive} filtra por destino.
 */
class KafkaMessageVerifierReceiver implements MessageVerifierReceiver<Message<?>> {

    private final KafkaConsumer<String, String> consumer;
    private final Set<String> assignedTopics = new HashSet<>();

    KafkaMessageVerifierReceiver(String bootstrapServers) {
        Properties props = new Properties();
        props.put(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrapServers);
        props.put(ConsumerConfig.GROUP_ID_CONFIG, "scc-contract-verifier-" + UUID.randomUUID());
        props.put(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "latest");
        props.put(ConsumerConfig.ENABLE_AUTO_COMMIT_CONFIG, false);
        this.consumer = new KafkaConsumer<>(props, new StringDeserializer(), new StringDeserializer());
    }

    /**
     * Deja el consumidor en el final actual del topic usando solo {@code poll()} (sin
     * {@code position()}/{@code seekToEnd()}, que pueden bloquear). Con {@code auto.offset.reset=latest}
     * un consumidor recién asignado arranca al final; para topics ya asignados, drena lo pendiente.
     */
    synchronized void seekToEnd(String topic) {
        if (assignedTopics.add(topic)) {
            List<TopicPartition> partitions = consumer.partitionsFor(topic).stream()
                    .map(info -> new TopicPartition(topic, info.partition()))
                    .toList();
            Set<TopicPartition> all = new HashSet<>(consumer.assignment());
            all.addAll(partitions);
            consumer.assign(all);
        }
        ConsumerRecords<String, String> records;
        do {
            records = consumer.poll(Duration.ofMillis(300));
        } while (!records.isEmpty());
    }

    @Override
    public Message<?> receive(String destination, long timeout, TimeUnit timeUnit, YamlContract contract) {
        long deadline = System.currentTimeMillis() + timeUnit.toMillis(timeout);
        while (System.currentTimeMillis() < deadline) {
            ConsumerRecords<String, String> records = consumer.poll(Duration.ofMillis(200));
            for (ConsumerRecord<String, String> record : records) {
                if (record.topic().equals(destination)) {
                    return toMessage(record);
                }
            }
        }
        return null;
    }

    @Override
    public Message<?> receive(String destination, YamlContract contract) {
        return receive(destination, 15, TimeUnit.SECONDS, contract);
    }

    private Message<?> toMessage(ConsumerRecord<String, String> record) {
        Map<String, Object> headers = new HashMap<>();
        record.headers().forEach(header ->
                headers.put(header.key(), new String(header.value(), StandardCharsets.UTF_8)));
        return MessageBuilder.withPayload((Object) record.value()).copyHeaders(headers).build();
    }
}
