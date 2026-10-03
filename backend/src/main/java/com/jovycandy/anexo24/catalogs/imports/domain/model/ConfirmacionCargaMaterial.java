package com.jovycandy.anexo24.catalogs.imports.domain.model;

import java.time.LocalDateTime;

/** Resultado de confirmar una carga de materiales en el catálogo autoritativo. */
public record ConfirmacionCargaMaterial(long cargaId, String estado, int totalFilas, int filasValidas,
                                        int filasConError, LocalDateTime confirmadaEn, String resultado) {

    /** La carga fue confirmada por el stage legacy. */
    public static final String CONFIRMADO = "CONFIRMED";

    /** El stage legacy encontró errores de negocio que se persistieron en app24. */
    public static final String ERRORES_NEGOCIO = "BUSINESS_ERRORS";
}
