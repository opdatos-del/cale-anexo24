package com.jovycandy.anexo24.security;

/**
 * Excepción de credenciales inválidas o cuenta no disponible.
 *
 * <p>Mensaje genérico para no revelar si la clave o la contraseña
 * fueron incorrectas (ADR-002).</p>
 */
public class CredencialesInvalidasException extends RuntimeException {

    /**
     * Crea la excepción con mensaje genérico.
     */
    public CredencialesInvalidasException() {
        super("Credenciales inválidas o cuenta no disponible.");
    }
}