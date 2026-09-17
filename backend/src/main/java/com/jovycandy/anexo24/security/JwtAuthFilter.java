package com.jovycandy.anexo24.security;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/**
 * Filtro que autentica solicitudes mediante token JWT.
 *
 * <p>Lee {@code Authorization: Bearer <token>}, valida la firma y
 * establece la autenticación con los permisos contenidos en el token
 * (ADR-002). Las solicitudes sin token continúan como anónimas.</p>
 */
@Component
public class JwtAuthFilter extends OncePerRequestFilter {

    /** Prefijo del encabezado de autorización. */
    private static final String BEARER = "Bearer ";

    private final JwtTokenService tokenService;

    /**
     * Constructor con el servicio de tokens.
     *
     * @param tokenService servicio de emisión y validación JWT
     */
    public JwtAuthFilter(JwtTokenService tokenService) {
        this.tokenService = tokenService;
    }

    /**
     * Autentica la solicitud si contiene un token válido.
     *
     * @param request  solicitud HTTP
     * @param response respuesta HTTP
     * @param chain    cadena de filtros
     */
    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain chain) throws ServletException, IOException {
        String header = request.getHeader("Authorization");
        if (header != null && header.startsWith(BEARER)) {
            try {
                Claims claims = tokenService.parseToken(header.substring(BEARER.length()));
                List<SimpleGrantedAuthority> authorities = new ArrayList<>();
                Object raw = claims.get("auth");
                if (raw instanceof List<?> lista) {
                    for (Object item : lista) {
                        authorities.add(new SimpleGrantedAuthority(String.valueOf(item)));
                    }
                }
                var authentication = new UsernamePasswordAuthenticationToken(
                        claims.getSubject(), null, authorities);
                SecurityContextHolder.getContext().setAuthentication(authentication);
            } catch (JwtException | IllegalArgumentException e) {
                // token inválido -> la solicitud permanece anónima
                SecurityContextHolder.clearContext();
            }
        }
        chain.doFilter(request, response);
    }
}