package com.jovycandy.anexo24.shared.exception;

/**
 * Excepción para intentos de crear un recurso que ya existe.
 *
 * <p>Se traduce a {@code 409 RECURSO_DUPLICADO} en la capa API
 * (convención de errores de administración, FASE 1).</p>
 */
public class RecursoDuplicadoException extends RuntimeException {

    /**
     * Crea la excepción con mensaje público genérico.
     */
    public RecursoDuplicadoException() {
        super("El recurso ya existe.");
    }
}