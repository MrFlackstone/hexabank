package com.hexabank.account.contract;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.kafka.clients.producer.KafkaProducer;
import org.apache.kafka.clients.producer.ProducerConfig;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.apache.kafka.common.serialization.StringSerializer;
import org.springframework.cloud.contract.verifier.converter.YamlContract;
import org.springframework.cloud.contract.verifier.messaging.MessageVerifierSender;
import org.springframework.messaging.Message;

import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.Properties;

/**
 * Emisor de mensajes a Kafka para la verificación del lado consumidor de account-service (Spring
 * Cloud Contract, Fase 13b): cuando el stub-runner dispara un {@code label}, envía el
 * {@code outputMessage} del contrato de transfer-service al topic {@code account-commands}; el
 * {@code @KafkaListener} real ({@code AccountCommandListener}) lo procesa. Copia el body como JSON de
 * texto y las cabeceras del contrato (incluida {@code __TypeId__}) para que el {@code JsonDeserializer}
 * reconstruya el tipo concreto.
 */
class KafkaMessageVerifierSender implements MessageVerifierSender<Message<?>> {

    private final KafkaProducer<String, String> producer;
    private final ObjectMapper objectMapper = new ObjectMapper();

    KafkaMessageVerifierSender(String bootstrapServers) {
        Properties props = new Properties();
        props.put(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrapServers);
        this.producer = new KafkaProducer<>(props, new StringSerializer(), new StringSerializer());
    }

    @Override
    public void send(Message<?> message, String destination, YamlContract contract) {
        send(message.getPayload(), message.getHeaders(), destination, contract);
    }

    @Override
    public <T> void send(T payload, Map<String, Object> headers, String destination, YamlContract contract) {
        String value = payload instanceof String text ? text : toJson(payload);
        ProducerRecord<String, String> record = new ProducerRecord<>(destination, null, value);
        if (headers != null) {
            headers.forEach((key, headerValue) -> {
                if (headerValue != null && !key.startsWith("__spring")) {
                    record.headers().add(key, headerValue.toString().getBytes(StandardCharsets.UTF_8));
                }
            });
        }
        try {
            producer.send(record).get();
        } catch (Exception e) {
            throw new IllegalStateException("No se pudo enviar el mensaje de contrato a " + destination, e);
        }
    }

    private String toJson(Object payload) {
        try {
            return objectMapper.writeValueAsString(payload);
        } catch (Exception e) {
            throw new IllegalStateException("No se pudo serializar el cuerpo del contrato a JSON", e);
        }
    }
}
