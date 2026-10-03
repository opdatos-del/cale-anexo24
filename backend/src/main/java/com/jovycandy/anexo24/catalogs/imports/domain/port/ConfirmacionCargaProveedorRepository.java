package com.jovycandy.anexo24.catalogs.imports.domain.port;

import com.jovycandy.anexo24.catalogs.imports.domain.model.ConfirmacionCargaProveedor;

/**
 * Puerto para la confirmación autoritativa de una carga de proveedores.
 *
 * <p>La implementación ejecuta el wrapper versionado
 * {@code dbo.APP24_C_PROVEEDOR_CARGA_CONFIRMAR} que orquesta el stage legacy
 * {@code dbo.TMPPROVEEDORES} / {@code dbo.ECARGAPROVEEDORES} y deja los nuevos
 * proveedores válidos en {@code dbo.Proveedores}.</p>
 */
public interface ConfirmacionCargaProveedorRepository {

    /**
     * Confirma de forma atómica la carga indicada mediante el command SQL versionado.
     *
     * @param cargaId identificador de la carga de staging
     * @return resumen estable de la confirmación
     */
    ConfirmacionCargaProveedor confirmar(long cargaId);
}