package com.jovycandy.anexo24.catalogs.imports.application.usecase;

import com.jovycandy.anexo24.catalogs.imports.domain.model.ConfirmacionCargaAgente;
import com.jovycandy.anexo24.catalogs.imports.domain.port.ConfirmacionCargaAgenteRepository;
import org.springframework.stereotype.Service;

/** Coordina la confirmación de una carga de agentes aduanales. */
@Service
public class ConfirmarCargaAgenteUseCase {

    private final ConfirmacionCargaAgenteRepository repository;

    public ConfirmarCargaAgenteUseCase(ConfirmacionCargaAgenteRepository repository) {
        this.repository = repository;
    }

    /**
     * Ejecuta el command autoritativo asociado a la carga.
     *
     * @param cargaId identificador de la carga
     * @return resultado de la operación
     */
    public ConfirmacionCargaAgente ejecutar(long cargaId) {
        return repository.confirmar(cargaId);
    }
}