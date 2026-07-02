package com.hexabank.transfer.application.port.out;

import com.hexabank.transfer.application.port.in.TransferSort;
import com.hexabank.transfer.domain.TransferStatus;

import java.time.OffsetDateTime;
import java.util.Set;
import java.util.UUID;

/**
 * Criterios de búsqueda que la aplicación entrega al puerto de persistencia. {@code visibleAccountIds}
 * es el predicado de visibilidad: si es nulo no se filtra (STAFF); si tiene valores, solo se devuelven
 * transferencias cuyo origen o destino esté en el conjunto.
 */
public record TransferSearchCriteria(TransferStatus status,
                                     OffsetDateTime from,
                                     OffsetDateTime to,
                                     Set<UUID> visibleAccountIds,
                                     int page,
                                     int size,
                                     TransferSort sortBy,
                                     boolean ascending) {
}
