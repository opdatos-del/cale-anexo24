package com.jovycandy.anexo24.operations.actas.api.dto;

import com.jovycandy.anexo24.operations.actas.domain.model.ConfirmacionCargaActa;

import java.time.LocalDateTime;

/** Respuesta estable de la confirmacion de actas. */
public record ConfirmacionCargaActaResponse(long cargaId, String estado, int totalFilas, int filasValidas,
                                            int filasConError, LocalDateTime confirmadaEn, String resultado) {

    /**
     * Convierte el resultado de dominio a la representacion HTTP.
     *
     * @param confirmacion resultado de dominio
     * @return respuesta HTTP
     */
    public static ConfirmacionCargaActaResponse from(ConfirmacionCargaActa confirmacion) {
        return new ConfirmacionCargaActaResponse(confirmacion.cargaId(), confirmacion.estado(),
                confirmacion.totalFilas(), confirmacion.filasValidas(), confirmacion.filasConError(),
                confirmacion.confirmadaEn(), confirmacion.resultado());
    }
}
