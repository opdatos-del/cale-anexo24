package com.jovycandy.anexo24.security.api;

import com.jovycandy.anexo24.security.api.dto.LoginRequest;
import com.jovycandy.anexo24.security.api.dto.LoginResponse;
import com.jovycandy.anexo24.security.application.LoginService;
import com.jovycandy.anexo24.shared.api.GlobalExceptionHandler;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.mock.web.MockHttpServletRequest;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/** Pruebas de propagación de correlación desde API a LoginService. */
@ExtendWith(MockitoExtension.class)
class LoginControllerTest {

    @Mock
    private LoginService loginService;

    @Test
    void propagaCorrelationIdNormalizadoDelAtributoRequest() {
        LoginController controller = new LoginController(loginService);
        LoginRequest request = new LoginRequest("operador", "secreto-ficticio");
        LoginResponse response = new LoginResponse("token-ficticio", 15L, "Operador", List.of());
        MockHttpServletRequest servletRequest = new MockHttpServletRequest();
        servletRequest.setAttribute(GlobalExceptionHandler.CORRELATION_ID_ATTR, "req-123");
        when(loginService.login(request, "req-123")).thenReturn(response);

        ResponseEntity<LoginResponse> result = controller.login(request, servletRequest);

        verify(loginService).login(request, "req-123");
        assertThat(result.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(result.getBody()).isSameAs(response);
    }
}
