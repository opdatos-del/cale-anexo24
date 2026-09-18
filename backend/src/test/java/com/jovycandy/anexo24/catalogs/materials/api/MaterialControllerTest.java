package com.jovycandy.anexo24.catalogs.materials.api;

import com.jovycandy.anexo24.catalogs.materials.application.query.ListarMaterialesUseCase;

import com.jovycandy.anexo24.Anexo24Application;
import com.jovycandy.anexo24.shared.api.Pagina;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.dao.DataAccessResourceFailureException;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.security.core.authority.SimpleGrantedAuthority;

import java.util.List;

import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/** Pruebas web y de autorización del catálogo de Materiales. */
@SpringBootTest(classes = Anexo24Application.class)
@AutoConfigureMockMvc
@ActiveProfiles("test")
class MaterialControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private ListarMaterialesUseCase useCase;


    @Test
    void sinTokenResponde401() throws Exception {
        mockMvc.perform(get("/api/v1/catalogos/materiales"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void sinPermisoResponde403() throws Exception {
        mockMvc.perform(get("/api/v1/catalogos/materiales")
                        .with(user("usuario").authorities(new SimpleGrantedAuthority("OTRO_PERMISO"))))
                .andExpect(status().isForbidden());
    }

    @Test
    void parametrosInvalidosResponden400() throws Exception {
        when(useCase.ejecutar(null, 0, 20))
                .thenThrow(new IllegalArgumentException("pagina inválida"));

mockMvc.perform(get("/api/v1/catalogos/materiales?pagina=0")
                        .with(user("usuario").authorities(new SimpleGrantedAuthority("MATERIALES_CONSULTAR"))))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("SOLICITUD_INVALIDA"))
                .andExpect(jsonPath("$.correlationId").exists());
    }

    @Test
    void autorizadoResponde200() throws Exception {
        when(useCase.ejecutar(null, 1, 20)).thenReturn(new Pagina<>(List.of(), 0, 1, 20));

        mockMvc.perform(get("/api/v1/catalogos/materiales")
                        .with(user("usuario").authorities(new SimpleGrantedAuthority("MATERIALES_CONSULTAR"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.items").isArray())
                .andExpect(jsonPath("$.items").isEmpty());
    }

    @Test
    void errorDeBaseDeDatosResponde503() throws Exception {
        when(useCase.ejecutar(anyString(), anyInt(), anyInt()))
                .thenThrow(new DataAccessResourceFailureException("BD no disponible"));

        mockMvc.perform(get("/api/v1/catalogos/materiales?filtro=abc")
                        .with(user("usuario").authorities(new SimpleGrantedAuthority("MATERIALES_CONSULTAR"))))
                .andExpect(status().isServiceUnavailable())
                .andExpect(jsonPath("$.code").value("DEPENDENCIA_NO_DISPONIBLE"));
    }

}
