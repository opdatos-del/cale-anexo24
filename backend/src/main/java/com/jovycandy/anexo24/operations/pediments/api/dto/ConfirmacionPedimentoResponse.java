package com.jovycandy.anexo24.operations.pediments.api.dto;

import com.jovycandy.anexo24.operations.pediments.domain.model.ConfirmacionPedimento;

import java.time.LocalDateTime;

/** Respuesta de la confirmación autoritativa de una carga de pedimentos. */
public record ConfirmacionPedimentoResponse(long cargaId, String estado, String resultado,
                                            Integer tipoOperacion, int operacionesProcesadas,
                                            int partidasProcesadas, LocalDateTime fechaConfirmacion) {

    /**
     * Convierte el resultado de dominio a DTO de API.
     *
     * @param confirmacion resultado de dominio
     * @return DTO de respuesta
     */
    public static ConfirmacionPedimentoResponse from(ConfirmacionPedimento confirmacion) {
        return new ConfirmacionPedimentoResponse(confirmacion.cargaId(), confirmacion.estado(),
                confirmacion.resultado(), confirmacion.tipoOperacion(), confirmacion.operacionesProcesadas(),
                confirmacion.partidasProcesadas(), confirmacion.fechaConfirmacion());
    }
}
