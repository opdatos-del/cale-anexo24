package com.jovycandy.anexo24.catalogs.imports.domain.model;

/** Error estructural o de validación de una fila de catálogo. */
public record CatalogImportError(String hoja, Integer fila, String columna,
                                 String valorEnmascarado, String codigo, String mensaje) {
}
