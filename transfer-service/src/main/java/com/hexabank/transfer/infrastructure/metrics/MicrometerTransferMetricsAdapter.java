package com.hexabank.transfer.infrastructure.metrics;

import com.hexabank.transfer.application.port.out.TransferMetricsPort;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;

/**
 * Adaptador de salida de métricas sobre Micrometer. Registra dos contadores monótonos que el
 * registry de Prometheus expone en {@code /actuator/prometheus}:
 * {@code hexabank_transfers_completed_total} y {@code hexabank_transfers_failed_total}. Permiten
 * calcular el throughput de transferencias con {@code rate(...)} en Grafana.
 */
public class MicrometerTransferMetricsAdapter implements TransferMetricsPort {

    private final Counter completedCounter;
    private final Counter failedCounter;
    private final Counter reapedCounter;

    public MicrometerTransferMetricsAdapter(MeterRegistry meterRegistry) {
        this.completedCounter = Counter.builder("hexabank.transfers.completed")
                .description("Transferencias que han llegado a estado COMPLETED")
                .register(meterRegistry);
        this.failedCounter = Counter.builder("hexabank.transfers.failed")
                .description("Transferencias que han terminado en FAILED")
                .register(meterRegistry);
        this.reapedCounter = Counter.builder("hexabank.transfers.reaped")
                .description("Sagas atascadas recuperadas por el reaper (timeout)")
                .register(meterRegistry);
    }

    @Override
    public void transferCompleted() {
        completedCounter.increment();
    }

    @Override
    public void transferFailed() {
        failedCounter.increment();
    }

    @Override
    public void sagaReaped() {
        reapedCounter.increment();
    }
}
