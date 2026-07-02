package com.hexabank.transfer.application.port.out;

import com.hexabank.transfer.application.port.in.PageResult;
import com.hexabank.transfer.domain.Transfer;

/**
 * Puerto de salida: búsqueda paginada de transferencias. La implementación traduce los criterios a la
 * query SQL (filtros + orden + página) y devuelve únicamente la página solicitada con sus metadatos.
 */
public interface SearchTransfersPort {

    PageResult<Transfer> search(TransferSearchCriteria criteria);
}
