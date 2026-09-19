package com.jovycandy.anexo24.catalogs.products.domain.port;

import com.jovycandy.anexo24.catalogs.products.domain.model.Producto;
import com.jovycandy.anexo24.shared.api.Pagina;

/** Puerto de consulta del catálogo de Productos mediante el contrato SQL aprobado. */
public interface ProductoRepository {

    /**
     * Consulta productos paginados con filtro opcional.
     *
     * @param filtro texto para código, nombre o fracción
     * @param pagina número de página base 1
     * @param tamano tamaño de página
     * @return página de productos y total de coincidencias
     */
    Pagina<Producto> findPage(String filtro, int pagina, int tamano);
}
