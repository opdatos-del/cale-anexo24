package com.jovycandy.anexo24.catalogs.imports.domain.port;

import com.jovycandy.anexo24.catalogs.imports.domain.model.ConfirmacionCargaAgente;

/**
 * Puerto para la confirmación autoritativa de una carga de agentes aduanales.
 *
 * <p>La implementación ejecuta el wrapper versionado
 * {@code dbo.APP24_C_AGENTE_CARGA_CONFIRMAR} que orquesta el stage legacy
 * {@code dbo.TMPagentes} / {@code dbo.ECARGAagentes} y deja los nuevos
 * agentes válidos en {@code dbo.agentes}.</p>
 */
public interface ConfirmacionCargaAgenteRepository {

    /**
     * Confirma de forma atómica la carga indicada mediante el command SQL versionado.
     *
     * @param cargaId identificador de la carga de staging
     * @return resumen estable de la confirmación
     */
    ConfirmacionCargaAgente confirmar(long cargaId);
}