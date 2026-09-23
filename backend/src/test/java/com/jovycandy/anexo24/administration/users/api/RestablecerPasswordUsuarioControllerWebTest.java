package com.jovycandy.anexo24.administration.users.api;

import com.jovycandy.anexo24.Anexo24Application;
import com.jovycandy.anexo24.administration.users.application.command.RestablecerPasswordUsuarioUseCase;
import com.jovycandy.anexo24.shared.exception.RecursoNoEncontradoException;
import com.jovycandy.anexo24.shared.exception.SolicitudInvalidaException;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doThrow;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/** Pruebas HTTP y de autorización del restablecimiento administrativo de contraseña. */
@SpringBootTest(classes = Anexo24Application.class)
@AutoConfigureMockMvc
@ActiveProfiles("test")
class RestablecerPasswordUsuarioControllerWebTest {
    private static final String ENDPOINT = "/api/v1/administracion/usuarios/42/password";

    @Autowired private MockMvc mockMvc;
    @MockitoBean private RestablecerPasswordUsuarioUseCase useCase;

    @Test
    void autorizadoResponde204SinBodyNiSecretos() throws Exception {
        mockMvc.perform(post(ENDPOINT)
                        .with(user("admin").authorities(new SimpleGrantedAuthority("USUARIOS_ADMINISTRAR")))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"password\":\"Abcdefgh1!\"}"))
                .andExpect(status().isNoContent())
                .andExpect(content().string(""));
    }

    @Test
    void bodyVacioResponde400() throws Exception {
        mockMvc.perform(post(ENDPOINT)
                        .with(user("admin").authorities(new SimpleGrantedAuthority("USUARIOS_ADMINISTRAR")))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void passwordFueraDePoliticaResponde400SinReflejarPassword() throws Exception {
        doThrow(new SolicitudInvalidaException("La contraseña no cumple la política de seguridad."))
                .when(useCase).ejecutar(anyLong(), any(), any());
        mockMvc.perform(post(ENDPOINT)
                        .with(user("admin").authorities(new SimpleGrantedAuthority("USUARIOS_ADMINISTRAR")))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"password\":\"secreto-invalido\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("SOLICITUD_INVALIDA"))
                .andExpect(content().string(org.hamcrest.Matchers.not(
                        org.hamcrest.Matchers.containsString("secreto-invalido"))));
    }

    @Test
    void sinAutenticacionResponde401() throws Exception {
        mockMvc.perform(post(ENDPOINT)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"password\":\"Abcdefgh1!\"}"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void sinPermisoResponde403() throws Exception {
        mockMvc.perform(post(ENDPOINT)
                        .with(user("usuario").authorities(new SimpleGrantedAuthority("OTRO_PERMISO")))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"password\":\"Abcdefgh1!\"}"))
                .andExpect(status().isForbidden());
    }

    @Test
    void usuarioInexistenteResponde404() throws Exception {
        doThrow(new RecursoNoEncontradoException()).when(useCase).ejecutar(anyLong(), any(), anyString());
        mockMvc.perform(post(ENDPOINT)
                        .with(user("admin").authorities(new SimpleGrantedAuthority("USUARIOS_ADMINISTRAR")))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"password\":\"Abcdefgh1!\"}"))
                .andExpect(status().isNotFound());
    }
}
