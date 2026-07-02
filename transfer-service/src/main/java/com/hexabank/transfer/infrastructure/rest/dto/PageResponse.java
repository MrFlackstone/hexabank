package com.hexabank.transfer.infrastructure.rest.dto;

import com.hexabank.transfer.application.port.in.PageResult;

import java.util.List;
import java.util.function.Function;

/**
 * DTO de salida de un listado paginado: contenido + metadatos para que el cliente pinte la paginación
 * sin cargar nunca el dataset completo.
 */
public record PageResponse<T>(List<T> content, int page, int size, long totalElements, int totalPages) {

    public static <S, T> PageResponse<T> from(PageResult<S> result, Function<S, T> mapper) {
        return new PageResponse<>(result.content().stream().map(mapper).toList(),
                result.page(), result.size(), result.totalElements(), result.totalPages());
    }
}
