package com.jovycandy.anexo24.catalogs.imports.domain.port;

import com.jovycandy.anexo24.catalogs.imports.domain.model.ConfirmacionCargaSubmaquila;

/**
 * Puerto para la confirmación autoritativa de una carga de constancias de transferencia.
 *
 * <p>La implementación ejecuta el wrapper versionado
 * {@code dbo.APP24_C_SUBMAQUILA_CARGA_CONFIRMAR} que aísla el stage legacy
 * {@code dbo.TMPSUBMAQUILA}, delega la regla de negocio en
 * {@code dbo.CARGA_SUBMAQUILA} y deja los renglones válidos en
 * {@code dbo.SALIDAS} / {@code dbo.PSALIDAS}.</p>
 */
public interface ConfirmacionCargaSubmaquilaRepository {

    /**
     * Confirma de forma atómica la carga indicada mediante el command SQL versionado.
     *
     * @param cargaId identificador de la carga de staging
     * @return resumen estable de la confirmación
     */
    ConfirmacionCargaSubmaquila confirmar(long cargaId);
}
