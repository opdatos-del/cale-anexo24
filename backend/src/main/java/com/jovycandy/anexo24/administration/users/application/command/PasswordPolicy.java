package com.jovycandy.anexo24.administration.users.application.command;

import com.jovycandy.anexo24.shared.exception.SolicitudInvalidaException;
import org.springframework.stereotype.Component;

import java.util.regex.Pattern;

/** Valida la política V1 de contraseñas administrativas. */
@Component
public class PasswordPolicy {
    private static final Pattern PASSWORD_VALIDA = Pattern.compile(
            "(?s)(?=.*[A-Z])(?=.*[0-9])(?=.*[^\\p{Alnum}\\s]).{10,50}");

    /**
     * Valida una contraseña antes de codificarla.
     *
     * @param password contraseña temporal en texto plano
     */
    public void validar(String password) {
        if (password == null || !PASSWORD_VALIDA.matcher(password).matches()) {
            throw new SolicitudInvalidaException("La contraseña no cumple la política de seguridad.");
        }
    }
}
