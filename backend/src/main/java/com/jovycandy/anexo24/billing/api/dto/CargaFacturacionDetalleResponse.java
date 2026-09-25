package com.jovycandy.anexo24.billing.api.dto;

import com.jovycandy.anexo24.billing.domain.model.CargaFacturacionDetalle;

import java.util.List;
import java.util.Map;

public record CargaFacturacionDetalleResponse(long id, String archivo, String hash, String estado,
                                              int totalRegistros, int registrosValidos, int registrosInvalidos,
                                              Preview preview, List<Error> errores) {
    public record Preview(List<Map<String, String>> filas, int pagina, int tamano) {}
    public record Error(String hoja, Integer fila, String columna, String valorEnmascarado, String codigo, String mensaje) {}

    public static CargaFacturacionDetalleResponse from(CargaFacturacionDetalle detail, int pagina, int tamano) {
        return new CargaFacturacionDetalleResponse(detail.id(), detail.archivo(), detail.hash(), detail.estado(),
                detail.totalRegistros(), detail.registrosValidos(), detail.registrosInvalidos(),
                new Preview(detail.filas().stream().map(CargaFacturacionDetalle.Fila::datos).toList(), pagina, tamano),
                detail.errores().stream().map(e -> new Error(e.hoja(), e.fila(), e.columna(), e.valorEnmascarado(), e.codigo(), e.mensaje())).toList());
    }
}
