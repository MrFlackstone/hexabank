package com.hexabank.transfer.application.port.out;

import com.hexabank.shared.events.DomainEvent;

/**
 * Puerto de salida: publica un evento de dominio (comando a account-service o evento terminal de la
 * transferencia). El adaptador elige el topic y la clave según el tipo de evento.
 */
public interface EventPublisherPort {

    void publish(DomainEvent event);
}
