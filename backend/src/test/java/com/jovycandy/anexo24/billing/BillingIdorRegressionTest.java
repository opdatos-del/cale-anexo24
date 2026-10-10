package com.jovycandy.anexo24.billing;

import com.jovycandy.anexo24.Anexo24Application;
import com.jovycandy.anexo24.billing.application.usecase.CargarFacturacionUseCase;
import com.jovycandy.anexo24.billing.domain.model.CargaFacturacionDetalle;
import com.jovycandy.anexo24.billing.domain.model.CargaFacturacionResumen;
import com.jovycandy.anexo24.billing.domain.model.EstadoCargaFacturacionPersistida;
import com.jovycandy.anexo24.billing.domain.port.CargaFacturacionRepository;
import com.jovycandy.anexo24.billing.domain.port.PlantillaFacturacionRepository;
import com.jovycandy.anexo24.security.AuthenticatedUserPrincipal;
import com.jovycandy.anexo24.shared.api.Pagina;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.RequestPostProcessor;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.authentication;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(classes = Anexo24Application.class)
@AutoConfigureMockMvc
@ActiveProfiles("test")
class BillingIdorRegressionTest {
    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private CargaFacturacionRepository repository;

    @MockitoBean
    private CargarFacturacionUseCase useCase;

    @MockitoBean
    private PlantillaFacturacionRepository plantillaRepository;

    @Test
    void BILLING_IDOR_REGRESSION_duenoVeDetalleYOtroUsuarioRecibe404() throws Exception {
        when(repository.findById(101L, 7L, 1, 100)).thenReturn(Optional.of(detail()));
        when(repository.findById(101L, 8L, 1, 100)).thenReturn(Optional.empty());

        mockMvc.perform(get("/api/v1/facturacion/cargas/101").with(authenticatedUser(7L)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(101))
                .andExpect(jsonPath("$.preview.filas[0].Documento").value("DOC-A"))
                .andExpect(jsonPath("$.errores[0].mensaje").value("Fila inválida"));

        mockMvc.perform(get("/api/v1/facturacion/cargas/101").with(authenticatedUser(8L)))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.preview").doesNotExist())
                .andExpect(jsonPath("$.errores").doesNotExist());

        verify(repository).findById(101L, 7L, 1, 100);
        verify(repository).findById(101L, 8L, 1, 100);
    }

    @Test
    void BILLING_IDOR_REGRESSION_listaAisladaPorUsuario() throws Exception {
        when(repository.buscar(eq(7L), isNull(EstadoCargaFacturacionPersistida.class), isNull(), isNull(), eq(1), eq(20)))
                .thenReturn(new Pagina<>(List.of(summary(101L, "a.xlsx")), 1, 1, 20));
        when(repository.buscar(eq(8L), isNull(EstadoCargaFacturacionPersistida.class), isNull(), isNull(), eq(1), eq(20)))
                .thenReturn(new Pagina<>(List.of(summary(202L, "b.xlsx")), 1, 1, 20));

        mockMvc.perform(get("/api/v1/facturacion/cargas").with(authenticatedUser(7L)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.items[0].id").value(101))
                .andExpect(jsonPath("$.items[1]").doesNotExist());
        mockMvc.perform(get("/api/v1/facturacion/cargas").with(authenticatedUser(8L)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.items[0].id").value(202))
                .andExpect(jsonPath("$.items[1]").doesNotExist());
    }

    @Test
    void detalleSinAutenticacionResponde401() throws Exception {
        mockMvc.perform(get("/api/v1/facturacion/cargas/101"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void detalleSinPermisoResponde403() throws Exception {
        mockMvc.perform(get("/api/v1/facturacion/cargas/101")
                        .with(user("qa").authorities(new SimpleGrantedAuthority("OPERACIONES_CONSULTAR"))))
                .andExpect(status().isForbidden());
    }

    private RequestPostProcessor authenticatedUser(long userId) {
        var principal = new AuthenticatedUserPrincipal(userId, "usuario" + userId);
        var token = UsernamePasswordAuthenticationToken.authenticated(principal, null,
                List.of(new SimpleGrantedAuthority("FACTURACION_CARGAR")));
        return authentication(token);
    }

    private CargaFacturacionDetalle detail() {
        return new CargaFacturacionDetalle(101L, "a.xlsx", "hash-a", "PREVISUALIZADA", 1, 1, 0,
                List.of(new CargaFacturacionDetalle.Fila("FACTURAS", 2, Map.of("Documento", "DOC-A"))),
                List.of(new com.jovycandy.anexo24.billing.domain.model.ArchivoFacturacion.Error(
                        "FACTURAS", 2, "Documento", null, "ERR-1", "Fila inválida")));
    }

    private CargaFacturacionResumen summary(long id, String archivo) {
        return new CargaFacturacionResumen(id, archivo, "hash-" + id, LocalDateTime.of(2026, 10, 10, 12, 0),
                EstadoCargaFacturacionPersistida.PREVISUALIZADA, 1, 1, 0);
    }
}
