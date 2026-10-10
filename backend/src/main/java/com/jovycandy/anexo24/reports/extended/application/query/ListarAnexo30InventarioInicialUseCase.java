package com.jovycandy.anexo24.reports.extended.application.query;

import com.jovycandy.anexo24.reports.extended.domain.model.Anexo30InventarioInicial;
import com.jovycandy.anexo24.reports.extended.domain.port.Anexo30InventarioInicialRepository;
import com.jovycandy.anexo24.shared.api.Pagina;
import com.jovycandy.anexo24.shared.exception.SolicitudInvalidaException;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

/** Consulta el inventario inicial persistido sin recalcular Anexo 30. */
@Service
public class ListarAnexo30InventarioInicialUseCase {
    private static final int TAMANO_PAGINA_EXPORTACION = 100;
    private static final int MAXIMO_FILAS_EXPORTACION = 10_000;
    private final Anexo30InventarioInicialRepository repository;

    public ListarAnexo30InventarioInicialUseCase(Anexo30InventarioInicialRepository repository) {
        this.repository = repository;
    }

    public Pagina<Anexo30InventarioInicial> ejecutar(String filtro, int pagina, int tamano) {
        if (pagina < 1 || tamano < 1 || tamano > 100) {
            throw new SolicitudInvalidaException("La paginacion de inventario inicial no es valida.");
        }
        return repository.findPage(normalizar(filtro), pagina, tamano);
    }

    public List<Anexo30InventarioInicial> exportar(String filtro) {
        String normalizado = normalizar(filtro);
        Pagina<Anexo30InventarioInicial> primera = repository.findPage(normalizado, 1, TAMANO_PAGINA_EXPORTACION);
        if (primera.total() > MAXIMO_FILAS_EXPORTACION) {
            throw new SolicitudInvalidaException("La exportacion excede el maximo de " + MAXIMO_FILAS_EXPORTACION + " filas.");
        }
        if (primera.total() == 0) return List.of();
        List<Anexo30InventarioInicial> resultado = new ArrayList<>((int) primera.total());
        resultado.addAll(primera.items());
        for (int pagina = 2; pagina <= primera.totalPaginas(); pagina++) {
            resultado.addAll(repository.findPage(normalizado, pagina, TAMANO_PAGINA_EXPORTACION).items());
        }
        return resultado;
    }

    private String normalizar(String valor) {
        return valor == null || valor.isBlank() ? null : valor.trim();
    }
}
