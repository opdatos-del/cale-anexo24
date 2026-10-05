package com.jovycandy.anexo24.catalogs.imports.domain.model;

import java.time.LocalDateTime;

/**
 * Resultado de confirmar una carga de agentes aduanales en el catálogo autoritativo.
 *
 * <p>El stage legacy {@code dbo.CARGAAgentes} es INSERT-only: los agentes con clave
 * existente en {@code dbo.agentes} se omiten silenciosamente y no se reportan como error;
 * los duplicados internos de la carga sí se contabilizan como errores de negocio.</p>
 */
public record ConfirmacionCargaAgente(long cargaId, String estado, int totalFilas, int filasValidas,
                                      int filasConError, LocalDateTime confirmadaEn, String resultado) {

    /** La carga fue confirmada por el stage legacy. */
    public static final String CONFIRMADO = "CONFIRMED";

    /** El stage legacy encontró errores de negocio que se persistieron en app24. */
    public static final String ERRORES_NEGOCIO = "BUSINESS_ERRORS";
}