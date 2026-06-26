package com.hexabank.account.application.port.out;

import com.hexabank.shared.events.DomainEvent;

/**
 * Puerto de salida: publica un evento de dominio resultado de procesar un comando.
 */
public interface EventPublisherPort {

    void publish(DomainEvent event);
}
