package com.jovycandy.anexo24.operations.pediments.domain.model;

import java.time.LocalDateTime;

/**
 * Resultado de una confirmación autoritativa de carga de pedimentos.
 *
 * <p>No conoce Spring, JDBC ni tipos de SQL Server.</p>
 */
public record ConfirmacionPedimento(long cargaId, String estado, String resultado, Integer tipoOperacion,
                                    int operacionesProcesadas, int partidasProcesadas,
                                    LocalDateTime fechaConfirmacion) {

    /** Resultado cuando la carga se confirmó en esta ejecución. */
    public static final String CONFIRMADO = "CONFIRMED";

    /** Resultado idempotente cuando la carga ya estaba confirmada. */
    public static final String YA_CONFIRMADO = "ALREADY_CONFIRMED";

    /** Indica si la carga quedó confirmada (nueva o previamente). */
    public boolean confirmada() {
        return CONFIRMADO.equals(resultado) || YA_CONFIRMADO.equals(resultado);
    }
}
