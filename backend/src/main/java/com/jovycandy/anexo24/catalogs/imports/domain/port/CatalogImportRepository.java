package com.jovycandy.anexo24.catalogs.imports.domain.port;

import com.jovycandy.anexo24.catalogs.imports.domain.model.CatalogImportArchivo;
import com.jovycandy.anexo24.catalogs.imports.domain.model.CatalogImportDetalle;
import com.jovycandy.anexo24.catalogs.imports.domain.model.CatalogImportError;
import com.jovycandy.anexo24.catalogs.imports.domain.model.CatalogImportType;

import java.util.List;
import java.util.Optional;

/** Puerto explícito de staging de materiales y productos. */
public interface CatalogImportRepository {
    boolean existsByHash(CatalogImportType type, String hash);
    long save(CatalogImportArchivo archivo, long usuarioId, String correlationId);
    Optional<CatalogImportDetalle> findById(CatalogImportType type, long id, int pagina, int tamano);
    List<CatalogImportError> findErrors(CatalogImportType type, long id, int pagina, int tamano);
}
