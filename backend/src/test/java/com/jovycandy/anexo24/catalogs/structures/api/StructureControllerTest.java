package com.jovycandy.anexo24.catalogs.structures.api;

import com.jovycandy.anexo24.Anexo24Application;
import com.jovycandy.anexo24.catalogs.structures.application.query.ListarEstructurasUseCase;
import com.jovycandy.anexo24.shared.api.Pagina;
import com.jovycandy.anexo24.shared.exception.SolicitudInvalidaException;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.dao.DataAccessResourceFailureException;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/** Pruebas web y de autorización del catálogo de estructuras. */
@SpringBootTest(classes = Anexo24Application.class)
@AutoConfigureMockMvc
@ActiveProfiles("test")
class StructureControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private ListarEstructurasUseCase useCase;

    @Test
    void sinTokenResponde401ConApiError() throws Exception {
        mockMvc.perform(get("/api/v1/catalogos/estructuras"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("AUTENTICACION_REQUERIDA"))
                .andExpect(jsonPath("$.correlationId").exists());
    }

    @Test
    void sinPermisoResponde403() throws Exception {
        mockMvc.perform(get("/api/v1/catalogos/estructuras")
                        .with(user("usuario").authorities(new SimpleGrantedAuthority("OTRO_PERMISO"))))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("ACCESO_DENEGADO"));
    }

    @Test
    void autorizadoResponde200() throws Exception {
        when(useCase.ejecutar(null, null, 1, 20)).thenReturn(new Pagina<>(List.of(), 0, 1, 20));

        mockMvc.perform(get("/api/v1/catalogos/estructuras")
                        .with(user("usuario")
                                .authorities(new SimpleGrantedAuthority("ESTRUCTURAS_CONSULTAR"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.items").isArray())
                .andExpect(jsonPath("$.items").isEmpty())
                .andExpect(jsonPath("$.total").value(0))
                .andExpect(jsonPath("$.pagina").value(1))
                .andExpect(jsonPath("$.tamano").value(20));
    }

    @Test
    void parametrosInvalidosResponden400() throws Exception {
        when(useCase.ejecutar(null, null, 0, 20))
                .thenThrow(new SolicitudInvalidaException("pagina inválida"));

        mockMvc.perform(get("/api/v1/catalogos/estructuras?pagina=0")
                        .with(user("usuario")
                                .authorities(new SimpleGrantedAuthority("ESTRUCTURAS_CONSULTAR"))))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("SOLICITUD_INVALIDA"))
                .andExpect(jsonPath("$.correlationId").exists());
    }

    @Test
    void errorDeBaseDeDatosResponde503() throws Exception {
        when(useCase.ejecutar(any(), any(), anyInt(), anyInt()))
                .thenThrow(new DataAccessResourceFailureException("BD no disponible"));

        mockMvc.perform(get("/api/v1/catalogos/estructuras?producto=200060&material=MAT-1")
                        .with(user("usuario")
                                .authorities(new SimpleGrantedAuthority("ESTRUCTURAS_CONSULTAR"))))
                .andExpect(status().isServiceUnavailable())
                .andExpect(jsonPath("$.code").value("DEPENDENCIA_NO_DISPONIBLE"))
                .andExpect(jsonPath("$.correlationId").exists());
    }
}
