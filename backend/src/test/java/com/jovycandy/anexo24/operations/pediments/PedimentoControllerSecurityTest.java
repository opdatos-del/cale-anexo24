package com.jovycandy.anexo24.operations.pediments;

import com.jovycandy.anexo24.Anexo24Application;
import com.jovycandy.anexo24.operations.pediments.application.command.ExcelPedimentoParser;
import com.jovycandy.anexo24.operations.pediments.application.usecase.CargarPedimentosUseCase;
import com.jovycandy.anexo24.operations.pediments.domain.port.CargaPedimentoRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.Optional;

import static org.mockito.Mockito.when;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/** Verifica el permiso específico de carga de pedimentos. */
@SpringBootTest(classes = Anexo24Application.class)
@AutoConfigureMockMvc
@ActiveProfiles("test")
class PedimentoControllerSecurityTest {
    @Autowired MockMvc mockMvc;
    @MockitoBean ExcelPedimentoParser parser;
    @MockitoBean CargarPedimentosUseCase useCase;
    @MockitoBean CargaPedimentoRepository repository;

    @Test
    void sinAutenticacionRecibe401() throws Exception {
        mockMvc.perform(get("/api/v1/operaciones/pedimentos/cargas/1"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void autenticadoSinPermisoRecibe403() throws Exception {
        mockMvc.perform(get("/api/v1/operaciones/pedimentos/cargas/1").with(user("consulta")))
                .andExpect(status().isForbidden());
    }

    @Test
    void permisoPedimentosAutorizaLaConsulta() throws Exception {
        when(repository.findById(1, 1, 100)).thenReturn(Optional.empty());
        mockMvc.perform(get("/api/v1/operaciones/pedimentos/cargas/1")
                        .with(user("admin").authorities(new SimpleGrantedAuthority("PEDIMENTOS_CARGAR"))))
                .andExpect(status().isNotFound());
    }
}
