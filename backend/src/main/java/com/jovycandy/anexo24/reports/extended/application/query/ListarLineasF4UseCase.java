package com.jovycandy.anexo24.reports.extended.application.query;

import com.jovycandy.anexo24.reports.extended.domain.model.LineaF4;
import com.jovycandy.anexo24.reports.extended.domain.port.LineaF4Repository;
import com.jovycandy.anexo24.shared.api.Pagina;
import com.jovycandy.anexo24.shared.exception.SolicitudInvalidaException;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

/** Lista líneas dirigidas F4 (CTM APAA y desperdicio) sin ejecutar descargos. */
@Service
public class ListarLineasF4UseCase {

    private static final int TAMANO_PAGINA_EXPORTACION = 100;
    private static final int MAXIMO_FILAS_EXPORTACION = 10_000;

    private final LineaF4Repository repository;

    public ListarLineasF4UseCase(LineaF4Repository repository) {
        this.repository = repository;
    }

    public Pagina<LineaF4> ejecutar(String filtro, int pagina, int tamano) {
        if (pagina < 1 || tamano < 1 || tamano > 100) {
            throw new SolicitudInvalidaException("La paginación de líneas F4 no es válida.");
        }
        return repository.findPage(normalizar(filtro), pagina, tamano);
    }

    /** Obtiene todas las líneas filtradas aptas para exportación. */
    public List<LineaF4> exportar(String filtro) {
        String normalizado = normalizar(filtro);
        Pagina<LineaF4> primeraPagina = repository.findPage(normalizado, 1, TAMANO_PAGINA_EXPORTACION);
        if (primeraPagina.total() > MAXIMO_FILAS_EXPORTACION) {
            throw new SolicitudInvalidaException("La exportación excede el máximo de "
                    + MAXIMO_FILAS_EXPORTACION + " filas.");
        }
        if (primeraPagina.total() == 0) return List.of();

        List<LineaF4> resultados = new ArrayList<>((int) primeraPagina.total());
        resultados.addAll(primeraPagina.items());
        for (int pagina = 2; pagina <= primeraPagina.totalPaginas(); pagina++) {
            resultados.addAll(repository.findPage(normalizado, pagina, TAMANO_PAGINA_EXPORTACION).items());
        }
        return resultados;
    }

    private String normalizar(String valor) {
        if (valor == null || valor.isBlank()) return null;
        return valor.trim();
    }
}
