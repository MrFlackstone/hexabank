package com.hexabank.transfer.config;

import com.hexabank.transfer.application.port.out.EventPublisherPort;
import com.hexabank.transfer.application.port.out.LoadTransferPort;
import com.hexabank.transfer.application.port.out.ProcessedEventPort;
import com.hexabank.transfer.application.port.out.SaveTransferPort;
import com.hexabank.transfer.application.service.TransferService;
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
                                    ProcessedEventPort processedEventPort) {
        return new TransferService(loadTransferPort, saveTransferPort, eventPublisherPort, processedEventPort);
    }
}
