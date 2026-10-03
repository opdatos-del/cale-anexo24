package com.jovycandy.anexo24.catalogs.imports.api.dto;

import com.jovycandy.anexo24.catalogs.imports.domain.model.ConfirmacionCargaProveedor;

import java.time.LocalDateTime;

/** Respuesta estable de la confirmación de proveedores. */
public record ConfirmacionCargaProveedorResponse(long cargaId, String estado, int totalFilas, int filasValidas,
                                                 int filasConError, LocalDateTime confirmadaEn, String resultado) {

    /**
     * Convierte el resultado de dominio a la representación HTTP.
     *
     * @param confirmacion resultado de dominio
     * @return respuesta HTTP
     */
    public static ConfirmacionCargaProveedorResponse from(ConfirmacionCargaProveedor confirmacion) {
        return new ConfirmacionCargaProveedorResponse(confirmacion.cargaId(), confirmacion.estado(),
                confirmacion.totalFilas(), confirmacion.filasValidas(), confirmacion.filasConError(),
                confirmacion.confirmadaEn(), confirmacion.resultado());
    }
}