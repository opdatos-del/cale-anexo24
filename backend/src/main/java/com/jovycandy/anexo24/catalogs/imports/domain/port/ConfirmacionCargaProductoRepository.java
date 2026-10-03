package com.jovycandy.anexo24.catalogs.imports.domain.port;

import com.jovycandy.anexo24.catalogs.imports.domain.model.ConfirmacionCargaProducto;

/**
 * Puerto para la confirmación autoritativa de una carga de productos.
 *
 * <p>La implementación ejecuta el wrapper versionado
 * {@code dbo.APP24_C_PRODUCTO_CARGA_CONFIRMAR} que orquesta el stage legacy
 * {@code dbo.tmpproductos} / {@code dbo.ECargaProducto} y deja los nuevos
 * productos válidos en {@code dbo.productos}.</p>
 */
public interface ConfirmacionCargaProductoRepository {

    /**
     * Confirma de forma atómica la carga indicada mediante el command SQL versionado.
     *
     * @param cargaId identificador de la carga de staging
     * @return resumen estable de la confirmación
     */
    ConfirmacionCargaProducto confirmar(long cargaId);
}