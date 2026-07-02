package com.hexabank.transfer.application.port.in;

import java.util.List;

/**
 * Página de resultados propia de la aplicación: contenido más los metadatos que el cliente necesita
 * para paginar ({@code totalElements}, {@code totalPages}, página y tamaño actuales).
 *
 * <p>Abstracción deliberadamente plana para no filtrar {@code Pageable}/{@code Page} de Spring Data
 * a través de los puertos: la capa de aplicación permanece libre de framework.</p>
 */
public record PageResult<T>(List<T> content, int page, int size, long totalElements, int totalPages) {

    /** Página vacía (p. ej. un USER sin cuentas: no hay nada que consultar en BD). */
    public static <T> PageResult<T> empty(int page, int size) {
        return new PageResult<>(List.of(), page, size, 0L, 0);
    }
}
