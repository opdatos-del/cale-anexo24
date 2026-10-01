package com.jovycandy.anexo24.reports.extended.application.query;

import com.jovycandy.anexo24.reports.extended.domain.model.OperacionBloqueada;
import com.jovycandy.anexo24.reports.extended.domain.port.OperacionBloqueadaRepository;
import com.jovycandy.anexo24.shared.api.Pagina;
import com.jovycandy.anexo24.shared.exception.SolicitudInvalidaException;
import org.springframework.stereotype.Service;

/** Lista snapshots históricos de operaciones bloqueadas sin resolverlas. */
@Service
public class ListarOperacionesBloqueadasUseCase {
    private final OperacionBloqueadaRepository repository;

    public ListarOperacionesBloqueadasUseCase(OperacionBloqueadaRepository repository) {
        this.repository = repository;
    }

    /**
     * Consulta operaciones bloqueadas almacenadas por el proceso legacy.
     *
     * @param filtro texto opcional sobre documentos, claves, producto, material o folio
     * @param pagina número de página base 1
     * @param tamano tamaño de página entre 1 y 100
     * @return página de snapshots bloqueados
     */
    public Pagina<OperacionBloqueada> ejecutar(String filtro, int pagina, int tamano) {
        if (pagina < 1 || tamano < 1 || tamano > 100) {
            throw new SolicitudInvalidaException("La paginación de operaciones bloqueadas no es válida.");
        }
        return repository.findPage(normalizar(filtro), pagina, tamano);
    }

    private String normalizar(String valor) {
        if (valor == null || valor.isBlank()) return null;
        return valor.trim();
    }
}
