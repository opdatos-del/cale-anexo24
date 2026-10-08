package com.jovycandy.anexo24.reports.extended.application.query;

import com.jovycandy.anexo24.reports.extended.domain.model.Anexo30Comparativa;
import com.jovycandy.anexo24.reports.extended.domain.port.Anexo30ComparativaRepository;
import com.jovycandy.anexo24.shared.api.Pagina;
import com.jovycandy.anexo24.shared.exception.SolicitudInvalidaException;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

/** Lista la ultima comparativa A31/A24 persistida de Anexo 30. */
@Service
public class ListarAnexo30ComparativaUseCase {
    private static final int TAMANO_PAGINA_EXPORTACION = 100;
    private static final int MAXIMO_FILAS_EXPORTACION = 10_000;

    private final Anexo30ComparativaRepository repository;

    public ListarAnexo30ComparativaUseCase(Anexo30ComparativaRepository repository) {
        this.repository = repository;
    }

    public Pagina<Anexo30Comparativa> ejecutar(String filtro, int pagina, int tamano) {
        if (pagina < 1 || tamano < 1 || tamano > 100) {
            throw new SolicitudInvalidaException("La paginacion de comparativa Anexo 30 no es valida.");
        }
        return repository.findPage(normalizar(filtro), pagina, tamano);
    }

    /** Obtiene comparativa persistida apta para exportacion sin recalcular Anexo 30. */
    public List<Anexo30Comparativa> exportar(String filtro) {
        String normalizado = normalizar(filtro);
        Pagina<Anexo30Comparativa> primeraPagina = repository.findPage(normalizado, 1, TAMANO_PAGINA_EXPORTACION);
        if (primeraPagina.total() > MAXIMO_FILAS_EXPORTACION) {
            throw new SolicitudInvalidaException("La exportacion excede el maximo de "
                    + MAXIMO_FILAS_EXPORTACION + " filas.");
        }
        if (primeraPagina.total() == 0) return List.of();

        List<Anexo30Comparativa> resultados = new ArrayList<>((int) primeraPagina.total());
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
