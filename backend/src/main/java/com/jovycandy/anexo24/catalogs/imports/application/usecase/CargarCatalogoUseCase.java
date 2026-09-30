package com.jovycandy.anexo24.catalogs.imports.application.usecase;

import com.jovycandy.anexo24.catalogs.imports.domain.model.CatalogImportArchivo;
import com.jovycandy.anexo24.catalogs.imports.domain.port.CatalogImportRepository;
import com.jovycandy.anexo24.shared.exception.RecursoDuplicadoException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Guarda sólo staging y preview de un contrato explícito de catálogo. */
@Service
public class CargarCatalogoUseCase {
    private final CatalogImportRepository repository;

    public CargarCatalogoUseCase(CatalogImportRepository repository) {
        this.repository = repository;
    }

    @Transactional(transactionManager = "appTransactionManager")
    public long ejecutar(CatalogImportArchivo archivo, long usuarioId, String correlationId) {
        if (repository.existsByHash(archivo.tipo(), archivo.hash())) throw new RecursoDuplicadoException();
        return repository.save(archivo, usuarioId, correlationId);
    }
}
