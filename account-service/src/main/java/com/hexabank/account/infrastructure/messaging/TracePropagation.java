package com.hexabank.account.infrastructure.messaging;

import io.micrometer.tracing.TraceContext;
import io.micrometer.tracing.Tracer;
import io.micrometer.tracing.propagation.Propagator;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.Map;

/**
 * Captura de contexto de traza para el patrón Outbox (Fase 12: tracing distribuido).
 *
 * <p>El relay publica los eventos en un hilo distinto (y posterior) al que ejecutó el caso de uso, así
 * que la instrumentación automática de Kafka no puede enlazar el envío con la traza original: en el
 * hilo del relay ya no hay span activo. Este componente captura el {@code traceparent} W3C del span
 * vivo al encolar el evento; el relay lo persiste en la fila outbox y lo reinyecta como cabecera Kafka
 * al publicar, de modo que el consumidor continúe la <strong>misma traza</strong> a través del broker.</p>
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
}
