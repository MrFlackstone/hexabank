package com.hexabank.transfer.application.port.in;

import com.hexabank.transfer.domain.Transfer;

/**
 * Puerto de entrada: caso de uso "listar transferencias con filtros, orden y paginación server-side".
 *
 * <p>La visibilidad es un predicado obligatorio de la consulta: un llamante sin rol STAFF solo ve
 * transferencias que involucran (origen o destino) cuentas de su propiedad; STAFF las ve todas.
 * El servidor pagina, ordena y filtra en SQL y devuelve solo la página pedida.</p>
 */
public interface SearchTransfersUseCase {

    PageResult<Transfer> searchTransfers(TransferSearchQuery query, CallerContext caller);
}
