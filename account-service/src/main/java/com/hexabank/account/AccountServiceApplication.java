package com.hexabank.account;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * Punto de arranque del microservicio account-service.
 *
 * <p>Al estar en el paquete raíz {@code com.hexabank.account}, el escaneo de componentes cubre las
 * tres capas (domain/application/infrastructure) sin configuración extra.</p>
 */
@SpringBootApplication
public class AccountServiceApplication {

    public static void main(String[] args) {
        SpringApplication.run(AccountServiceApplication.class, args);
    }
}
