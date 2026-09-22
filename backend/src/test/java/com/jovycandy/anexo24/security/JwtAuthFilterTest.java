package com.jovycandy.anexo24.security;

import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockFilterChain;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

import java.nio.charset.StandardCharsets;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/** Pruebas de identidad y permisos extraídos por el filtro JWT. */
class JwtAuthFilterTest {

    private static final String SECRET = "test-secret-for-tests-must-have-at-least-32-chars";

    @AfterEach
    void limpiaContexto() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void tokenValidoConservaUsuarioYPermisosEnElPrincipal() throws Exception {
        String token = new JwtTokenService(SECRET, 15).generateToken(
                42L, "operador", List.of("OPERACIONES_CONSULTAR", "BITACORA_CONSULTAR"));

        autentica(token);

        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        assertThat(authentication).isNotNull();
        assertThat(authentication.isAuthenticated()).isTrue();
        assertThat(authentication.getPrincipal()).isInstanceOf(AuthenticatedUserPrincipal.class);
        AuthenticatedUserPrincipal principal = (AuthenticatedUserPrincipal) authentication.getPrincipal();
        assertThat(principal.userId()).isEqualTo(42L);
        assertThat(principal.username()).isEqualTo("operador");
        assertThat(authentication.getName()).isEqualTo("operador");
        assertThat(authentication.getAuthorities())
                .extracting(Object::toString)
                .containsExactly("OPERACIONES_CONSULTAR", "BITACORA_CONSULTAR");
    }

    @Test
    void tokenConFirmaInvalidaNoAutentica() throws Exception {
        String token = Jwts.builder()
                .subject("operador")
                .claim("uid", 42L)
                .claim("auth", List.of("OPERACIONES_CONSULTAR"))
                .signWith(Keys.hmacShaKeyFor("other-secret-for-tests-must-have-at-least-32".getBytes(StandardCharsets.UTF_8)))
                .compact();

        autentica(token);

        assertThat(SecurityContextHolder.getContext().getAuthentication()).isNull();
    }

    @Test
    void tokenValidoSinUidNoAutentica() throws Exception {
        String token = tokenFirmado("operador", null);

        autentica(token);

        assertThat(SecurityContextHolder.getContext().getAuthentication()).isNull();
    }

    @Test
    void tokenValidoConUidNoPositivoNoAutentica() throws Exception {
        String token = tokenFirmado("operador", 0L);

        autentica(token);

        assertThat(SecurityContextHolder.getContext().getAuthentication()).isNull();
    }

    private void autentica(String token) throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addHeader("Authorization", "Bearer " + token);
        new JwtAuthFilter(new JwtTokenService(SECRET, 15)).doFilter(
                request, new MockHttpServletResponse(), new MockFilterChain());
    }

    private String tokenFirmado(String username, Object userId) {
        var builder = Jwts.builder()
                .subject(username)
                .claim("auth", List.of("OPERACIONES_CONSULTAR"));
        if (userId != null) {
            builder.claim("uid", userId);
        }
        return builder.signWith(Keys.hmacShaKeyFor(SECRET.getBytes(StandardCharsets.UTF_8))).compact();
    }
}
