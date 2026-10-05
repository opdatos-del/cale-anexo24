package com.jovycandy.anexo24.catalogs.imports.domain.model;

import java.time.LocalDateTime;

/**
 * Resultado de confirmar una carga de constancias de transferencia (submaquila) en las
 * tablas autoritativas.
 *
 * <p>El stage legacy {@code dbo.CARGA_SUBMAQUILA} es INSERT-only y no valida: inserta
 * {@code dbo.SALIDAS} agrupadas por folio/fecha/submaquilador y una fila de
 * {@code dbo.PSALIDAS} por renglón del stage {@code dbo.TMPSUBMAQUILA}. No existe error
 * stage para este contrato, por lo que todo error del legacy revierte la transacción
 * completa y se propaga como error de la confirmación.</p>
 */
public record ConfirmacionCargaSubmaquila(long cargaId, String estado, int totalFilas, int filasValidas,
                                          int filasConError, LocalDateTime confirmadaEn, String resultado) {

    /** La carga fue confirmada por el stage legacy. */
    public static final String CONFIRMADO = "CONFIRMED";
}
