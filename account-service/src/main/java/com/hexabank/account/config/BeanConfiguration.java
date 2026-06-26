package com.hexabank.account.config;

import com.hexabank.account.application.port.out.EventPublisherPort;
import com.hexabank.account.application.port.out.LoadAccountPort;
import com.hexabank.account.application.port.out.ProcessedEventPort;
import com.hexabank.account.application.port.out.SaveAccountPort;
import com.hexabank.account.application.service.AccountCommandService;
import com.hexabank.account.application.service.AccountService;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Cableado de la capa de aplicación: registra los servicios (clases planas) como beans inyectándoles
 * los puertos de salida.
 */
@Configuration
public class BeanConfiguration {

    @Bean
    AccountService accountService(LoadAccountPort loadAccountPort, SaveAccountPort saveAccountPort) {
        return new AccountService(loadAccountPort, saveAccountPort);
    }

    /** Casos de uso de la saga (débito/crédito/reembolso). */
    @Bean
    AccountCommandService accountCommandService(LoadAccountPort loadAccountPort,
                                                SaveAccountPort saveAccountPort,
                                                EventPublisherPort eventPublisherPort,
                                                ProcessedEventPort processedEventPort) {
        return new AccountCommandService(loadAccountPort, saveAccountPort, eventPublisherPort, processedEventPort);
    }
}
