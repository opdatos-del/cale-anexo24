package com.jovycandy.anexo24.shared.exception;

/**
 * Excepción para cargas de pedimento que no pueden confirmarse por reglas de negocio
 * o de contrato de datos.
 *
 * <p>Se traduce a {@code 422 CONFIRMACION_NO_PROCESABLE} en la capa API. No se usa
 * {@link SolicitudInvalidaException} para no alterar su semántica global de 400.</p>
 */
public class ConfirmacionNoProcesableException extends RuntimeException {

    /**
     * Crea la excepción con mensaje público genérico.
     */
    public ConfirmacionNoProcesableException() {
        super("La carga no puede confirmarse en su estado o contrato actual.");
    }
}
