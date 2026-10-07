package com.jovycandy.anexo24.reports.extended.application.query;

import com.jovycandy.anexo24.reports.extended.domain.model.Anexo30Fraccion;
import com.jovycandy.anexo24.reports.extended.domain.port.Anexo30FraccionRepository;
import com.jovycandy.anexo24.shared.api.Pagina;
import com.jovycandy.anexo24.shared.exception.SolicitudInvalidaException;
import org.springframework.stereotype.Service;

/** Lista fracciones persistidas usadas por el proceso Anexo 30. */
@Service
public class ListarAnexo30FraccionesUseCase {

    private final Anexo30FraccionRepository repository;

    public ListarAnexo30FraccionesUseCase(Anexo30FraccionRepository repository) {
        this.repository = repository;
    }

    /** Consulta el registro persistido sin ejecutar ni recalcular el proceso Anexo 30. */
    public Pagina<Anexo30Fraccion> ejecutar(String filtro, int pagina, int tamano) {
        if (pagina < 1 || tamano < 1 || tamano > 100) {
            throw new SolicitudInvalidaException("La paginación de fracciones Anexo 30 no es válida.");
        }
        return repository.findPage(filtro == null || filtro.isBlank() ? null : filtro.trim(), pagina, tamano);
    }
}
