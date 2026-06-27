package com.hexabank.ledger.config;

import com.hexabank.ledger.application.port.out.LedgerBroadcastPort;
import com.hexabank.ledger.application.port.out.LedgerEntryRepositoryPort;
import com.hexabank.ledger.application.port.out.ProcessedEventPort;
import com.hexabank.ledger.application.port.out.TransferViewRepositoryPort;
import com.hexabank.ledger.application.service.LedgerService;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Cableado de la capa de aplicación: registra {@link LedgerService} como bean inyectándole los puertos
 * de salida. Un único bean satisface los tres puertos de entrada (ingesta, consulta y proyección), que
 * la clase implementa.
 */
@Configuration
public class BeanConfiguration {

    @Bean
    LedgerService ledgerService(LedgerEntryRepositoryPort ledgerEntryRepositoryPort,
                                TransferViewRepositoryPort transferViewRepositoryPort,
                                ProcessedEventPort processedEventPort,
                                LedgerBroadcastPort ledgerBroadcastPort) {
        return new LedgerService(ledgerEntryRepositoryPort, transferViewRepositoryPort,
                processedEventPort, ledgerBroadcastPort);
    }
}
