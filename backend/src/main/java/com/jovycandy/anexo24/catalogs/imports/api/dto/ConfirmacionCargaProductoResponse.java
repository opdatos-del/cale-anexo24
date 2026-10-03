package com.jovycandy.anexo24.catalogs.imports.api.dto;

import com.jovycandy.anexo24.catalogs.imports.domain.model.ConfirmacionCargaProducto;

import java.time.LocalDateTime;

/** Respuesta estable de la confirmación de productos. */
public record ConfirmacionCargaProductoResponse(long cargaId, String estado, int totalFilas, int filasValidas,
                                                int filasConError, LocalDateTime confirmadaEn, String resultado) {

    /**
     * Convierte el resultado de dominio a la representación HTTP.
     *
     * @param confirmacion resultado de dominio
     * @return respuesta HTTP
     */
    public static ConfirmacionCargaProductoResponse from(ConfirmacionCargaProducto confirmacion) {
        return new ConfirmacionCargaProductoResponse(confirmacion.cargaId(), confirmacion.estado(),
                confirmacion.totalFilas(), confirmacion.filasValidas(), confirmacion.filasConError(),
                confirmacion.confirmadaEn(), confirmacion.resultado());
    }
}