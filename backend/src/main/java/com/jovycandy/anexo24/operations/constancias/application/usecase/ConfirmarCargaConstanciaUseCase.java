package com.jovycandy.anexo24.operations.constancias.application.usecase;

import com.jovycandy.anexo24.operations.constancias.domain.model.ConfirmacionCargaConstancia;
import com.jovycandy.anexo24.operations.constancias.domain.port.ConfirmacionCargaConstanciaRepository;
import org.springframework.stereotype.Service;

/** Coordina la confirmación de una carga de constancias. */
@Service
public class ConfirmarCargaConstanciaUseCase {

    private final ConfirmacionCargaConstanciaRepository repository;

    public ConfirmarCargaConstanciaUseCase(ConfirmacionCargaConstanciaRepository repository) {
        this.repository = repository;
    }

    /**
     * Ejecuta el command autoritativo asociado a la carga.
     *
     * @param cargaId identificador de la carga
     * @return resultado de la operación
     */
    public ConfirmacionCargaConstancia ejecutar(long cargaId) {
        return repository.confirmar(cargaId);
    }
}
