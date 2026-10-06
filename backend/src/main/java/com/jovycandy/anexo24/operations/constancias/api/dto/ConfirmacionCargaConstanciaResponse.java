package com.jovycandy.anexo24.operations.constancias.api.dto;

import com.jovycandy.anexo24.operations.constancias.domain.model.ConfirmacionCargaConstancia;

import java.time.LocalDateTime;

/** Respuesta estable de la confirmacion de constancias. */
public record ConfirmacionCargaConstanciaResponse(long cargaId, String estado, int totalFilas, int filasValidas,
                                            int filasConError, LocalDateTime confirmadaEn, String resultado) {

    /**
     * Convierte el resultado de dominio a la representacion HTTP.
     *
     * @param confirmacion resultado de dominio
     * @return respuesta HTTP
     */
    public static ConfirmacionCargaConstanciaResponse from(ConfirmacionCargaConstancia confirmacion) {
        return new ConfirmacionCargaConstanciaResponse(confirmacion.cargaId(), confirmacion.estado(),
                confirmacion.totalFilas(), confirmacion.filasValidas(), confirmacion.filasConError(),
                confirmacion.confirmadaEn(), confirmacion.resultado());
    }
}
