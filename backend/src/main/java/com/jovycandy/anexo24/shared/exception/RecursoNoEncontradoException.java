package com.jovycandy.anexo24.shared.exception;

/**
 * Excepción para recursos que no existen o no son accesibles.
 *
 * <p>Se traduce a {@code 404 RECURSO_NO_ENCONTRADO} en la capa API
 * (convención de errores de administración, FASE 1).</p>
 */
public class RecursoNoEncontradoException extends RuntimeException {

    /**
     * Crea la excepción con mensaje público genérico.
     */
    public RecursoNoEncontradoException() {
        super("El recurso solicitado no existe.");
    }
}