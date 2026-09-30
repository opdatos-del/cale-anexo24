package com.jovycandy.anexo24.catalogs.imports.domain.model;

import java.util.List;

/** Resultado validado de una carga de materiales o productos. */
public record CatalogImportArchivo(CatalogImportType tipo, String nombre, String hash,
                                   String versionContrato, List<String> columnas,
                                   List<CatalogImportFila> filas, List<CatalogImportError> errores,
                                   boolean fallida) {
    public int totalFilas() { return filas.size(); }

    public int filasInvalidas() {
        if (errores.stream().anyMatch(error -> error.fila() == null)) return totalFilas();
        return (int) errores.stream().filter(error -> error.fila() != null)
                .map(error -> (error.hoja() == null ? "" : error.hoja()) + "\u0000" + error.fila())
                .distinct().count();
    }

    public int filasValidas() { return Math.max(0, totalFilas() - filasInvalidas()); }
}
