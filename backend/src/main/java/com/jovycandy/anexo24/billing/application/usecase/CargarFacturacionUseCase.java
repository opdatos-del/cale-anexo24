package com.jovycandy.anexo24.billing.application.usecase;

import com.jovycandy.anexo24.billing.domain.model.ArchivoFacturacion;
import com.jovycandy.anexo24.billing.domain.port.CargaFacturacionRepository;
import com.jovycandy.anexo24.shared.exception.RecursoDuplicadoException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Service
public class CargarFacturacionUseCase {
    private final CargaFacturacionRepository repository;
    private final ObjectMapper objectMapper = new ObjectMapper();

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
                .map(archivo -> repository.save(archivo, usuarioId, correlationId, filasJson(archivo)))
                .toList();
    }

    private String filasJson(ArchivoFacturacion archivo) {
        try {
            return objectMapper.writeValueAsString(archivo.filasNormalizadas().stream().map(fila -> {
                Map<String, String> datos = new LinkedHashMap<>();
                for (int i = 0; i < archivo.columnas().size(); i++)
                    datos.put(archivo.columnas().get(i), i < fila.celdas().size() ? fila.celdas().get(i) : "");
                return Map.of("hoja", fila.hoja(), "fila", fila.numero(), "datos", datos);
            }).toList());
        } catch (JsonProcessingException exception) {
            throw new IllegalStateException("No se pudieron serializar las filas de staging", exception);
        }
    }
}
