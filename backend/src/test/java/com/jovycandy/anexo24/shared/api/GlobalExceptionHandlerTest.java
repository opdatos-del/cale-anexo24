package com.jovycandy.anexo24.shared.api;

import com.jovycandy.anexo24.shared.exception.EstadoIncompatibleException;
import com.jovycandy.anexo24.shared.exception.RecursoDuplicadoException;
import com.jovycandy.anexo24.shared.exception.RecursoNoEncontradoException;
import jakarta.servlet.http.HttpServletRequest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

/** Pruebas unitarias del manejador global de errores. */
@ExtendWith(MockitoExtension.class)
class GlobalExceptionHandlerTest {

    private static final String CORRELATION_ID = "req-1";

    @Mock
    private HttpServletRequest request;

    private GlobalExceptionHandler handler;

    @BeforeEach
    void setUp() {
        handler = new GlobalExceptionHandler();
        when(request.getAttribute(GlobalExceptionHandler.CORRELATION_ID_ATTR))
                .thenReturn(CORRELATION_ID);
    }

    @Test
    void recursoNoEncontradoResponde404ConCodigoYCorrelacion() {
        ResponseEntity<ApiError> respuesta = handler.handleRecursoNoEncontrado(
                new RecursoNoEncontradoException(), request);

        assertThat(respuesta.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
        ApiError cuerpo = respuesta.getBody();
        assertThat(cuerpo.code()).isEqualTo("RECURSO_NO_ENCONTRADO");
        assertThat(cuerpo.message()).isEqualTo("El recurso solicitado no existe.");
        assertThat(cuerpo.correlationId()).isEqualTo(CORRELATION_ID);
    }

    @Test
    void recursoDuplicadoResponde409ConCodigoYCorrelacion() {
        ResponseEntity<ApiError> respuesta = handler.handleRecursoDuplicado(
                new RecursoDuplicadoException(), request);

        assertThat(respuesta.getStatusCode()).isEqualTo(HttpStatus.CONFLICT);
        ApiError cuerpo = respuesta.getBody();
        assertThat(cuerpo.code()).isEqualTo("RECURSO_DUPLICADO");
        assertThat(cuerpo.message()).isEqualTo("El recurso ya existe.");
        assertThat(cuerpo.correlationId()).isEqualTo(CORRELATION_ID);
    }

    @Test
    void estadoIncompatibleResponde409ConCodigoYCorrelacion() {
        ResponseEntity<ApiError> respuesta = handler.handleEstadoIncompatible(
                new EstadoIncompatibleException(), request);

        assertThat(respuesta.getStatusCode()).isEqualTo(HttpStatus.CONFLICT);
        ApiError cuerpo = respuesta.getBody();
        assertThat(cuerpo.code()).isEqualTo("ESTADO_INCOMPATIBLE");
        assertThat(cuerpo.message())
                .isEqualTo("La operación entra en conflicto con el estado actual del recurso.");
        assertThat(cuerpo.correlationId()).isEqualTo(CORRELATION_ID);
    }
}