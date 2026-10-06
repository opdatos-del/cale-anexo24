package com.jovycandy.anexo24.operations.constancias.domain.port;

import com.jovycandy.anexo24.operations.constancias.domain.model.ConfirmacionCargaConstancia;

/** Puerto de confirmación autoritativa de una carga de constancias. */
public interface ConfirmacionCargaConstanciaRepository {

    /**
     * Confirma de forma atómica la carga indicada ejecutando el command SQL versionado.
     *
     * @param cargaId identificador de la carga de staging
     * @return resumen estable de la confirmación
     */
    ConfirmacionCargaConstancia confirmar(long cargaId);
}
