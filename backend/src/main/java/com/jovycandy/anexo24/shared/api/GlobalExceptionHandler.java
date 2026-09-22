package com.jovycandy.anexo24.shared.api;

import com.jovycandy.anexo24.security.CredencialesInvalidasException;
import com.jovycandy.anexo24.shared.exception.EstadoIncompatibleException;
import com.jovycandy.anexo24.shared.exception.RecursoDuplicadoException;
import com.jovycandy.anexo24.shared.exception.RecursoNoEncontradoException;
import com.jovycandy.anexo24.shared.exception.SolicitudInvalidaException;
import jakarta.servlet.http.HttpServletRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataAccessException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;

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
     * Maneja accesos autenticados sin permisos suficientes.
     *
     * @param ex excepción de autorización
     * @param request solicitud HTTP actual
     * @return 403 con mensaje genérico y correlación
     */
    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<ApiError> handleAccessDenied(AccessDeniedException ex,
                                                        HttpServletRequest request) {
        return response(HttpStatus.FORBIDDEN, "ACCESO_DENEGADO",
                "No tienes permisos para realizar esta operación.", request);
    }

    /**
     * Maneja solicitudes no autenticadas.
     *
     * @param ex excepción de autenticación
     * @param request solicitud HTTP actual
     * @return 401 con mensaje genérico y correlación
     */
    @ExceptionHandler(AuthenticationException.class)
    public ResponseEntity<ApiError> handleAuthentication(AuthenticationException ex,
                                                          HttpServletRequest request) {
        return response(HttpStatus.UNAUTHORIZED, "AUTENTICACION_REQUERIDA",
                "Se requiere una autenticación válida.", request);
    }

    /**
     * Maneja JSON inválido o parámetros con tipo incorrecto.
     *
     * @param ex excepción de formato o conversión
     * @param request solicitud HTTP actual
     * @return 400 con mensaje accionable y correlación
     */
    @ExceptionHandler({HttpMessageNotReadableException.class, MethodArgumentTypeMismatchException.class})
    public ResponseEntity<ApiError> handleMalformedRequest(Exception ex,
                                                            HttpServletRequest request) {
        return response(HttpStatus.BAD_REQUEST, "SOLICITUD_INVALIDA",
                "La solicitud tiene un formato o parámetro inválido.", request);
    }

    /**
     * Maneja reglas de negocio incumplidas por parámetros de la solicitud.
     *
     * @param ex excepción de solicitud inválida
     * @param request solicitud HTTP actual
     * @return 400 con mensaje público y correlación
     */
    @ExceptionHandler(SolicitudInvalidaException.class)
    public ResponseEntity<ApiError> handleInvalidRequest(SolicitudInvalidaException ex,
                                                          HttpServletRequest request) {
        return response(HttpStatus.BAD_REQUEST, "SOLICITUD_INVALIDA",
                "La solicitud tiene un formato o parámetro inválido.", request);
    }

    /**
     * Maneja recursos inexistentes con respuesta 404.
     *
     * @param ex      excepción de recurso no encontrado
     * @param request solicitud HTTP actual
     * @return 404 con código RECURSO_NO_ENCONTRADO y correlación
     */
    @ExceptionHandler(RecursoNoEncontradoException.class)
    public ResponseEntity<ApiError> handleRecursoNoEncontrado(RecursoNoEncontradoException ex,
                                                               HttpServletRequest request) {
        return response(HttpStatus.NOT_FOUND, "RECURSO_NO_ENCONTRADO",
                ex.getMessage(), request);
    }

    /**
     * Maneja duplicados con respuesta 409.
     *
     * @param ex      excepción de recurso duplicado
     * @param request solicitud HTTP actual
     * @return 409 con código RECURSO_DUPLICADO y correlación
     */
    @ExceptionHandler(RecursoDuplicadoException.class)
    public ResponseEntity<ApiError> handleRecursoDuplicado(RecursoDuplicadoException ex,
                                                             HttpServletRequest request) {
        return response(HttpStatus.CONFLICT, "RECURSO_DUPLICADO",
                ex.getMessage(), request);
    }

    /**
     * Maneja conflictos de estado con respuesta 409.
     *
     * @param ex      excepción de estado incompatible
     * @param request solicitud HTTP actual
     * @return 409 con código ESTADO_INCOMPATIBLE y correlación
     */
    @ExceptionHandler(EstadoIncompatibleException.class)
    public ResponseEntity<ApiError> handleEstadoIncompatible(EstadoIncompatibleException ex,
                                                              HttpServletRequest request) {
        return response(HttpStatus.CONFLICT, "ESTADO_INCOMPATIBLE",
                ex.getMessage(), request);
    }

    /**
     * Maneja indisponibilidad de una dependencia de datos.
     *
     * @param ex excepción de acceso a datos
     * @param request solicitud HTTP actual
     * @return 503 sin detalles internos
     */
    @ExceptionHandler(DataAccessException.class)
    public ResponseEntity<ApiError> handleDataAccess(DataAccessException ex,
                                                      HttpServletRequest request) {
        String correlationId = correlationIdOf(request);
        log.error("Error de acceso a datos [{}] en {} {}", correlationId,
                request.getMethod(), request.getRequestURI(), ex);
        return response(HttpStatus.SERVICE_UNAVAILABLE, "DEPENDENCIA_NO_DISPONIBLE",
                "El servicio de datos no está disponible. Reporte el identificador de correlación.", request);
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
     * Construye una respuesta de error estándar.
     *
     * @param status estado HTTP
     * @param code código estable de error
     * @param message mensaje público
     * @param request solicitud HTTP actual
     * @return respuesta estándar de API
     */
    private ResponseEntity<ApiError> response(HttpStatus status, String code,
                                               String message, HttpServletRequest request) {
        return ResponseEntity.status(status)
                .body(new ApiError(code, message, correlationIdOf(request)));
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