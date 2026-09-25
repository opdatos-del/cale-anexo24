package com.jovycandy.anexo24.billing.domain.model;

import java.util.List;
import java.util.Map;

/** Datos persistidos necesarios para recuperar una carga después de reiniciar. */
public record CargaFacturacionDetalle(long id, String archivo, String hash, String estado,
                                      int totalRegistros, int registrosValidos, int registrosInvalidos,
                                      List<Fila> filas, List<ArchivoFacturacion.Error> errores) {
    public record Fila(String hoja, int numero, Map<String, String> datos) {}
}
