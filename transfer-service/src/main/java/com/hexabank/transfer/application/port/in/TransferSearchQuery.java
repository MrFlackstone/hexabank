package com.hexabank.transfer.application.port.in;

import com.hexabank.transfer.domain.TransferStatus;

import java.time.OffsetDateTime;

/**
 * Consulta paginada de transferencias, ya validada por el adaptador de entrada. Todos los filtros
 * son opcionales (nulos = sin filtro); la paginación y el orden siempre están definidos.
 */
public record TransferSearchQuery(TransferStatus status,
                                  OffsetDateTime from,
                                  OffsetDateTime to,
                                  int page,
                                  int size,
                                  TransferSort sortBy,
                                  boolean ascending) {

    public TransferSearchQuery {
        if (page < 0) {
            throw new IllegalArgumentException("La página no puede ser negativa");
        }
        if (size < 1) {
            throw new IllegalArgumentException("El tamaño de página debe ser al menos 1");
        }
    }
}
