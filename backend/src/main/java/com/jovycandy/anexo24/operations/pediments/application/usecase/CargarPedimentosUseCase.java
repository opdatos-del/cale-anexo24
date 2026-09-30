package com.jovycandy.anexo24.operations.pediments.application.usecase;

import com.jovycandy.anexo24.operations.pediments.domain.model.CargaPedimentoArchivo;
import com.jovycandy.anexo24.operations.pediments.domain.port.CargaPedimentoRepository;
import com.jovycandy.anexo24.shared.exception.RecursoDuplicadoException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Persiste únicamente staging y resultados de validación, sin confirmación operativa. */
@Service
public class CargarPedimentosUseCase {
    private final CargaPedimentoRepository repository;

    public CargarPedimentosUseCase(CargaPedimentoRepository repository) {
        this.repository = repository;
    }

    /**
     * Guarda una carga si su fingerprint no está activo previamente.
     *
     * @param archivo resultado del parser
     * @param usuarioId usuario autenticado
     * @param correlationId correlación de la solicitud
     * @return identificador durable de la carga
     */
    @Transactional(transactionManager = "appTransactionManager")
    public long ejecutar(CargaPedimentoArchivo archivo, long usuarioId, String correlationId) {
        if (repository.existsByHash(archivo.hash())) throw new RecursoDuplicadoException();
        return repository.save(archivo, usuarioId, correlationId);
    }
}
