package com.hexabank.transfer.infrastructure.scheduling;

import com.hexabank.transfer.application.port.in.ReapStaleTransfersUseCase;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.time.Instant;

/**
 * Adaptador de infraestructura que dispara periódicamente el reaper de sagas atascadas. Calcula el
 * instante de corte ({@code ahora - timeout}) y delega la recuperación en el caso de uso; la lógica
 * de negocio (qué hacer con cada estado) vive en la aplicación.
 */
@Component
public class SagaReaperScheduler {

    private static final Logger log = LoggerFactory.getLogger(SagaReaperScheduler.class);

    private final ReapStaleTransfersUseCase reaper;
    private final Duration timeout;

    public SagaReaperScheduler(ReapStaleTransfersUseCase reaper,
                               @Value("${hexabank.saga.timeout:PT30S}") Duration timeout) {
        this.reaper = reaper;
        this.timeout = timeout;
    }

    @Scheduled(fixedDelayString = "${hexabank.saga.reaper.poll-interval-ms:5000}")
    public void reapStaleTransfers() {
        int reaped = reaper.reapStaleTransfers(Instant.now().minus(timeout));
        if (reaped > 0) {
            log.info("Reaper: {} saga(s) atascada(s) recuperada(s) (timeout {})", reaped, timeout);
        }
    }
}
