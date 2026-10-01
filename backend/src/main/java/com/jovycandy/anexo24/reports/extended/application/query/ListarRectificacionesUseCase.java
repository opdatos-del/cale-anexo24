package com.jovycandy.anexo24.reports.extended.application.query;

import com.jovycandy.anexo24.reports.extended.domain.model.Rectificacion;
import com.jovycandy.anexo24.reports.extended.domain.port.RectificacionRepository;
import com.jovycandy.anexo24.shared.api.Pagina;
import com.jovycandy.anexo24.shared.exception.SolicitudInvalidaException;
import org.springframework.stereotype.Service;

/** Lista el resumen read-only de rectificaciones con filtro y paginación acotada. */
@Service
public class ListarRectificacionesUseCase {
    private final RectificacionRepository repository;

    public ListarRectificacionesUseCase(RectificacionRepository repository) {
        this.repository = repository;
    }

    public Pagina<Rectificacion> ejecutar(String filtro, int pagina, int tamano) {
        if (pagina < 1 || tamano < 1 || tamano > 100) {
            throw new SolicitudInvalidaException("La paginación de rectificaciones no es válida.");
        }
        return repository.findPage(normalizar(filtro), pagina, tamano);
    }

    private String normalizar(String valor) {
        if (valor == null || valor.isBlank()) return null;
        return valor.trim();
    }
}
