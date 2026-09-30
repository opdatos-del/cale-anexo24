package com.jovycandy.anexo24.reports.extended.application.query;

import com.jovycandy.anexo24.reports.extended.domain.model.Compulsa;
import com.jovycandy.anexo24.reports.extended.domain.port.CompulsaRepository;
import com.jovycandy.anexo24.shared.api.Pagina;
import com.jovycandy.anexo24.shared.exception.SolicitudInvalidaException;
import org.springframework.stereotype.Service;

/** Lista la compulsa general con filtro textual y paginación acotada. */
@Service
public class ListarCompulsaUseCase {
    private final CompulsaRepository repository;

    public ListarCompulsaUseCase(CompulsaRepository repository) {
        this.repository = repository;
    }

    public Pagina<Compulsa> ejecutar(String filtro, int pagina, int tamano) {
        if (pagina < 1 || tamano < 1 || tamano > 100) {
            throw new SolicitudInvalidaException("La paginación de compulsa no es válida.");
        }
        return repository.findPage(normalizar(filtro), pagina, tamano);
    }

    private String normalizar(String valor) {
        if (valor == null || valor.isBlank()) return null;
        return valor.trim();
    }
}
