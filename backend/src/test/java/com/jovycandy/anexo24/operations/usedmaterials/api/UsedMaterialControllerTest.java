package com.jovycandy.anexo24.operations.usedmaterials.api;

import com.jovycandy.anexo24.Anexo24Application;
import com.jovycandy.anexo24.operations.usedmaterials.application.query.ListarMaterialesUtilizadosUseCase;
import com.jovycandy.anexo24.operations.usedmaterials.domain.model.MaterialUtilizado;
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
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/** Pruebas web y de autorización de Materiales Utilizados. */
@SpringBootTest(classes = Anexo24Application.class)
@AutoConfigureMockMvc
@ActiveProfiles("test")
class UsedMaterialControllerTest {

    private static final LocalDate DESDE = LocalDate.of(2025, 10, 31);
    private static final LocalDate HASTA = LocalDate.of(2026, 8, 18);

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private ListarMaterialesUtilizadosUseCase useCase;

    @Test
    void sinTokenResponde401() throws Exception {
        mockMvc.perform(get("/api/v1/operaciones/materiales-utilizados")
                        .param("desde", DESDE.toString())
                        .param("hasta", HASTA.toString()))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("AUTENTICACION_REQUERIDA"));
    }

    @Test
    void fechaNoIsoYSolicitudInvalidaResponden400() throws Exception {
        mockMvc.perform(get("/api/v1/operaciones/materiales-utilizados")
                        .param("desde", "no-es-fecha")
                        .param("hasta", HASTA.toString())
                        .with(user(usuarioAutorizado())))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("SOLICITUD_INVALIDA"));

        when(useCase.ejecutar(any(), any(), any(), any(), any(), any(), anyInt(), anyInt()))
                .thenThrow(new SolicitudInvalidaException("filtro inválido"));
        mockMvc.perform(get("/api/v1/operaciones/materiales-utilizados")
                        .param("desde", DESDE.toString())
                        .param("hasta", HASTA.toString())
                        .param("material", "M".repeat(51))
                        .with(user(usuarioAutorizado())))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("SOLICITUD_INVALIDA"));
    }

    @Test
    void sinPermisoResponde403() throws Exception {
        mockMvc.perform(get("/api/v1/operaciones/materiales-utilizados")
                        .param("desde", DESDE.toString())
                        .param("hasta", HASTA.toString())
                        .with(user("usuario").authorities(new SimpleGrantedAuthority("OTRO_PERMISO"))))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("ACCESO_DENEGADO"));
    }

    @Test
    void autorizadoResponde200ConShapeYSerializaNulos() throws Exception {
        MaterialUtilizado fila = new MaterialUtilizado(390L, new BigDecimal("1001"),
                new BigDecimal("2001"), new BigDecimal("3024"), new BigDecimal("4124"),
                "190-1562-5003971", "190-1562-5001284", "500017", "AZUCAR ESTANDAR",
                "300861", "CHERRY SLICES", new BigDecimal("10.5000"),
                new BigDecimal("0.5000"), null, new BigDecimal("11.0000"), "KG",
                LocalDateTime.of(2025, 12, 1, 0, 0));
        when(useCase.ejecutar(DESDE, HASTA, "500017", "300861", "190-1562-5001284", "F4", 1, 20))
                .thenReturn(new Pagina<>(List.of(fila), 1, 1, 20));

        mockMvc.perform(get("/api/v1/operaciones/materiales-utilizados")
                        .param("desde", DESDE.toString())
                        .param("hasta", HASTA.toString())
                        .param("material", "500017")
                        .param("producto", "300861")
                        .param("pedimentoSalida", "190-1562-5001284")
                        .param("clavePedimentoSalida", "F4")
                        .with(user(usuarioAutorizado())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.items").isArray())
                .andExpect(jsonPath("$.items[0].descargaId").value(390))
                .andExpect(jsonPath("$.items[0].entradaId").value(1001))
                .andExpect(jsonPath("$.items[0].cantidadDesperdicio").doesNotExist())
                .andExpect(jsonPath("$.items[0].fecha").value("2025-12-01T00:00:00"))
                .andExpect(jsonPath("$.total").value(1))
                .andExpect(jsonPath("$.pagina").value(1))
                .andExpect(jsonPath("$.tamano").value(20));
    }

    @Test
    void errorDeBaseDeDatosResponde503() throws Exception {
        when(useCase.ejecutar(any(), any(), any(), any(), any(), any(), anyInt(), anyInt()))
                .thenThrow(new DataAccessResourceFailureException("BD no disponible"));

        mockMvc.perform(get("/api/v1/operaciones/materiales-utilizados")
                        .param("desde", DESDE.toString())
                        .param("hasta", HASTA.toString())
                        .with(user(usuarioAutorizado())))
                .andExpect(status().isServiceUnavailable())
                .andExpect(jsonPath("$.code").value("DEPENDENCIA_NO_DISPONIBLE"));
    }

    private static org.springframework.security.core.userdetails.UserDetails usuarioAutorizado() {
        return org.springframework.security.core.userdetails.User.withUsername("usuario")
                .password("n/a")
                .authorities("OPERACIONES_CONSULTAR")
                .build();
    }
}
