package com.jovycandy.anexo24.reports.extended.application.query;

import com.jovycandy.anexo24.reports.extended.domain.model.Anexo30Descarga;
import com.jovycandy.anexo24.reports.extended.domain.port.Anexo30DescargaRepository;
import com.jovycandy.anexo24.shared.api.Pagina;
import com.jovycandy.anexo24.shared.exception.SolicitudInvalidaException;
import org.springframework.stereotype.Service;

/** Lista descargas del último snapshot persistido de Anexo 30. */
@Service
public class ListarAnexo30DescargasUseCase {
    private final Anexo30DescargaRepository repository;
    public ListarAnexo30DescargasUseCase(Anexo30DescargaRepository repository) { this.repository = repository; }
    public Pagina<Anexo30Descarga> ejecutar(String filtro, int pagina, int tamano) {
        if (pagina < 1 || tamano < 1 || tamano > 100) {
            throw new SolicitudInvalidaException("La paginación de descargas Anexo 30 no es válida.");
        }
        return repository.findPage(filtro == null || filtro.isBlank() ? null : filtro.trim(), pagina, tamano);
    }
}
