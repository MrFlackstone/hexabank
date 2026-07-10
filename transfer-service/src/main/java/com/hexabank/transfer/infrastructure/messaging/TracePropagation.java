package com.hexabank.transfer.infrastructure.messaging;

import io.micrometer.tracing.Span;
import io.micrometer.tracing.TraceContext;
import io.micrometer.tracing.Tracer;
import io.micrometer.tracing.propagation.Propagator;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.Map;

/**
 * Puente de contexto de traza para el patrón Outbox (Fase 12: tracing distribuido).
 *
 * <p>El relay publica los eventos en un hilo distinto (y posterior) al que ejecutó el caso de uso, así
 * que la instrumentación automática de Kafka no puede enlazar el envío con la traza original: en el
 * hilo del relay ya no hay span activo. Este componente cierra ese hueco: captura el {@code traceparent}
 * W3C del span vivo al encolar el evento (para persistirlo en la fila outbox) y lo restablece en el
 * relay, de modo que el span de productor de Kafka cuelgue de la traza original y una transferencia
 * produzca <strong>una sola traza</strong> a través del broker.</p>
 */
@Component
public class TracePropagation {

    private final Tracer tracer;
    private final Propagator propagator;

    public TracePropagation(Tracer tracer, Propagator propagator) {
        this.tracer = tracer;
        this.propagator = propagator;
    }

    /** {@code traceparent} W3C del span activo, o {@code null} si no hay traza en curso. */
    public String currentTraceparent() {
        TraceContext context = tracer.currentTraceContext().context();
        if (context == null) {
            return null;
        }
        Map<String, String> carrier = new HashMap<>();
        propagator.inject(context, carrier, Map::put);
        return carrier.get("traceparent");
    }

    /**
     * Ejecuta {@code action} con el contexto de traza reconstruido desde {@code traceparent} activo en
     * el hilo actual, para que la observación del productor Kafka enlace su span como hijo de la traza
     * original. Si {@code traceparent} es {@code null}/vacío, ejecuta la acción sin contexto.
     */
    public <T> T withRestoredContext(String traceparent, TraceCallable<T> action) throws Exception {
        if (traceparent == null || traceparent.isBlank()) {
            return action.call();
        }
        Map<String, String> carrier = new HashMap<>();
        carrier.put("traceparent", traceparent);
        Span span = propagator.extract(carrier, Map::get).name("outbox-relay publish").start();
        try (Tracer.SpanInScope ignored = tracer.withSpan(span)) {
            return action.call();
        } finally {
            span.end();
        }
    }

    /** Acción que puede propagar excepciones (p. ej. el {@code get()} bloqueante del envío a Kafka). */
    @FunctionalInterface
    public interface TraceCallable<T> {
        T call() throws Exception;
    }
}
