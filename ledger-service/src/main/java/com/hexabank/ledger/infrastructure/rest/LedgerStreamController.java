package com.hexabank.ledger.infrastructure.rest;

import com.hexabank.ledger.infrastructure.sse.SseLedgerBroadcaster;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.MediaType;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

/**
 * Adaptador de entrada que expone el feed en vivo del libro mayor por Server-Sent Events. El frontend
 * se suscribe a {@code GET /ledger/stream} con el JWT en la cabecera {@code Authorization: Bearer}
 * (consumido con un cliente SSE basado en fetch, no el {@code EventSource} nativo) y recibe en tiempo
 * real los asientos de sus cuentas (todos, si es STAFF).
 */
@RestController
@Tag(name = "Libro mayor (stream SSE)",
        description = "Feed en vivo del libro mayor por Server-Sent Events; requiere Bearer en la cabecera.")
public class LedgerStreamController {

    private final SseLedgerBroadcaster broadcaster;

    public LedgerStreamController(SseLedgerBroadcaster broadcaster) {
        this.broadcaster = broadcaster;
    }

    @Operation(summary = "Feed en vivo del libro (SSE)",
            description = "Server-Sent Events (text/event-stream). El token va en la cabecera Authorization: Bearer "
                    + "(no como query param). Swagger UI no renderiza bien un stream SSE; usar el frontend o curl para probarlo.")
    @GetMapping(value = "/v1/ledger/stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public SseEmitter stream(Authentication authentication) {
        return broadcaster.subscribe(CallerContexts.from(authentication));
    }
}
