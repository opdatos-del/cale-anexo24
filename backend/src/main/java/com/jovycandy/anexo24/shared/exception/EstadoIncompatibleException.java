package com.jovycandy.anexo24.shared.exception;

/**
 * Excepción para operaciones incompatibles con el estado del recurso.
 *
 * <p>Se traduce a {@code 409 ESTADO_INCOMPATIBLE} en la capa API
 * (convención de errores de administración, FASE 1).</p>
 */
public class EstadoIncompatibleException extends RuntimeException {

    /**
     * Crea la excepción con mensaje público genérico.
     */
    public EstadoIncompatibleException() {
        super("La operación entra en conflicto con el estado actual del recurso.");
    }
}