package com.jovycandy.anexo24.operations.pediments.api.dto;

import com.jovycandy.anexo24.operations.pediments.domain.model.PedimentoError;

import java.util.List;

/** Página de errores de validación de una carga. */
public record PedimentoErrorsResponse(long cargaId, int pagina, int tamano, List<CargaPedimentoResponse.Error> errores) {
    public static PedimentoErrorsResponse from(long cargaId, int pagina, int tamano, List<PedimentoError> errors) {
        return new PedimentoErrorsResponse(cargaId, pagina, tamano, errors.stream().map(CargaPedimentoResponse::from).toList());
    }
}
