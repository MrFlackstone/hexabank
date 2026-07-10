-- Fase 12 (tracing distribuido): guarda el traceparent W3C del span activo en el momento de encolar
-- el evento. El relay lo reinyecta como cabecera Kafka al publicar, de modo que la traza sobreviva al
-- salto asíncrono outbox -> relay (hilos distintos) y una transferencia produzca una sola traza que
-- cruza el broker. NULL = evento encolado fuera de una traza (p. ej. reaper sin contexto HTTP).
ALTER TABLE outbox ADD COLUMN traceparent VARCHAR(64);
