package com.jovycandy.anexo24.catalogs.imports.application.usecase;

import com.jovycandy.anexo24.catalogs.imports.domain.model.ConfirmacionCargaProducto;
import com.jovycandy.anexo24.catalogs.imports.domain.port.ConfirmacionCargaProductoRepository;
import org.springframework.stereotype.Service;

/** Coordina la confirmación de una carga de productos. */
@Service
public class ConfirmarCargaProductoUseCase {

    private final ConfirmacionCargaProductoRepository repository;

    public ConfirmarCargaProductoUseCase(ConfirmacionCargaProductoRepository repository) {
        this.repository = repository;
    }

    /**
     * Ejecuta el command autoritativo asociado a la carga.
     *
     * @param cargaId identificador de la carga
     * @return resultado de la operación
     */
    public ConfirmacionCargaProducto ejecutar(long cargaId) {
        return repository.confirmar(cargaId);
    }
}