package com.jovycandy.anexo24.reports.extended.application.query;

import com.jovycandy.anexo24.reports.extended.domain.model.CompulsaDetalle;
import com.jovycandy.anexo24.reports.extended.domain.port.CompulsaDetalleRepository;
import com.jovycandy.anexo24.shared.api.Pagina;
import com.jovycandy.anexo24.shared.exception.SolicitudInvalidaException;
import org.springframework.stereotype.Service;

/** Lista el detalle read-only de compulsa con filtro tecnico y paginacion acotada. */
@Service
public class ListarCompulsaDetalleUseCase {
    private final CompulsaDetalleRepository repository;

    public ListarCompulsaDetalleUseCase(CompulsaDetalleRepository repository) {
        this.repository = repository;
    }

    public Pagina<CompulsaDetalle> ejecutar(String filtro, int pagina, int tamano) {
        if (pagina < 1 || tamano < 1 || tamano > 100) {
            throw new SolicitudInvalidaException("La paginacion del detalle de compulsa no es valida.");
        }
        return repository.findPage(filtro == null || filtro.isBlank() ? null : filtro.trim(), pagina, tamano);
    }
}
