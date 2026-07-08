package com.hexabank.transfer;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

/**
 * Punto de arranque del microservicio transfer-service.
 *
 * <p>{@link EnableScheduling} habilita las tareas periódicas: el relay de la outbox y el reaper de
 * sagas atascadas.</p>
 */
@SpringBootApplication
@EnableScheduling
public class TransferServiceApplication {

    public static void main(String[] args) {
        SpringApplication.run(TransferServiceApplication.class, args);
    }
}
