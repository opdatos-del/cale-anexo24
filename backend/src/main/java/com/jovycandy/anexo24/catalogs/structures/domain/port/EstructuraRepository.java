package com.jovycandy.anexo24.catalogs.structures.domain.port;

import com.jovycandy.anexo24.catalogs.structures.domain.model.EstructuraDetalle;
import com.jovycandy.anexo24.shared.api.Pagina;

/** Puerto de consulta de líneas BOM mediante el contrato SQL aprobado. */
public interface EstructuraRepository {

    /**
     * Consulta líneas de estructura paginadas con filtros opcionales.
     *
     * @param producto clave funcional del producto
     * @param material clave funcional del material
     * @param pagina número de página base 1
     * @param tamano tamaño de página
     * @return página con líneas BOM y total de coincidencias
     */
    Pagina<EstructuraDetalle> findPage(String producto, String material, int pagina, int tamano);
}
