package com.jovycandy.anexo24.shared.exception;

/** Excepción para parámetros que incumplen las reglas de una solicitud. */
public class SolicitudInvalidaException extends RuntimeException {

    /**
     * Constructor con el motivo de la solicitud inválida.
     *
     * @param message mensaje técnico para uso interno
     */
    public SolicitudInvalidaException(String message) {
        super(message);
    }
}
