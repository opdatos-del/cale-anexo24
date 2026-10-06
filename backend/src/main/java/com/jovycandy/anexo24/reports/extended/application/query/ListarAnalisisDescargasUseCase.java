package com.jovycandy.anexo24.reports.extended.application.query;

import com.jovycandy.anexo24.reports.extended.domain.model.AnalisisDescarga;
import com.jovycandy.anexo24.reports.extended.domain.port.AnalisisDescargaRepository;
import com.jovycandy.anexo24.shared.api.Pagina;
import com.jovycandy.anexo24.shared.exception.SolicitudInvalidaException;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

/** Lista relaciones históricas de descarga sin ejecutar procesos legacy. */
@Service
public class ListarAnalisisDescargasUseCase {
    private static final int TAMANO_PAGINA_EXPORTACION = 100;
    private static final int MAXIMO_FILAS_EXPORTACION = 10_000;

    private final AnalisisDescargaRepository repository;

    public ListarAnalisisDescargasUseCase(AnalisisDescargaRepository repository) {
        this.repository = repository;
    }

    /**
     * Consulta la proyección read-only enriquecida de descargas.
     *
     * @param filtro texto opcional sobre pedimentos, materiales y productos
     * @param pagina número de página base 1
     * @param tamano tamaño de página entre 1 y 100
     * @return página de análisis histórico
     */
    public Pagina<AnalisisDescarga> ejecutar(String filtro, int pagina, int tamano) {
        if (pagina < 1 || tamano < 1 || tamano > 100) {
            throw new SolicitudInvalidaException("La paginación del análisis de descargas no es válida.");
        }
        return repository.findPage(normalizar(filtro), pagina, tamano);
    }

    /** Obtiene todas las relaciones históricas filtradas aptas para exportación. */
    public List<AnalisisDescarga> exportar(String filtro) {
        String normalizado = normalizar(filtro);
        Pagina<AnalisisDescarga> primeraPagina = repository.findPage(normalizado, 1, TAMANO_PAGINA_EXPORTACION);
        if (primeraPagina.total() > MAXIMO_FILAS_EXPORTACION) {
            throw new SolicitudInvalidaException("La exportación excede el máximo de "
                    + MAXIMO_FILAS_EXPORTACION + " filas.");
        }
        if (primeraPagina.total() == 0) return List.of();

        List<AnalisisDescarga> resultados = new ArrayList<>((int) primeraPagina.total());
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
