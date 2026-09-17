package com.jovycandy.anexo24.catalogs.materials.domain.port;

import com.jovycandy.anexo24.catalogs.materials.domain.model.Material;
import com.jovycandy.anexo24.shared.api.Pagina;

/**
 * Puerto de consulta de materiales del Módulo C.
 *
 * <p>Implementado con JdbcTemplate contra {@code dbo.material};
 * el dominio no conoce la implementación (ADR-001).</p>
 */
public interface MaterialRepository {

    /**
     * Consulta materiales paginados con filtro opcional.
     *
     * @param filtro   texto para filtrar por clave, descripción o fracción
     * @param pagina   número de página (base 1)
     * @param tamano   tamaño de página
     * @return página con materiales y total de registros
     */
    Pagina<Material> findPage(String filtro, int pagina, int tamano);
}