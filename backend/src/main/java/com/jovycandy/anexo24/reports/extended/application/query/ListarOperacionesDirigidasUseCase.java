package com.jovycandy.anexo24.reports.extended.application.query;

import com.jovycandy.anexo24.reports.extended.domain.model.OperacionDirigida;
import com.jovycandy.anexo24.reports.extended.domain.port.OperacionDirigidaRepository;
import com.jovycandy.anexo24.shared.api.Pagina;
import com.jovycandy.anexo24.shared.exception.SolicitudInvalidaException;
import org.springframework.stereotype.Service;

/** Lista líneas de salida marcadas como dirigidas, sin ejecutar descargos. */
@Service
public class ListarOperacionesDirigidasUseCase {
    private final OperacionDirigidaRepository repository;

    public ListarOperacionesDirigidasUseCase(OperacionDirigidaRepository repository) {
        this.repository = repository;
    }

    public Pagina<OperacionDirigida> ejecutar(String filtro, int pagina, int tamano) {
        if (pagina < 1 || tamano < 1 || tamano > 100) {
            throw new SolicitudInvalidaException("La paginación de dirigidos no es válida.");
        }
        return repository.findPage(normalizar(filtro), pagina, tamano);
    }

    private String normalizar(String valor) {
        if (valor == null || valor.isBlank()) return null;
        return valor.trim();
    }
}
