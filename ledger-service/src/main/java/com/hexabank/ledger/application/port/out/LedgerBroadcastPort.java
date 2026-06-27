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

    void broadcast(LedgerEntry entry);
}
