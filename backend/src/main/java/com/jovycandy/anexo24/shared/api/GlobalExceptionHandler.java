package com.jovycandy.anexo24.shared.api;

import com.jovycandy.anexo24.security.CredencialesInvalidasException;
import jakarta.servlet.http.HttpServletRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.HashMap;
import java.util.Map;

/**
 * Traduce excepciones a respuestas de error estándar de la API.
 *
 * <p>Evita filtrar detalles internos (ADR-004) y expone mensajes
 * accionables junto al identificador de correlación.</p>
 */
@RestControllerAdvice
public class GlobalExceptionHandler {

    /** Registro de errores internos. */
    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    /** Constante para localizar el identificador de correlación en el request. */
    public static final String CORRELATION_ID_ATTR = "correlationId";

    /**
     * Maneja credenciales inválidas con respuesta 401.
     *
     * @param ex      excepción de credenciales
     * @param request solicitud HTTP actual
     * @return 401 con mensaje genérico y correlación
     */
    @ExceptionHandler(CredencialesInvalidasException.class)
    public ResponseEntity<ApiError> handleCredenciales(CredencialesInvalidasException ex,
                                                       HttpServletRequest request) {
        String correlationId = correlationIdOf(request);
        ApiError body = new ApiError("CREDENCIALES_INVALIDAS",
                ex.getMessage(), correlationId);
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(body);
    }

    /**
     * Maneja errores de validación de parámetros de solicitud.
     *
     * @param ex      excepción de validación
     * @param request solicitud HTTP actual
     * @return 400 con el detalle de cada campo inválido
     */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ApiError> handleValidation(MethodArgumentNotValidException ex,
                                                     HttpServletRequest request) {
        Map<String, Object> details = new HashMap<>();
        ex.getBindingResult().getFieldErrors().forEach(error ->
                details.put(error.getField(), error.getDefaultMessage()));
        String correlationId = correlationIdOf(request);
        ApiError body = new ApiError("VALIDACION_INVALIDA",
                "La solicitud contiene campos inválidos.", correlationId, null, details);
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(body);
    }

    /**
     * Maneja excepciones inesperadas como respuesta genérica de error.
     *
     * @param ex      excepción no controlada
     * @param request solicitud HTTP actual
     * @return 500 sin detalles internos, conservando la correlación
     */
    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiError> handleUnexpected(Exception ex, HttpServletRequest request) {
        String correlationId = correlationIdOf(request);
        log.error("Error no controlado [{}] en {} {}", correlationId,
                request.getMethod(), request.getRequestURI(), ex);
        ApiError body = new ApiError("ERROR_INTERNO",
                "Ocurrió un error inesperado. Reporte el identificador de correlación.",
                correlationId, null, null);
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(body);
    }

    /**
     * Obtiene el identificador de correlación asociado al request.
     *
     * @param request solicitud HTTP actual
     * @return identificador de correlación o cadena vacía si no existe
     */
    private String correlationIdOf(HttpServletRequest request) {
        Object value = request.getAttribute(CORRELATION_ID_ATTR);
        return value == null ? "" : value.toString();
    }
}