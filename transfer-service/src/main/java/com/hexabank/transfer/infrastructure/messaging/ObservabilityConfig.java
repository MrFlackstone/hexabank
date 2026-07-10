package com.hexabank.transfer.infrastructure.messaging;

import io.micrometer.observation.ObservationPredicate;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Ajustes de observabilidad para el tracing (Fase 12).
 */
@Configuration
public class ObservabilityConfig {

    /**
     * No traza los sondeos periódicos del outbox relay y del reaper ({@code @Scheduled} cada pocos
     * cientos de ms): con muestreo al 100% inundarían Tempo con miles de trazas vacías. Las trazas de
     * interés son las de negocio (la transferencia end-to-end a través de la saga).
     */
    @Bean
    ObservationPredicate suppressScheduledTaskObservations() {
        return (name, context) -> !"tasks.scheduled.execution".equals(name);
    }
}
