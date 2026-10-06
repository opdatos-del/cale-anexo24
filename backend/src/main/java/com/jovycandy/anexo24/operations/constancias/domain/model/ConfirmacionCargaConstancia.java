package com.jovycandy.anexo24.operations.constancias.domain.model;

import java.time.LocalDateTime;

/**
 * Resultado de confirmar una carga de constancias reutilizando el SP legacy
 * {@code dbo.CARGACONSTANCIAS}. El wrapper versionado {@code dbo.APP24_C_CONSTANCIA_CARGA_CONFIRMAR}
 * deja las filas autoritativas en {@code dbo.SALIDAS} / {@code dbo.PSALIDAS} / {@code dbo.DIRIGIDO}.
 */
public record ConfirmacionCargaConstancia(long cargaId, String estado, int totalFilas, int filasValidas,
                                    int filasConError, LocalDateTime confirmadaEn, String resultado) {

    /** La carga fue confirmada por el stage legacy. */
    public static final String CONFIRMADO = "CONFIRMED";
}
