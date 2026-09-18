package com.jovycandy.anexo24.security;



import com.jovycandy.anexo24.shared.api.CorrelationIdFilter;
import com.jovycandy.anexo24.shared.api.GlobalExceptionHandler;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.MediaType;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.time.Instant;
import java.util.UUID;

/** Entry point que devuelve errores de autenticación con el contrato de la API. */
@Component
public class ApiAuthenticationEntryPoint implements AuthenticationEntryPoint {


    /**
     * Responde una solicitud no autenticada sin exponer detalles internos.
     *
     * @param request solicitud HTTP
     * @param response respuesta HTTP
     * @param authException excepción interna de autenticación
     * @throws IOException si no es posible escribir la respuesta
     * @throws ServletException si el contenedor rechaza la respuesta
     */
    @Override
    public void commence(HttpServletRequest request, HttpServletResponse response,
                         AuthenticationException authException) throws IOException, ServletException {
        String correlationId = correlationIdOf(request);
        response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.setCharacterEncoding("UTF-8");
        response.setHeader(CorrelationIdFilter.HEADER_NAME, correlationId);

        String timestamp = Instant.now().toString();
        response.getWriter().write("{\"code\":\"AUTENTICACION_REQUERIDA\","
                + "\"message\":\"Se requiere una autenticación válida.\","
                + "\"correlationId\":\"" + correlationId + "\","
                + "\"timestamp\":\"" + timestamp + "\"}");
    }

    private String correlationIdOf(HttpServletRequest request) {
        Object value = request.getAttribute(GlobalExceptionHandler.CORRELATION_ID_ATTR);
        if (value != null) {
            return value.toString();
        }
        String header = request.getHeader(CorrelationIdFilter.HEADER_NAME);
        if (header != null && header.matches("[A-Za-z0-9._-]{1,40}")) {
            request.setAttribute(GlobalExceptionHandler.CORRELATION_ID_ATTR, header);
            return header;
        }
        String generated = UUID.randomUUID().toString();
        request.setAttribute(GlobalExceptionHandler.CORRELATION_ID_ATTR, generated);
        return generated;
    }
}
