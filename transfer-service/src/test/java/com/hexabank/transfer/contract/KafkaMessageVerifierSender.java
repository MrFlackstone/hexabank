package com.hexabank.transfer.contract;

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
 * Emisor de mensajes a Kafka para la verificación de contratos del lado consumidor (Spring Cloud
 * Contract, Fase 13b). Cuando el stub-runner dispara un {@code label}, envía el {@code outputMessage}
 * del contrato del productor al topic; el {@code @KafkaListener} real del consumidor lo procesa.
 *
 * <p>El cuerpo se serializa como JSON de texto y las cabeceras del contrato (incluida
 * {@code __TypeId__} con el FQCN del evento) se copian como cabeceras Kafka, de modo que el
 * {@code JsonDeserializer} del consumidor reconstruye el tipo concreto igual que en producción.</p>
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
