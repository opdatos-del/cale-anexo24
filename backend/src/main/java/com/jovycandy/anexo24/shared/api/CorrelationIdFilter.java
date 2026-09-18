package com.jovycandy.anexo24.shared.api;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.UUID;

/**
 * Asigna un identificador de correlación a cada solicitud.
 *
 * <p>Acepta el encabezado {@code X-Correlation-Id} si el cliente lo
 * proporciona; de lo contrario genera un {@link UUID}. El valor queda
 * disponible como atributo del request para logs y respuestas de error
 * (ADR-004).</p>
 */
@Component
public class CorrelationIdFilter extends OncePerRequestFilter {

    /** Encabezado HTTP opcional para trazabilidad externa. */
    public static final String HEADER_NAME = "X-Correlation-Id";

    /**
     * Genera o reutiliza el identificador y lo propaga a la respuesta.
     *
     * @param request  solicitud HTTP
     * @param response respuesta HTTP
     * @param chain    cadena de filtros
     */
    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain chain) throws ServletException, IOException {
        String correlationId = request.getHeader(HEADER_NAME);
        if (correlationId == null || !correlationId.matches("[A-Za-z0-9._-]{1,40}")) {
            correlationId = UUID.randomUUID().toString();
        }
        request.setAttribute(GlobalExceptionHandler.CORRELATION_ID_ATTR, correlationId);
        response.setHeader(HEADER_NAME, correlationId);
        chain.doFilter(request, response);
    }
}