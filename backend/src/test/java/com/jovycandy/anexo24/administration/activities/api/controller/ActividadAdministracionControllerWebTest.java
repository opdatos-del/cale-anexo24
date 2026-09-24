package com.jovycandy.anexo24.administration.activities.api.controller;

import com.jovycandy.anexo24.Anexo24Application;
import com.jovycandy.anexo24.administration.activities.application.query.ListarActividadesUseCase;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;

import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/** Pruebas HTTP y de autorización del catálogo administrativo de actividades. */
@SpringBootTest(classes = Anexo24Application.class)
@AutoConfigureMockMvc
@ActiveProfiles("test")
class ActividadAdministracionControllerWebTest {
    private static final String ENDPOINT = "/api/v1/administracion/actividades";

    @Autowired private MockMvc mockMvc;
    @MockitoBean private ListarActividadesUseCase useCase;

    @Test
    void permisoPerfilesAdministrarResponde200() throws Exception {
        when(useCase.ejecutar()).thenReturn(List.of());

        mockMvc.perform(get(ENDPOINT)
                        .with(user("admin").authorities(new SimpleGrantedAuthority("PERFILES_ADMINISTRAR"))))
                .andExpect(status().isOk());
    }

    @Test
    void otroPermisoResponde403() throws Exception {
        mockMvc.perform(get(ENDPOINT)
                        .with(user("usuario").authorities(new SimpleGrantedAuthority("OTRO_PERMISO"))))
                .andExpect(status().isForbidden());
    }

    @Test
    void sinAutenticacionResponde401() throws Exception {
        mockMvc.perform(get(ENDPOINT))
                .andExpect(status().isUnauthorized());
    }
}
