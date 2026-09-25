package com.jovycandy.anexo24.billing;

import com.jovycandy.anexo24.Anexo24Application;
import com.jovycandy.anexo24.billing.application.usecase.CargarFacturacionUseCase;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(classes = Anexo24Application.class)
@AutoConfigureMockMvc
@ActiveProfiles("test")
class CargaFacturacionControllerSecurityTest {
    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private CargarFacturacionUseCase useCase;

    @Test
    void uploadSinAutenticacionResponde401() throws Exception {
        mockMvc.perform(multipart("/api/v1/facturacion/cargas")
                        .file(file()))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void uploadConAuthorityDeOtroModuloResponde403() throws Exception {
        mockMvc.perform(multipart("/api/v1/facturacion/cargas")
                        .file(file())
                        .with(user("qa").authorities(new SimpleGrantedAuthority("OPERACIONES_CONSULTAR"))))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("ACCESO_DENEGADO"));
    }

    @Test
    void permisoDeCargaAlcanzaValidacionDelArchivo() throws Exception {
        mockMvc.perform(multipart("/api/v1/facturacion/cargas")
                        .file(file())
                        .with(user("qa").authorities(new SimpleGrantedAuthority("FACTURACION_CARGAR"))))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("FACTURACION_ARCHIVO_INVALIDO"));
    }

    private MockMultipartFile file() {
        return new MockMultipartFile("archivos", "invalido.csv", "text/csv", new byte[]{1});
    }
}
