package com.jovycandy.anexo24.billing.api.dto;

import java.util.List;
import java.util.Map;

public record CargaFacturacionResponse(List<Carga> cargas, String correlationId, String plantilla,
                                      boolean confirmacionDisponible) {
    public record Carga(long id, String archivo, String hash, String estado, int totalRegistros,
                        int registrosValidos, int registrosInvalidos, Preview preview,
                        List<Error> errores) {}
    public record Preview(List<String> columnas, List<Map<String, String>> filas) {}
    public record Error(String hoja, Integer fila, String columna, String valorEnmascarado,
                        String codigo, String mensaje) {}
}
