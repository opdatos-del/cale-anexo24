package com.jovycandy.anexo24.billing.domain.model;

import java.util.List;

public record ArchivoFacturacion(String nombre, String hash, int hojas, int filas,
                                 List<String> columnas, List<Fila> preview,
                                 List<Fila> filasNormalizadas, List<Error> errores,
                                 boolean fallida) {
    public ArchivoFacturacion(String nombre, String hash, int hojas, int filas,
                              List<String> columnas, List<Fila> preview,
                              List<Error> errores, boolean fallida) {
        this(nombre, hash, hojas, filas, columnas, preview, preview, errores, fallida);
    }

    public record Fila(String hoja, int numero, List<String> celdas) {}
    public record Error(String hoja, Integer fila, String columna, String valorEnmascarado,
                        String codigo, String mensaje) {}
}
