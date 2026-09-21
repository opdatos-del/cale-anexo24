package com.jovycandy.anexo24.operations.entries.api;

import com.jovycandy.anexo24.Anexo24Application;
import com.jovycandy.anexo24.operations.entries.application.query.ListarEntradasUseCase;
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

import java.time.LocalDate;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/** Pruebas web y de autorización de consulta de Entradas. */
@SpringBootTest(classes = Anexo24Application.class)
@AutoConfigureMockMvc
@ActiveProfiles("test")
class EntryControllerTest {

    private static final LocalDate DESDE = LocalDate.of(2025, 9, 23);
    private static final LocalDate HASTA = LocalDate.of(2026, 5, 28);

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private ListarEntradasUseCase useCase;

    @Test
    void sinTokenResponde401ConApiError() throws Exception {
        mockMvc.perform(get("/api/v1/operaciones/entradas")
                        .param("desde", "2025-09-23")
                        .param("hasta", "2026-05-28"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("AUTENTICACION_REQUERIDA"))
                .andExpect(jsonPath("$.correlationId").exists());
    }

    @Test
    void fechaNoISOResponde400ConApiError() throws Exception {
        mockMvc.perform(get("/api/v1/operaciones/entradas")
                        .param("desde", "no-es-fecha")
                        .param("hasta", "2026-05-28")
                        .with(user(usuarioAutorizado())))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("SOLICITUD_INVALIDA"))
                .andExpect(jsonPath("$.correlationId").exists());
    }

    @Test
    void filtroDemasiadoLargoResponde400ConApiError() throws Exception {
        when(useCase.ejecutar(DESDE, HASTA, null, "ABCDEF", null, null, 1, 20))
                .thenThrow(new SolicitudInvalidaException("filtro inválido"));

        mockMvc.perform(get("/api/v1/operaciones/entradas")
                        .param("desde", "2025-09-23")
                        .param("hasta", "2026-05-28")
                        .param("clavePedimento", "ABCDEF")
                        .with(user(usuarioAutorizado())))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("SOLICITUD_INVALIDA"))
                .andExpect(jsonPath("$.correlationId").exists());
    }

    @Test
    void sinPermisoResponde403() throws Exception {
        mockMvc.perform(get("/api/v1/operaciones/entradas")
                        .param("desde", "2025-09-23")
                        .param("hasta", "2026-05-28")
                        .with(user("usuario").authorities(new SimpleGrantedAuthority("OTRO_PERMISO"))))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("ACCESO_DENEGADO"));
    }

    @Test
    void autorizadoResponde200ConShapeDePagina() throws Exception {
        when(useCase.ejecutar(DESDE, HASTA, "5003971", "A1", "17019999", "500017", 1, 20))
                .thenReturn(new Pagina<>(List.of(), 1, 1, 20));

        mockMvc.perform(get("/api/v1/operaciones/entradas")
                        .param("desde", "2025-09-23")
                        .param("hasta", "2026-05-28")
                        .param("pedimento", "5003971")
                        .param("clavePedimento", "A1")
                        .param("fraccion", "17019999")
                        .param("numeroParte", "500017")
                        .with(user("usuario")
                                .authorities(new SimpleGrantedAuthority("OPERACIONES_CONSULTAR"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.items").isArray())
                .andExpect(jsonPath("$.total").value(1))
                .andExpect(jsonPath("$.pagina").value(1))
                .andExpect(jsonPath("$.tamano").value(20));
    }

    @Test
    void sinDesdeResponde400() throws Exception {
        when(useCase.ejecutar(any(), any(), any(), any(), any(), any(), anyInt(), anyInt()))
                .thenThrow(new SolicitudInvalidaException("rango obligatorio"));

        mockMvc.perform(get("/api/v1/operaciones/entradas")
                        .param("hasta", "2026-05-28")
                        .with(user(usuarioAutorizado())))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("SOLICITUD_INVALIDA"));
    }

    @Test
    void sinHastaResponde400() throws Exception {
        when(useCase.ejecutar(any(), any(), any(), any(), any(), any(), anyInt(), anyInt()))
                .thenThrow(new SolicitudInvalidaException("rango obligatorio"));

        mockMvc.perform(get("/api/v1/operaciones/entradas")
                        .param("desde", "2025-09-23")
                        .with(user(usuarioAutorizado())))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("SOLICITUD_INVALIDA"));
    }

    @Test
    void rangoInvertidoResponde400() throws Exception {
        when(useCase.ejecutar(any(), any(), any(), any(), any(), any(), anyInt(), anyInt()))
                .thenThrow(new SolicitudInvalidaException("rango inválido"));

        mockMvc.perform(get("/api/v1/operaciones/entradas")
                        .param("desde", "2026-05-28")
                        .param("hasta", "2025-09-23")
                        .with(user(usuarioAutorizado())))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("SOLICITUD_INVALIDA"));
    }

    @Test
    void paginacionInvalidaResponde400() throws Exception {
        when(useCase.ejecutar(any(), any(), any(), any(), any(), any(), anyInt(), anyInt()))
                .thenThrow(new SolicitudInvalidaException("paginación inválida"));

        mockMvc.perform(get("/api/v1/operaciones/entradas")
                        .param("desde", "2025-09-23")
                        .param("hasta", "2026-05-28")
                        .param("tamano", "101")
                        .with(user(usuarioAutorizado())))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("SOLICITUD_INVALIDA"));
    }

    @Test
    void errorDeBaseDeDatosResponde503() throws Exception {
        when(useCase.ejecutar(any(), any(), any(), any(), any(), any(), anyInt(), anyInt()))
                .thenThrow(new DataAccessResourceFailureException("BD no disponible"));

        mockMvc.perform(get("/api/v1/operaciones/entradas")
                        .param("desde", "2025-09-23")
                        .param("hasta", "2026-05-28")
                        .with(user(usuarioAutorizado())))
                .andExpect(status().isServiceUnavailable())
                .andExpect(jsonPath("$.code").value("DEPENDENCIA_NO_DISPONIBLE"))
                .andExpect(jsonPath("$.correlationId").exists());
    }

    private static org.springframework.security.core.userdetails.UserDetails usuarioAutorizado() {
        return org.springframework.security.core.userdetails.User.withUsername("usuario")
                .password("n/a")
                .authorities("OPERACIONES_CONSULTAR")
                .build();
    }
}
