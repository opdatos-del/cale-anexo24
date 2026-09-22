package com.jovycandy.anexo24.security;

import java.security.Principal;

/**
 * Identidad autenticada extraída exclusivamente de un JWT validado.
 *
 * @param userId   identificador del usuario en el esquema app24
 * @param username clave de acceso del usuario
 */
public record AuthenticatedUserPrincipal(Long userId, String username) implements Principal {

    /**
     * Devuelve la clave de acceso para mantener el contrato de Principal.
     *
     * @return clave de acceso del usuario
     */
    @Override
    public String getName() {
        return username;
    }
}
