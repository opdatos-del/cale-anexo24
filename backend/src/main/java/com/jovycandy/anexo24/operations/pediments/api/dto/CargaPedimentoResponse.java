package com.jovycandy.anexo24.operations.pediments.api.dto;

import com.jovycandy.anexo24.operations.pediments.domain.model.CargaPedimentoDetalle;
import com.jovycandy.anexo24.operations.pediments.domain.model.PedimentoError;

import java.util.List;
import java.util.Map;

/** Respuesta de carga/consulta sin habilitar confirmación operativa. */
public record CargaPedimentoResponse(long id, String archivo, String hash, String estado,
                                     int totalFilas, int filasValidas, int filasInvalidas,
                                     String versionPlantilla, String correlationId,
                                     Preview preview, List<Error> errores) {
    public record Preview(List<String> columnas, List<Map<String, String>> filas, int pagina, int tamano,
                          long totalFilas) {
    }

    public record Error(String hoja, Integer fila, String columna, String valorEnmascarado,
                        String codigo, String mensaje) {
    }

    public static CargaPedimentoResponse from(CargaPedimentoDetalle detail, List<String> columnas,
                                               int pagina, int tamano) {
        return new CargaPedimentoResponse(detail.id(), detail.archivo(), detail.hash(), detail.estado(),
                detail.totalFilas(), detail.filasValidas(), detail.filasInvalidas(), detail.versionPlantilla(),
                detail.correlationId(), new Preview(columnas,
                detail.filas().stream().map(CargaPedimentoDetalle.Fila::datos).toList(), pagina, tamano,
                detail.totalFilasPersistidas()), detail.errores().stream().map(CargaPedimentoResponse::from).toList());
    }

    public static Error from(PedimentoError error) {
        return new Error(error.hoja(), error.fila(), error.columna(), error.valorEnmascarado(), error.codigo(), error.mensaje());
    }
}
