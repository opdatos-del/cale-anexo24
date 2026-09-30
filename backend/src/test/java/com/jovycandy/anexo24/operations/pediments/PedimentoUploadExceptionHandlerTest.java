package com.jovycandy.anexo24.operations.pediments;

import com.jovycandy.anexo24.operations.pediments.api.PedimentoUploadExceptionHandler;
import com.jovycandy.anexo24.shared.api.ApiError;
import com.jovycandy.anexo24.shared.api.GlobalExceptionHandler;
import jakarta.servlet.http.HttpServletRequest;
import org.junit.jupiter.api.Test;
import org.springframework.http.ResponseEntity;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/** Verifica que los errores de upload conserven la correlación del request. */
class PedimentoUploadExceptionHandlerTest {

    @Test
    void usaCorrelationIdDelContextoYNoLaUri() {
        HttpServletRequest request = mock(HttpServletRequest.class);
        when(request.getAttribute(GlobalExceptionHandler.CORRELATION_ID_ATTR)).thenReturn("corr-pedimento-001");
        PedimentoUploadExceptionHandler handler = new PedimentoUploadExceptionHandler();

        ResponseEntity<ApiError> response = handler.archivoInvalido(
                new PedimentoUploadExceptionHandler.PedimentoArchivoInvalidoException("archivo inválido"), request);

        assertThat(response.getStatusCode().value()).isEqualTo(400);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().correlationId()).isEqualTo("corr-pedimento-001");
        assertThat(response.getBody().correlationId()).isNotEqualTo(request.getRequestURI());
    }
}
