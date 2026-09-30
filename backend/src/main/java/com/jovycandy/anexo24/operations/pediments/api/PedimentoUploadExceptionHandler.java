package com.jovycandy.anexo24.operations.pediments.api;

import com.jovycandy.anexo24.shared.api.ApiError;
import com.jovycandy.anexo24.shared.api.GlobalExceptionHandler;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

/** Errores públicos de validación del upload de pedimentos. */
@RestControllerAdvice(basePackageClasses = PedimentoController.class)
public class PedimentoUploadExceptionHandler {
    @ExceptionHandler(PedimentoArchivoInvalidoException.class)
    public ResponseEntity<ApiError> archivoInvalido(PedimentoArchivoInvalidoException exception, HttpServletRequest request) {
        Object correlationId = request.getAttribute(GlobalExceptionHandler.CORRELATION_ID_ATTR);
        return ResponseEntity.badRequest().body(new ApiError("PEDIMENTO_ARCHIVO_INVALIDO", exception.getMessage(),
                correlationId == null ? "" : correlationId.toString()));
    }

    public static class PedimentoArchivoInvalidoException extends RuntimeException {
        public PedimentoArchivoInvalidoException(String message) {
            super(message);
        }
    }
}
