package com.hexabank.ledger.infrastructure.sse;

import com.hexabank.ledger.application.port.out.LedgerBroadcastPort;
import com.hexabank.ledger.domain.LedgerEntry;
import com.hexabank.ledger.infrastructure.rest.dto.LedgerEntryResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.IOException;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * Adaptador de salida que implementa {@link LedgerBroadcastPort} sobre Server-Sent Events. Mantiene la
 * lista de suscriptores conectados y empuja cada asiento nuevo a todos ellos.
 *
 * <p>La lista es {@link CopyOnWriteArrayList} porque hay concurrencia entre el hilo del consumidor de
 * Kafka (que difunde) y los hilos HTTP (que añaden/retiran emisores). Un emisor que falla al enviar se
 * descarta.</p>
 */
@Component
public class SseLedgerBroadcaster implements LedgerBroadcastPort {

    private static final Logger log = LoggerFactory.getLogger(SseLedgerBroadcaster.class);
    private static final long SSE_TIMEOUT_MS = 3_600_000L; // 1 hora

    private final List<SseEmitter> emitters = new CopyOnWriteArrayList<>();

    /** Registra un nuevo suscriptor SSE y lo retira al completarse, expirar o fallar. */
    public SseEmitter subscribe() {
        SseEmitter emitter = new SseEmitter(SSE_TIMEOUT_MS);
        emitter.onCompletion(() -> emitters.remove(emitter));
        emitter.onTimeout(() -> emitters.remove(emitter));
        emitter.onError(e -> emitters.remove(emitter));
        emitters.add(emitter);
        return emitter;
    }

    @Override
    public void broadcast(LedgerEntry entry) {
        LedgerEntryResponse payload = LedgerEntryResponse.from(entry);
        for (SseEmitter emitter : emitters) {
            try {
                emitter.send(SseEmitter.event().name("ledger-entry").data(payload));
            } catch (IOException | IllegalStateException ex) {
                log.debug("Suscriptor SSE caído, se descarta: {}", ex.getMessage());
                emitters.remove(emitter);
            }
        }
    }
}
