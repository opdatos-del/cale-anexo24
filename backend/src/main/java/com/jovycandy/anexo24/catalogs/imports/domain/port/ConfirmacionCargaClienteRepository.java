package com.jovycandy.anexo24.catalogs.imports.domain.port;

import com.jovycandy.anexo24.catalogs.imports.domain.model.ConfirmacionCargaCliente;

/**
 * Puerto para la confirmación autoritativa de una carga de clientes.
 *
 * <p>La implementación ejecuta el wrapper versionado
 * {@code dbo.APP24_C_CLIENTE_CARGA_CONFIRMAR} que orquesta el stage legacy
 * {@code dbo.TMPCLIENTES} / {@code dbo.ECARGACLIENTES} y deja los nuevos
 * clientes válidos en {@code dbo.clientes}.</p>
 */
public interface ConfirmacionCargaClienteRepository {

    /**
     * Confirma de forma atómica la carga indicada mediante el command SQL versionado.
     *
     * @param cargaId identificador de la carga de staging
     * @return resumen estable de la confirmación
     */
    ConfirmacionCargaCliente confirmar(long cargaId);
}