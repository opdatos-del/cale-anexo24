package com.jovycandy.anexo24.reports.extended.application.query;

import com.jovycandy.anexo24.reports.extended.domain.model.RectificacionDetalle;
import com.jovycandy.anexo24.reports.extended.domain.port.RectificacionDetalleRepository;
import com.jovycandy.anexo24.shared.api.Pagina;
import com.jovycandy.anexo24.shared.exception.SolicitudInvalidaException;
import org.springframework.stereotype.Service;

/** Lista el detalle read-only de rectificaciones con filtro y paginación acotada. */
@Service
public class ListarRectificacionesDetalleUseCase {
    private final RectificacionDetalleRepository repository;

    public ListarRectificacionesDetalleUseCase(RectificacionDetalleRepository repository) {
        this.repository = repository;
    }

    public Pagina<RectificacionDetalle> ejecutar(String filtro, int pagina, int tamano) {
        if (pagina < 1 || tamano < 1 || tamano > 100) {
            throw new SolicitudInvalidaException("La paginación del detalle de rectificaciones no es válida.");
        }
        return repository.findPage(filtro == null || filtro.isBlank() ? null : filtro.trim(), pagina, tamano);
    }
}
