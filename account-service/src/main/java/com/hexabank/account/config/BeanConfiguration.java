package com.hexabank.account.config;

import com.hexabank.account.application.port.out.LoadAccountPort;
import com.hexabank.account.application.port.out.SaveAccountPort;
import com.hexabank.account.application.service.AccountService;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Cableado explícito de la capa de aplicación con Spring.
 *
 * <p>El servicio de aplicación es una clase plana (sin {@code @Service}) para mantener la capa libre
 * de framework. Aquí, en la frontera de infraestructura, se construye como bean inyectándole los
 * adaptadores de salida (que Spring descubre por sus puertos). Es la materialización del principio
 * de inversión de dependencias: el framework sirve al núcleo, no al revés.</p>
 */
@Configuration
public class BeanConfiguration {

    @Bean
    AccountService accountService(LoadAccountPort loadAccountPort, SaveAccountPort saveAccountPort) {
        return new AccountService(loadAccountPort, saveAccountPort);
    }
}
