package com.jovycandy.anexo24.operations.actas.domain.model;

import java.time.LocalDateTime;

/**
 * Resultado de confirmar una carga de actas reutilizando el SP legacy
 * {@code dbo.CARGAACTAS}. El wrapper versionado {@code dbo.APP24_C_ACTA_CARGA_CONFIRMAR}
 * deja las filas autoritativas en {@code dbo.SALIDAS} / {@code dbo.PSALIDAS} / {@code dbo.DIRIGIDO}.
 */
public record ConfirmacionCargaActa(long cargaId, String estado, int totalFilas, int filasValidas,
                                    int filasConError, LocalDateTime confirmadaEn, String resultado) {

    /** La carga fue confirmada por el stage legacy. */
    public static final String CONFIRMADO = "CONFIRMED";
}
