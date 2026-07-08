package com.hexabank.transfer.config;

import com.hexabank.transfer.application.port.out.AccountOwnershipPort;
import com.hexabank.transfer.application.port.out.EventPublisherPort;
import com.hexabank.transfer.application.port.out.IdempotencyStorePort;
import com.hexabank.transfer.application.port.out.LoadTransferPort;
import com.hexabank.transfer.application.port.out.ProcessedEventPort;
import com.hexabank.transfer.application.port.out.SaveTransferPort;
import com.hexabank.transfer.application.port.out.SearchTransfersPort;
import com.hexabank.transfer.application.port.out.StaleTransferPort;
import com.hexabank.transfer.application.port.out.TransferMetricsPort;
import com.hexabank.transfer.application.service.AccountOwnershipService;
import com.hexabank.transfer.application.service.TransferService;
import com.hexabank.transfer.infrastructure.metrics.MicrometerTransferMetricsAdapter;
import io.micrometer.core.instrument.MeterRegistry;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Cableado de la capa de aplicación: registra el orquestador de la saga como bean inyectándole los
 * puertos de salida.
 */
@Configuration
public class BeanConfiguration {

    @Bean
    TransferService transferService(LoadTransferPort loadTransferPort,
                                    SaveTransferPort saveTransferPort,
                                    EventPublisherPort eventPublisherPort,
                                    ProcessedEventPort processedEventPort,
                                    TransferMetricsPort transferMetricsPort,
                                    AccountOwnershipPort accountOwnershipPort,
                                    SearchTransfersPort searchTransfersPort,
                                    IdempotencyStorePort idempotencyStorePort,
                                    StaleTransferPort staleTransferPort) {
        return new TransferService(loadTransferPort, saveTransferPort, eventPublisherPort,
                processedEventPort, transferMetricsPort, accountOwnershipPort, searchTransfersPort,
                idempotencyStorePort, staleTransferPort);
    }

    /** Proyección local de propietarios de cuenta (consumida de account-events). */
    @Bean
    AccountOwnershipService accountOwnershipService(AccountOwnershipPort accountOwnershipPort,
                                                    ProcessedEventPort processedEventPort) {
        return new AccountOwnershipService(accountOwnershipPort, processedEventPort);
    }

    /**
     * Adaptador de métricas de negocio sobre el {@link MeterRegistry} que autoconfigura Spring Boot
     * (Actuator + Micrometer Prometheus). Se cablea explícitamente, igual que el resto de puertos out.
     */
    @Bean
    TransferMetricsPort transferMetricsPort(MeterRegistry meterRegistry) {
        return new MicrometerTransferMetricsAdapter(meterRegistry);
    }
}
