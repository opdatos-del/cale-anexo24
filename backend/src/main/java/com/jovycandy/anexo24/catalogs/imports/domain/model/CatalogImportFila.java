package com.jovycandy.anexo24.catalogs.imports.domain.model;

import java.util.Map;

/** Fila normalizada que se conserva en staging app24. */
public record CatalogImportFila(String hoja, int numero, Map<String, String> datos) {
}
