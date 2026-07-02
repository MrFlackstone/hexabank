package com.hexabank.transfer.application.port.in;

/**
 * Campos por los que se puede ordenar el listado de transferencias. Enum cerrado = whitelist: el
 * adaptador REST solo puede pedir estos valores, nunca una propiedad arbitraria (evita sorpresas
 * en el ORDER BY generado).
 */
public enum TransferSort {
    CREATED_AT,
    AMOUNT,
    STATUS
}
