package com.jovycandy.anexo24.catalogs.imports.api.controller;

import com.jovycandy.anexo24.shared.api.ApiError;
import com.jovycandy.anexo24.shared.api.GlobalExceptionHandler;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

/** Errores públicos de archivos de importación de catálogos. */
@RestControllerAdvice(basePackageClasses = CatalogImportController.class)
public class CatalogImportExceptionHandler {
    @ExceptionHandler(ArchivoInvalidoException.class)
    public ResponseEntity<ApiError> archivoInvalido(ArchivoInvalidoException exception, HttpServletRequest request) {
        Object correlationId = request.getAttribute(GlobalExceptionHandler.CORRELATION_ID_ATTR);
        return ResponseEntity.badRequest().body(new ApiError("CATALOG_IMPORT_ARCHIVO_INVALIDO", exception.getMessage(),
                correlationId == null ? "" : correlationId.toString()));
    }

    public static class ArchivoInvalidoException extends RuntimeException {
        public ArchivoInvalidoException(String message) { super(message); }
    }
}
