package com.hexabank.transfer.application.port.in;

import com.hexabank.transfer.domain.Transfer;

/**
 * Puerto de entrada: caso de uso "solicitar una transferencia".
 *
 * <p>Autorizado por propiedad: un llamante sin rol STAFF solo puede transferir desde una cuenta de
 * la que sea propietario; la denegación es indistinguible de "cuenta inexistente" (404).</p>
 */
public interface RequestTransferUseCase {

    Transfer requestTransfer(RequestTransferCommand command, CallerContext caller);
}
