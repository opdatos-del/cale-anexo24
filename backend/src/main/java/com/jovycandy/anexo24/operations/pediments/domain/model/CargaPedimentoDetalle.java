package com.jovycandy.anexo24.operations.pediments.domain.model;

import java.util.List;
import java.util.Map;

/** Estado durable de una carga y una página de sus filas/errores. */
public record CargaPedimentoDetalle(long id, String archivo, String hash, String estado,
                                    int totalFilas, int filasValidas, int filasInvalidas,
                                    String versionPlantilla, String correlationId,
                                    List<Fila> filas, long totalFilasPersistidas,
                                    List<PedimentoError> errores) {
    public record Fila(String hoja, int numero, Map<String, String> datos) {
    }
}
