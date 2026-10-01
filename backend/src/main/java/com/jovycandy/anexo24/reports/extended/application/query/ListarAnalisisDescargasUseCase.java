package com.jovycandy.anexo24.reports.extended.application.query;

import com.jovycandy.anexo24.reports.extended.domain.model.AnalisisDescarga;
import com.jovycandy.anexo24.reports.extended.domain.port.AnalisisDescargaRepository;
import com.jovycandy.anexo24.shared.api.Pagina;
import com.jovycandy.anexo24.shared.exception.SolicitudInvalidaException;
import org.springframework.stereotype.Service;

/** Lista relaciones históricas de descarga sin ejecutar procesos legacy. */
@Service
public class ListarAnalisisDescargasUseCase {
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

    private String normalizar(String valor) {
        if (valor == null || valor.isBlank()) return null;
        return valor.trim();
    }
}
