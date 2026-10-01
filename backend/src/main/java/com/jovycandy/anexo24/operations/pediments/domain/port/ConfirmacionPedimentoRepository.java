package com.jovycandy.anexo24.operations.pediments.domain.port;

import com.jovycandy.anexo24.operations.pediments.domain.model.ConfirmacionPedimento;

/** Puerto del command autoritativo de confirmación de pedimentos. */
public interface ConfirmacionPedimentoRepository {

    /**
     * Confirma una carga de pedimentos de forma atómica e idempotente.
     *
     * @param cargaId       carga de staging a confirmar
     * @param usuarioId     actor autenticado
     * @param correlationId correlación de la solicitud, puede ser {@code null}
     * @return resultado de la confirmación
     */
    ConfirmacionPedimento confirmar(long cargaId, long usuarioId, String correlationId);
}
