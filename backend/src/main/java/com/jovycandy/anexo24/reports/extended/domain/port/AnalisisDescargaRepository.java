package com.jovycandy.anexo24.reports.extended.domain.port;

import com.jovycandy.anexo24.reports.extended.domain.model.AnalisisDescarga;
import com.jovycandy.anexo24.shared.api.Pagina;

/** Puerto de consulta del análisis histórico de descargas. */
public interface AnalisisDescargaRepository {

    /**
     * Busca relaciones de descarga con paginación estable.
     *
     * @param filtro texto opcional sobre identificadores y materiales
     * @param pagina número de página base 1
     * @param tamano tamaño de página entre 1 y 100
     * @return página de relaciones históricas
     */
    Pagina<AnalisisDescarga> findPage(String filtro, int pagina, int tamano);
}
