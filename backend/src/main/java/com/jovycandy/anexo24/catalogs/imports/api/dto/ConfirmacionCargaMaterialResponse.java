package com.jovycandy.anexo24.catalogs.imports.api.dto;

import com.jovycandy.anexo24.catalogs.imports.domain.model.ConfirmacionCargaMaterial;

import java.time.LocalDateTime;

/** Respuesta estable de la confirmación de materiales. */
public record ConfirmacionCargaMaterialResponse(long cargaId, String estado, int totalFilas, int filasValidas,
                                                int filasConError, LocalDateTime confirmadaEn, String resultado) {

    /**
     * Convierte el resultado de dominio a la representación HTTP.
     *
     * @param confirmacion resultado de dominio
     * @return respuesta HTTP
     */
    public static ConfirmacionCargaMaterialResponse from(ConfirmacionCargaMaterial confirmacion) {
        return new ConfirmacionCargaMaterialResponse(confirmacion.cargaId(), confirmacion.estado(),
                confirmacion.totalFilas(), confirmacion.filasValidas(), confirmacion.filasConError(),
                confirmacion.confirmadaEn(), confirmacion.resultado());
    }
}
