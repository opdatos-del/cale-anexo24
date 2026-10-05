package com.jovycandy.anexo24.catalogs.imports.application.usecase;

import com.jovycandy.anexo24.catalogs.imports.domain.model.ConfirmacionCargaSubmaquila;
import com.jovycandy.anexo24.catalogs.imports.domain.port.ConfirmacionCargaSubmaquilaRepository;
import org.springframework.stereotype.Service;

/** Coordina la confirmación de una carga de constancias de transferencia. */
@Service
public class ConfirmarCargaSubmaquilaUseCase {

    private final ConfirmacionCargaSubmaquilaRepository repository;

    public ConfirmarCargaSubmaquilaUseCase(ConfirmacionCargaSubmaquilaRepository repository) {
        this.repository = repository;
    }

    /**
     * Ejecuta el command autoritativo asociado a la carga.
     *
     * @param cargaId identificador de la carga
     * @return resultado de la operación
     */
    public ConfirmacionCargaSubmaquila ejecutar(long cargaId) {
        return repository.confirmar(cargaId);
    }
}
