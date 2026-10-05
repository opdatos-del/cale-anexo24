package com.jovycandy.anexo24.operations.actas.domain.port;

import com.jovycandy.anexo24.operations.actas.domain.model.ConfirmacionCargaActa;

/** Puerto de confirmación autoritativa de una carga de actas. */
public interface ConfirmacionCargaActaRepository {

    /**
     * Confirma de forma atómica la carga indicada ejecutando el command SQL versionado.
     *
     * @param cargaId identificador de la carga de staging
     * @return resumen estable de la confirmación
     */
    ConfirmacionCargaActa confirmar(long cargaId);
}
