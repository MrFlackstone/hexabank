package com.hexabank.ledger.infrastructure.rest;

import com.hexabank.ledger.infrastructure.sse.SseLedgerBroadcaster;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

/**
 * Adaptador de entrada que expone el feed en vivo del libro mayor por Server-Sent Events. El frontend
 * (Fase 4) se suscribe a {@code GET /ledger/stream} y recibe cada asiento nuevo en tiempo real.
 */
@RestController
public class LedgerStreamController {

    private final SseLedgerBroadcaster broadcaster;

    public LedgerStreamController(SseLedgerBroadcaster broadcaster) {
        this.broadcaster = broadcaster;
    }

    @GetMapping(value = "/ledger/stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public SseEmitter stream() {
        return broadcaster.subscribe();
    }
}
