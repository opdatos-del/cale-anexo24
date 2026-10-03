package com.jovycandy.anexo24.catalogs.imports.application.usecase;

import com.jovycandy.anexo24.catalogs.imports.domain.model.ConfirmacionCargaCliente;
import com.jovycandy.anexo24.catalogs.imports.domain.port.ConfirmacionCargaClienteRepository;
import org.springframework.stereotype.Service;

/** Coordina la confirmación de una carga de clientes. */
@Service
public class ConfirmarCargaClienteUseCase {

    private final ConfirmacionCargaClienteRepository repository;

    public ConfirmarCargaClienteUseCase(ConfirmacionCargaClienteRepository repository) {
        this.repository = repository;
    }

    /**
     * Ejecuta el command autoritativo asociado a la carga.
     *
     * @param cargaId identificador de la carga
     * @return resultado de la operación
     */
    public ConfirmacionCargaCliente ejecutar(long cargaId) {
        return repository.confirmar(cargaId);
    }
}