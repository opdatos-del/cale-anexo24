package com.jovycandy.anexo24.catalogs.imports.domain.model;

import java.time.LocalDateTime;

/**
 * Resultado de confirmar una carga de productos en el catálogo autoritativo.
 *
 * <p>El stage legacy {@code dbo.CARGA_PRODUCTOS} es INSERT-only: los productos con
 * clave existente se ignoran silenciosamente y no se reportan como error; los
 * duplicados internos de la carga sí se contabilizan como errores de negocio.</p>
 */
public record ConfirmacionCargaProducto(long cargaId, String estado, int totalFilas, int filasValidas,
                                        int filasConError, LocalDateTime confirmadaEn, String resultado) {

    /** La carga fue confirmada por el stage legacy. */
    public static final String CONFIRMADO = "CONFIRMED";

    /** El stage legacy encontró errores de negocio que se persistieron en app24. */
    public static final String ERRORES_NEGOCIO = "BUSINESS_ERRORS";
}