package com.jovycandy.anexo24.security;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/** Pruebas de acceso seguro al principal autenticado actual. */
class AuthenticatedUserContextTest {

    private final AuthenticatedUserContext context = new AuthenticatedUserContext();

    @AfterEach
    void limpiaContexto() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void devuelvePrincipalAutenticadoEsperado() {
        AuthenticatedUserPrincipal principal = new AuthenticatedUserPrincipal(42L, "operador");
        SecurityContextHolder.getContext().setAuthentication(new UsernamePasswordAuthenticationToken(
                principal, null, List.of(new SimpleGrantedAuthority("OPERACIONES_CONSULTAR"))));

        assertThat(context.currentUser()).contains(principal);
    }

    @Test
    void sinAuthenticationDevuelveVacio() {
        assertThat(context.currentUser()).isEmpty();
    }

    @Test
    void principalLegacyNoEsperadoDevuelveVacio() {
        SecurityContextHolder.getContext().setAuthentication(new UsernamePasswordAuthenticationToken(
                "operador", null, List.of(new SimpleGrantedAuthority("OPERACIONES_CONSULTAR"))));

        assertThat(context.currentUser()).isEmpty();
    }

    @Test
    void authenticationNoAutenticadaDevuelveVacio() {
        SecurityContextHolder.getContext().setAuthentication(new UsernamePasswordAuthenticationToken(
                new AuthenticatedUserPrincipal(42L, "operador"), null));

        assertThat(context.currentUser()).isEmpty();
    }
}
