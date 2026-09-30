package com.jovycandy.anexo24.operations.pediments.domain.model;

import java.util.List;

/** Resultado inmutable de parsear y validar un archivo de pedimentos. */
public record CargaPedimentoArchivo(String nombre, String hash, String versionPlantilla,
                                   List<String> columnas, List<PedimentoFila> filas,
                                   List<PedimentoError> errores, boolean fallida) {
    public int totalFilas() {
        return filas.size();
    }

    public int filasInvalidas() {
        if (errores.stream().anyMatch(error -> error.fila() == null)) return totalFilas();
        return (int) errores.stream()
                .filter(error -> error.fila() != null)
                .map(error -> (error.hoja() == null ? "" : error.hoja()) + "\u0000" + error.fila())
                .distinct()
                .count();
    }

    public int filasValidas() {
        return Math.max(0, totalFilas() - filasInvalidas());
    }
}
