package com.jovycandy.anexo24.catalogs.imports.domain.model;

import java.util.List;
import java.util.Map;

/** Preview persistida de una carga de catálogo. */
public record CatalogImportDetalle(long id, CatalogImportType tipo, String archivo, String hash,
                                  String estado, int totalFilas, int filasValidas,
                                  int filasInvalidas, String versionContrato,
                                  List<CatalogImportFila> filas, long totalPersistido,
                                  List<CatalogImportError> errores) {
}
