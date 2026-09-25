package com.jovycandy.anexo24.billing.application.usecase;

import com.jovycandy.anexo24.billing.domain.model.ArchivoFacturacion;
import com.jovycandy.anexo24.billing.domain.port.CargaFacturacionRepository;
import com.jovycandy.anexo24.shared.exception.RecursoDuplicadoException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashSet;
import java.util.List;

@Service
public class CargarFacturacionUseCase {
    private final CargaFacturacionRepository repository;

    public CargarFacturacionUseCase(CargaFacturacionRepository repository) {
        this.repository = repository;
    }


    @Transactional(transactionManager = "appTransactionManager")
    public List<Long> ejecutarLote(List<ArchivoFacturacion> archivos, long usuarioId, String correlationId) {
        HashSet<String> hashes = new HashSet<>();
        for (ArchivoFacturacion archivo : archivos) {
            if (!hashes.add(archivo.hash())) throw new RecursoDuplicadoException();
        }
        for (ArchivoFacturacion archivo : archivos) {
            if (repository.existsByHash(archivo.hash())) throw new RecursoDuplicadoException();
        }
        return archivos.stream()
                .map(archivo -> repository.save(archivo, usuarioId, correlationId))
                .toList();
    }
}
