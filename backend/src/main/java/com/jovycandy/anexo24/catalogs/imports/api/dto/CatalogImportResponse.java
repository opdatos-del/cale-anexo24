package com.jovycandy.anexo24.catalogs.imports.api.dto;

import com.jovycandy.anexo24.catalogs.imports.domain.model.CatalogImportDetalle;

import java.util.List;
import java.util.Map;

/** Respuesta común de preview; los endpoints de material y producto siguen separados. */
public record CatalogImportResponse(long id, String tipo, String archivo, String hash, String estado,
                                    int totalFilas, int filasValidas, int filasInvalidas,
                                    List<String> columnas, List<Map<String, String>> filas,
                                    long totalPersistido, List<Error> errores) {
    public record Error(String hoja, Integer fila, String columna, String valorEnmascarado,
                        String codigo, String mensaje) { }

    public static CatalogImportResponse from(CatalogImportDetalle detail, int previewLimit) {
        List<Map<String, String>> rows = detail.filas().stream().limit(previewLimit)
                .map(row -> row.datos()).toList();
        return new CatalogImportResponse(detail.id(), detail.tipo().name(), detail.archivo(), detail.hash(),
                detail.estado(), detail.totalFilas(), detail.filasValidas(), detail.filasInvalidas(),
                detail.filas().isEmpty() ? List.of() : List.copyOf(detail.filas().getFirst().datos().keySet()),
                rows, detail.totalPersistido(), detail.errores().stream().map(error -> new Error(error.hoja(), error.fila(),
                        error.columna(), error.valorEnmascarado(), error.codigo(), error.mensaje())).toList());
    }
}
