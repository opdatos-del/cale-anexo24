package com.jovycandy.anexo24.reports.extended.application.query;

import com.jovycandy.anexo24.reports.extended.domain.model.Vencimiento;
import com.jovycandy.anexo24.reports.extended.domain.port.VencimientoRepository;
import com.jovycandy.anexo24.shared.api.Pagina;
import com.jovycandy.anexo24.shared.exception.SolicitudInvalidaException;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

/** Lista vencimientos de desperdicio con filtro y paginación acotada. */
@Service
public class ListarVencimientosUseCase {
    private static final int TAMANO_PAGINA_EXPORTACION = 100;
    private static final int MAXIMO_FILAS_EXPORTACION = 10_000;

    private final VencimientoRepository repository;

    public ListarVencimientosUseCase(VencimientoRepository repository) {
        this.repository = repository;
    }

    public Pagina<Vencimiento> ejecutar(String filtro, int pagina, int tamano) {
        if (pagina < 1 || tamano < 1 || tamano > 100) {
            throw new SolicitudInvalidaException("La paginación de vencimientos no es válida.");
        }
        return repository.findPage(normalizar(filtro), pagina, tamano);
    }

    private String normalizar(String valor) {
        if (valor == null || valor.isBlank()) return null;
        return valor.trim();
    }

    /** Obtiene todas las filas filtradas aptas para exportacion. */
    public List<Vencimiento> exportar(String filtro) {
        String normalizado = normalizar(filtro);
        Pagina<Vencimiento> primeraPagina = repository.findPage(normalizado, 1, TAMANO_PAGINA_EXPORTACION);
        if (primeraPagina.total() > MAXIMO_FILAS_EXPORTACION) {
            throw new SolicitudInvalidaException("La exportacion excede el maximo de " + MAXIMO_FILAS_EXPORTACION + " filas.");
        }
        if (primeraPagina.total() == 0) return List.of();
        List<Vencimiento> resultados = new ArrayList<>((int) primeraPagina.total());
        resultados.addAll(primeraPagina.items());
        for (int pagina = 2; pagina <= primeraPagina.totalPaginas(); pagina++) {
            resultados.addAll(repository.findPage(normalizado, pagina, TAMANO_PAGINA_EXPORTACION).items());
        }
        return resultados;
    }

}
