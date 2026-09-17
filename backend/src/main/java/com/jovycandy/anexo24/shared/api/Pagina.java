package com.jovycandy.anexo24.shared.api;

import java.util.List;

/**
 * Página de resultados con metadatos de paginación.
 *
 * @param <T>   tipo de los elementos de la página
 * @param items elementos de la página
 * @param total total de registros que coinciden con la consulta
 * @param pagina número de página devuelta (base 1)
 * @param tamano tamaño de página
 */
public record Pagina<T>(List<T> items, long total, int pagina, int tamano) {

    /**
     * Calcula el total de páginas disponibles.
     *
     * @return total de páginas (al menos 1 si hay registros)
     */
    public long totalPaginas() {
        if (total == 0) return 0;
        return (total + tamano - 1) / tamano;
    }
}