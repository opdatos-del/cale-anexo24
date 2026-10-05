package com.jovycandy.anexo24.catalogs.imports.api.dto;

import com.jovycandy.anexo24.catalogs.imports.domain.model.ConfirmacionCargaSubmaquila;

import java.time.LocalDateTime;

/** Respuesta estable de la confirmación de constancias de transferencia. */
public record ConfirmacionCargaSubmaquilaResponse(long cargaId, String estado, int totalFilas, int filasValidas,
                                                  int filasConError, LocalDateTime confirmadaEn, String resultado) {

    /**
     * Convierte el resultado de dominio a la representación HTTP.
     *
     * @param confirmacion resultado de dominio
     * @return respuesta HTTP
     */
    public static ConfirmacionCargaSubmaquilaResponse from(ConfirmacionCargaSubmaquila confirmacion) {
        return new ConfirmacionCargaSubmaquilaResponse(confirmacion.cargaId(), confirmacion.estado(),
                confirmacion.totalFilas(), confirmacion.filasValidas(), confirmacion.filasConError(),
                confirmacion.confirmadaEn(), confirmacion.resultado());
    }
}
