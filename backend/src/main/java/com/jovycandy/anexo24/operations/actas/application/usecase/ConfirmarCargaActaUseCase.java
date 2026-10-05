package com.jovycandy.anexo24.operations.actas.application.usecase;

import com.jovycandy.anexo24.operations.actas.domain.model.ConfirmacionCargaActa;
import com.jovycandy.anexo24.operations.actas.domain.port.ConfirmacionCargaActaRepository;
import org.springframework.stereotype.Service;

/** Coordina la confirmación de una carga de actas. */
@Service
public class ConfirmarCargaActaUseCase {

    private final ConfirmacionCargaActaRepository repository;

    public ConfirmarCargaActaUseCase(ConfirmacionCargaActaRepository repository) {
        this.repository = repository;
    }

    /**
     * Ejecuta el command autoritativo asociado a la carga.
     *
     * @param cargaId identificador de la carga
     * @return resultado de la operación
     */
    public ConfirmacionCargaActa ejecutar(long cargaId) {
        return repository.confirmar(cargaId);
    }
}
