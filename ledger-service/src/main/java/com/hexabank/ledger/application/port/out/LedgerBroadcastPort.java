package com.hexabank.ledger.application.port.out;

import com.hexabank.ledger.domain.LedgerEntry;

/**
 * Puerto de salida para empujar asientos recién registrados a los suscriptores en vivo (SSE).
 *
 * <p>Modelar la difusión como puerto mantiene la capa de aplicación ajena al mecanismo de transporte:
 * el adaptador SSE de infraestructura es solo una implementación más, sustituible (p. ej. por WebSocket)
 * sin tocar el núcleo.</p>
 */
public interface LedgerBroadcastPort {

    /**
     * Difunde el asiento a los suscriptores autorizados: el propietario de la cuenta afectada y STAFF.
     * La aplicación resuelve {@code ownerId} (puede ser nulo si la proyección no conoce la cuenta:
     * entonces solo STAFF lo recibe); el adaptador no toca persistencia.
     */
    void broadcast(LedgerEntry entry, String ownerId);
}
