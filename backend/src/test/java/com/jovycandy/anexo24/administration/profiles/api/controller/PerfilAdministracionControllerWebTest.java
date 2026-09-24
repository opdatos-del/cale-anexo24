package com.jovycandy.anexo24.administration.profiles.api.controller;

import com.jovycandy.anexo24.Anexo24Application;
import com.jovycandy.anexo24.administration.profiles.application.command.ActualizarNombrePerfilUseCase;
import com.jovycandy.anexo24.administration.profiles.application.command.CambiarEstadoPerfilUseCase;
import com.jovycandy.anexo24.administration.profiles.application.command.CrearPerfilUseCase;
import com.jovycandy.anexo24.administration.profiles.application.command.ReemplazarPermisosPerfilUseCase;
import com.jovycandy.anexo24.administration.profiles.application.query.ListarPerfilesUseCase;
import com.jovycandy.anexo24.administration.profiles.application.query.ObtenerPermisosPerfilUseCase;
import com.jovycandy.anexo24.shared.api.Pagina;
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
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/** Pruebas HTTP y de autorización del listado administrativo de perfiles. */
@SpringBootTest(classes = Anexo24Application.class)
@AutoConfigureMockMvc
@ActiveProfiles("test")
class PerfilAdministracionControllerWebTest {
    private static final String ENDPOINT = "/api/v1/administracion/perfiles";

    @Autowired private MockMvc mockMvc;
    @MockitoBean private ListarPerfilesUseCase useCase;
    @MockitoBean private CrearPerfilUseCase crearPerfilUseCase;
    @MockitoBean private ActualizarNombrePerfilUseCase actualizarNombrePerfilUseCase;
    @MockitoBean private CambiarEstadoPerfilUseCase cambiarEstadoPerfilUseCase;
    @MockitoBean private ObtenerPermisosPerfilUseCase obtenerPermisosPerfilUseCase;
    @MockitoBean private ReemplazarPermisosPerfilUseCase reemplazarPermisosPerfilUseCase;

    @Test
    void permisoUsuariosAdministrarResponde200() throws Exception {
        when(useCase.ejecutar(null, null, 1, 20)).thenReturn(new Pagina<>(List.of(), 0L, 1, 20));

        mockMvc.perform(get(ENDPOINT)
                        .with(user("admin").authorities(new SimpleGrantedAuthority("USUARIOS_ADMINISTRAR"))))
                .andExpect(status().isOk());
    }

    @Test
    void permisoPerfilesAdministrarResponde200() throws Exception {
        when(useCase.ejecutar(null, null, 1, 20)).thenReturn(new Pagina<>(List.of(), 0L, 1, 20));

        mockMvc.perform(get(ENDPOINT)
                        .with(user("admin").authorities(new SimpleGrantedAuthority("PERFILES_ADMINISTRAR"))))
                .andExpect(status().isOk());
    }

    @Test
    void sinNingunoDeLosPermisosResponde403() throws Exception {
        mockMvc.perform(get(ENDPOINT)
                        .with(user("usuario").authorities(new SimpleGrantedAuthority("OTRO_PERMISO"))))
                .andExpect(status().isForbidden());
    }

    @Test
    void permisoUsuariosAdministrarNoAutorizaCommandsDePerfiles() throws Exception {
        mockMvc.perform(post(ENDPOINT)
                        .contentType("application/json")
                        .content("{\"nombre\":\"OPERACION\"}")
                        .with(user("admin").authorities(new SimpleGrantedAuthority("USUARIOS_ADMINISTRAR"))))
                .andExpect(status().isForbidden());
    }

    @Test
    void sinAutenticacionResponde401() throws Exception {
        mockMvc.perform(get(ENDPOINT))
                .andExpect(status().isUnauthorized());
    }
}
