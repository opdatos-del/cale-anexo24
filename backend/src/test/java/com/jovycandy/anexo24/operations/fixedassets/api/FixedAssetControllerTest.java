package com.jovycandy.anexo24.operations.fixedassets.api;

import com.jovycandy.anexo24.Anexo24Application;
import com.jovycandy.anexo24.operations.fixedassets.application.query.ListarActivosFijosUseCase;
import com.jovycandy.anexo24.operations.fixedassets.domain.model.ActivoFijo;
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

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/** Pruebas web y de autorización de consulta de Activos Fijos. */
@SpringBootTest(classes = Anexo24Application.class)
@AutoConfigureMockMvc
@ActiveProfiles("test")
class FixedAssetControllerTest {

    private static final LocalDate DESDE = LocalDate.of(2025, 9, 23);
    private static final LocalDate HASTA = LocalDate.of(2026, 5, 28);

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private ListarActivosFijosUseCase useCase;

    @Test
    void sinTokenResponde401() throws Exception {
        mockMvc.perform(get("/api/v1/operaciones/activos-fijos"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("AUTENTICACION_REQUERIDA"));
    }

    @Test
    void autorizadoSinRangoResponde200YSerializaCamposNulos() throws Exception {
        ActivoFijo activo = activoEjemplo();
        when(useCase.ejecutar(isNull(), isNull(), isNull(), isNull(), isNull(), isNull(),
                isNull(), isNull(), isNull(), eq(1), eq(20)))
                .thenReturn(new Pagina<>(List.of(activo), 1, 1, 20));

        mockMvc.perform(get("/api/v1/operaciones/activos-fijos")
                        .with(user(usuarioAutorizado())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.items").isArray())
                .andExpect(jsonPath("$.items[0].partidaEntradaId").value(2001))
                .andExpect(jsonPath("$.items[0].importacionId").value(1001))
                .andExpect(jsonPath("$.items[0].cantidad").value(6000000.0000))
                .andExpect(jsonPath("$.items[0].numeroSerie").doesNotExist())
                .andExpect(jsonPath("$.items[0].marca").doesNotExist())
                .andExpect(jsonPath("$.items[0].modelo").doesNotExist())
                .andExpect(jsonPath("$.items[0].fechaImportacion").value("2025-09-23T00:00:00"))
                .andExpect(jsonPath("$.total").value(1))
                .andExpect(jsonPath("$.pagina").value(1))
                .andExpect(jsonPath("$.tamano").value(20));
    }

    @Test
    void autorizadoConRangoResponde200() throws Exception {
        when(useCase.ejecutar(DESDE, HASTA, "5003971", "A1", "500017", "AZUCAR ESTANDAR",
                null, null, null, 1, 20)).thenReturn(new Pagina<>(List.of(), 1, 1, 20));

        mockMvc.perform(get("/api/v1/operaciones/activos-fijos")
                        .param("desde", DESDE.toString())
                        .param("hasta", HASTA.toString())
                        .param("pedimento", "5003971")
                        .param("clavePedimento", "A1")
                        .param("numeroParte", "500017")
                        .param("descripcion", "AZUCAR ESTANDAR")
                        .with(user(usuarioAutorizado())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.items").isArray())
                .andExpect(jsonPath("$.total").value(1));
    }

    @Test
    void soloDesdeResponde400() throws Exception {
        when(useCase.ejecutar(eq(DESDE), isNull(), isNull(), isNull(), isNull(), isNull(),
                isNull(), isNull(), isNull(), eq(1), eq(20)))
                .thenThrow(new SolicitudInvalidaException("rango incompleto"));

        mockMvc.perform(get("/api/v1/operaciones/activos-fijos")
                        .param("desde", DESDE.toString())
                        .with(user(usuarioAutorizado())))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("SOLICITUD_INVALIDA"));
    }

    @Test
    void soloHastaResponde400() throws Exception {
        when(useCase.ejecutar(isNull(), eq(HASTA), isNull(), isNull(), isNull(), isNull(),
                isNull(), isNull(), isNull(), eq(1), eq(20)))
                .thenThrow(new SolicitudInvalidaException("rango incompleto"));

        mockMvc.perform(get("/api/v1/operaciones/activos-fijos")
                        .param("hasta", HASTA.toString())
                        .with(user(usuarioAutorizado())))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("SOLICITUD_INVALIDA"));
    }

    @Test
    void rangoInvertidoResponde400() throws Exception {
        when(useCase.ejecutar(eq(HASTA), eq(DESDE), isNull(), isNull(), isNull(), isNull(),
                isNull(), isNull(), isNull(), eq(1), eq(20)))
                .thenThrow(new SolicitudInvalidaException("rango inválido"));

        mockMvc.perform(get("/api/v1/operaciones/activos-fijos")
                        .param("desde", HASTA.toString())
                        .param("hasta", DESDE.toString())
                        .with(user(usuarioAutorizado())))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("SOLICITUD_INVALIDA"));
    }

    @Test
    void filtroDemasiadoLargoResponde400() throws Exception {
        String descripcion = "D".repeat(251);
        when(useCase.ejecutar(isNull(), isNull(), isNull(), isNull(), isNull(), eq(descripcion),
                isNull(), isNull(), isNull(), eq(1), eq(20)))
                .thenThrow(new SolicitudInvalidaException("filtro inválido"));

        mockMvc.perform(get("/api/v1/operaciones/activos-fijos")
                        .param("descripcion", descripcion)
                        .with(user(usuarioAutorizado())))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("SOLICITUD_INVALIDA"));
    }

    @Test
    void sinPermisoResponde403() throws Exception {
        mockMvc.perform(get("/api/v1/operaciones/activos-fijos")
                        .with(user("usuario").authorities(new SimpleGrantedAuthority("OTRO_PERMISO"))))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("ACCESO_DENEGADO"));
    }

    @Test
    void errorDeBaseDeDatosResponde503() throws Exception {
        when(useCase.ejecutar(any(), any(), any(), any(), any(), any(), any(), any(), any(),
                anyInt(), anyInt())).thenThrow(new DataAccessResourceFailureException("BD no disponible"));

        mockMvc.perform(get("/api/v1/operaciones/activos-fijos")
                        .param("desde", DESDE.toString())
                        .param("hasta", HASTA.toString())
                        .with(user(usuarioAutorizado())))
                .andExpect(status().isServiceUnavailable())
                .andExpect(jsonPath("$.code").value("DEPENDENCIA_NO_DISPONIBLE"));
    }

    private static ActivoFijo activoEjemplo() {
        return new ActivoFijo(
                new BigDecimal("2001"),
                new BigDecimal("1001"),
                "5003971",
                "A1",
                LocalDateTime.of(2025, 9, 23, 0, 0),
                "500017",
                "AZUCAR ESTANDAR",
                "17019999",
                new BigDecimal("6000000.0000"),
                "KG",
                null,
                null,
                null);
    }

    private static org.springframework.security.core.userdetails.UserDetails usuarioAutorizado() {
        return org.springframework.security.core.userdetails.User.withUsername("usuario")
                .password("n/a")
                .authorities("OPERACIONES_CONSULTAR")
                .build();
    }
}
