package com.jovycandy.anexo24.reports.extended.application.query;

import com.jovycandy.anexo24.reports.extended.domain.model.Anexo30Comparativa;
import com.jovycandy.anexo24.reports.extended.domain.port.Anexo30ComparativaRepository;
import com.jovycandy.anexo24.shared.api.Pagina;
import com.jovycandy.anexo24.shared.exception.SolicitudInvalidaException;
import org.springframework.stereotype.Service;

/** Lista la última comparativa A31/A24 persistida de Anexo 30. */
@Service
public class ListarAnexo30ComparativaUseCase {
    private final Anexo30ComparativaRepository repository;
    public ListarAnexo30ComparativaUseCase(Anexo30ComparativaRepository repository) { this.repository = repository; }
    public Pagina<Anexo30Comparativa> ejecutar(String filtro, int pagina, int tamano) {
        if (pagina < 1 || tamano < 1 || tamano > 100) {
            throw new SolicitudInvalidaException("La paginación de comparativa Anexo 30 no es válida.");
        }
        return repository.findPage(filtro == null || filtro.isBlank() ? null : filtro.trim(), pagina, tamano);
    }
}
