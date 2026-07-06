package com.hexabank.ledger.infrastructure.sse;

import com.hexabank.ledger.application.port.in.CallerContext;
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
 * lista de suscriptores conectados y empuja cada asiento nuevo <em>solo</em> a quienes pueden verlo:
 * el propietario de la cuenta afectada y los suscriptores STAFF.
 *
 * <p>La lista es {@link CopyOnWriteArrayList} porque hay concurrencia entre el hilo del consumidor de
 * Kafka (que difunde) y los hilos HTTP (que añaden/retiran emisores). Un emisor que falla al enviar se
 * descarta.</p>
 */
@Component
public class SseLedgerBroadcaster implements LedgerBroadcastPort {

    private static final Logger log = LoggerFactory.getLogger(SseLedgerBroadcaster.class);
    private static final long SSE_TIMEOUT_MS = 3_600_000L; // 1 hora

    /** Suscriptor SSE con la identidad con la que se autenticó al abrir el stream. */
    private record Subscription(SseEmitter emitter, String subject, boolean staff) {

        boolean canSee(String ownerId) {
            return staff || (ownerId != null && ownerId.equals(subject));
        }
    }

    private final List<Subscription> subscriptions = new CopyOnWriteArrayList<>();

    /** Registra un nuevo suscriptor SSE y lo retira al completarse, expirar o fallar. */
    public SseEmitter subscribe(CallerContext caller) {
        SseEmitter emitter = new SseEmitter(SSE_TIMEOUT_MS);
        Subscription subscription = new Subscription(emitter, caller.subject(), caller.staff());
        emitter.onCompletion(() -> subscriptions.remove(subscription));
        emitter.onTimeout(() -> subscriptions.remove(subscription));
        emitter.onError(e -> subscriptions.remove(subscription));
        subscriptions.add(subscription);
        try {
            // Handshake: un comentario SSE inicial fuerza el volcado de las cabeceras 200 al conectar.
            // Sin el, Spring no confirma la respuesta hasta el primer asiento, y el cliente se quedaria
            // en "conectando" (y su onopen no dispararia) hasta que hubiera actividad. Un comentario (:)
            // no llega al onmessage del cliente.
            emitter.send(SseEmitter.event().comment("connected"));
        } catch (IOException ex) {
            subscriptions.remove(subscription);
            emitter.completeWithError(ex);
        }
        return emitter;
    }

    @Override
    public void broadcast(LedgerEntry entry, String ownerId) {
        LedgerEntryResponse payload = LedgerEntryResponse.from(entry);
        for (Subscription subscription : subscriptions) {
            if (!subscription.canSee(ownerId)) {
                continue;
            }
            try {
                subscription.emitter().send(SseEmitter.event().name("ledger-entry").data(payload));
            } catch (IOException | IllegalStateException ex) {
                log.debug("Suscriptor SSE caído, se descarta: {}", ex.getMessage());
                subscriptions.remove(subscription);
            }
        }
    }
}
