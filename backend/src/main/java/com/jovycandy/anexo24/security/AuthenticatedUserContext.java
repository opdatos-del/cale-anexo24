package com.jovycandy.anexo24.security;

import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

import java.util.Optional;

/** Acceso seguro al actor autenticado disponible en el SecurityContext actual. */
@Component
public class AuthenticatedUserContext {

    /**
     * Devuelve el principal sólo si proviene de autenticación JWT validada.
     *
     * @return actor autenticado o vacío cuando no existe identidad confiable
     */
    public Optional<AuthenticatedUserPrincipal> currentUser() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !authentication.isAuthenticated()
                || !(authentication.getPrincipal() instanceof AuthenticatedUserPrincipal principal)) {
            return Optional.empty();
        }
        return Optional.of(principal);
    }
}
