package com.jovycandy.anexo24.reports.extended.application.query;

import com.jovycandy.anexo24.reports.extended.domain.model.Anexo30Entrada;
import com.jovycandy.anexo24.reports.extended.domain.port.Anexo30EntradaRepository;
import com.jovycandy.anexo24.shared.api.Pagina;
import com.jovycandy.anexo24.shared.exception.SolicitudInvalidaException;
import org.springframework.stereotype.Service;

/** Lista entradas del snapshot A31 con filtro y paginacion acotada. */
@Service
public class ListarAnexo30EntradasUseCase {
    private final Anexo30EntradaRepository repository;

    public ListarAnexo30EntradasUseCase(Anexo30EntradaRepository repository) {
        this.repository = repository;
    }

    public Pagina<Anexo30Entrada> ejecutar(String filtro, int pagina, int tamano) {
        if (pagina < 1 || tamano < 1 || tamano > 100) {
            throw new SolicitudInvalidaException("La paginacion de entradas Anexo 30 no es valida.");
        }
        return repository.findPage(normalizar(filtro), pagina, tamano);
    }

    private String normalizar(String valor) {
        if (valor == null || valor.isBlank()) return null;
        return valor.trim();
    }
}
