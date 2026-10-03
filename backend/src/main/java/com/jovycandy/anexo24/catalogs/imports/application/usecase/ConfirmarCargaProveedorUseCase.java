package com.jovycandy.anexo24.catalogs.imports.application.usecase;

import com.jovycandy.anexo24.catalogs.imports.domain.model.ConfirmacionCargaProveedor;
import com.jovycandy.anexo24.catalogs.imports.domain.port.ConfirmacionCargaProveedorRepository;
import org.springframework.stereotype.Service;

/** Coordina la confirmación de una carga de proveedores. */
@Service
public class ConfirmarCargaProveedorUseCase {

    private final ConfirmacionCargaProveedorRepository repository;

    public ConfirmarCargaProveedorUseCase(ConfirmacionCargaProveedorRepository repository) {
        this.repository = repository;
    }

    /**
     * Ejecuta el command autoritativo asociado a la carga.
     *
     * @param cargaId identificador de la carga
     * @return resultado de la operación
     */
    public ConfirmacionCargaProveedor ejecutar(long cargaId) {
        return repository.confirmar(cargaId);
    }
}