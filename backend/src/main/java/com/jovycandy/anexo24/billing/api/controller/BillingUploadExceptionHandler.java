package com.jovycandy.anexo24.billing.api.controller;

import com.jovycandy.anexo24.shared.api.ApiError;
import com.jovycandy.anexo24.shared.api.GlobalExceptionHandler;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.multipart.MultipartException;

@RestControllerAdvice(basePackageClasses = CargaFacturacionController.class)
@Order(Ordered.HIGHEST_PRECEDENCE)
public class BillingUploadExceptionHandler {
    @ExceptionHandler(PlantillaFacturacionNoConfiguradaException.class)
    public ResponseEntity<ApiError> plantillaNoConfigurada(PlantillaFacturacionNoConfiguradaException exception,
                                                             HttpServletRequest request) {
        return ResponseEntity.status(HttpStatus.CONFLICT).body(error("FACTURACION_PLANTILLA_NO_CONFIGURADA",
                "No existe una plantilla activa de Facturación.", request));
    }

    @ExceptionHandler(BillingArchivoInvalidoException.class)
    public ResponseEntity<ApiError> archivoInvalido(BillingArchivoInvalidoException exception,
                                                     HttpServletRequest request) {
        return ResponseEntity.badRequest().body(error("FACTURACION_ARCHIVO_INVALIDO",
                exception.getMessage(), request));
    }

    @ExceptionHandler(MultipartException.class)
    public ResponseEntity<ApiError> multipartInvalido(MultipartException exception,
                                                        HttpServletRequest request) {
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(error("FACTURACION_MULTIPART_INVALIDO",
                "La solicitud multipart excede los límites o tiene una estructura inválida.", request));
    }

    private ApiError error(String code, String message, HttpServletRequest request) {
        Object correlationId = request.getAttribute(GlobalExceptionHandler.CORRELATION_ID_ATTR);
        return new ApiError(code, message, correlationId == null ? "" : correlationId.toString());
    }

    public static class PlantillaFacturacionNoConfiguradaException extends RuntimeException {
    }

    public static class BillingArchivoInvalidoException extends RuntimeException {
        public BillingArchivoInvalidoException(String message) { super(message); }
    }
}
