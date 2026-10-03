package com.jovycandy.anexo24.catalogs.imports.application.usecase;

import com.jovycandy.anexo24.catalogs.imports.domain.model.ConfirmacionCargaMaterial;
import com.jovycandy.anexo24.catalogs.imports.domain.port.ConfirmacionCargaMaterialRepository;
import org.springframework.stereotype.Service;

/** Coordina la confirmación de una carga de materiales. */
@Service
public class ConfirmarCargaMaterialUseCase {

    private final ConfirmacionCargaMaterialRepository repository;

    public ConfirmarCargaMaterialUseCase(ConfirmacionCargaMaterialRepository repository) {
        this.repository = repository;
    }

    /**
     * Ejecuta el command autoritativo asociado a la carga.
     *
     * @param cargaId identificador de la carga
     * @return resultado de la operación
     */
    public ConfirmacionCargaMaterial ejecutar(long cargaId) {
        return repository.confirmar(cargaId);
    }
}
