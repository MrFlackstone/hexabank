package com.hexabank.account.contract;

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
 * Receptor de mensajes de Kafka para la verificación de contratos del productor (Spring Cloud
 * Contract, Fase 13b). SCC 4.x ya no trae middleware de Kafka: el test generado llama a
 * {@code contractVerifierMessaging.receive("<topic>")}, que delega en este bean.
 *
 * <p>El test generado hace primero el {@code triggeredBy(...)} y luego el {@code receive(...)}. Para
 * que el receptor no lea mensajes de otros tests, cada método de test hace {@link #seekToEnd(String)}
 * en un {@code @BeforeEach} de la clase base: así el consumidor queda posicionado al final antes de
 * que el trigger publique, y {@link #receive} devuelve exactamente el mensaje recién emitido.</p>
 *
 * <p>El valor se lee como texto (JSON crudo) y las cabeceras Kafka (incluida {@code __TypeId__}, que
 * lleva el FQCN del evento) se copian a la {@link Message} para que el contrato pueda aseverarlas.</p>
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

    /** Asigna (una vez) todas las particiones del topic y coloca el offset al final. */
    synchronized void seekToEnd(String topic) {
        List<TopicPartition> partitions = consumer.partitionsFor(topic).stream()
                .map(info -> new TopicPartition(topic, info.partition()))
                .toList();
        if (assignedTopics.add(topic)) {
            Set<TopicPartition> all = new HashSet<>(consumer.assignment());
            all.addAll(partitions);
            consumer.assign(all);
        }
        consumer.seekToEnd(partitions);
        partitions.forEach(consumer::position); // fuerza la resolución de la posición
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
