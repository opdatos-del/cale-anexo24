package com.jovycandy.anexo24.operations.pediments;

import com.jovycandy.anexo24.Anexo24Application;
import com.jovycandy.anexo24.operations.pediments.application.command.ExcelPedimentoParser;
import com.jovycandy.anexo24.operations.pediments.application.usecase.CargarPedimentosUseCase;
import com.jovycandy.anexo24.operations.pediments.application.usecase.ConfirmarCargaPedimentoUseCase;
import com.jovycandy.anexo24.operations.pediments.domain.model.ConfirmacionPedimento;
import com.jovycandy.anexo24.operations.pediments.domain.port.CargaPedimentoRepository;
import com.jovycandy.anexo24.shared.exception.ConfirmacionNoProcesableException;
import com.jovycandy.anexo24.shared.exception.EstadoIncompatibleException;
import com.jovycandy.anexo24.shared.exception.RecursoNoEncontradoException;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDateTime;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/** Contrato HTTP y de seguridad de la confirmación autoritativa de pedimentos. */
@SpringBootTest(classes = Anexo24Application.class)
@AutoConfigureMockMvc
@ActiveProfiles("test")
class PedimentoConfirmacionSecurityTest {

    private static final String URL = "/api/v1/operaciones/pedimentos/cargas/1/confirmacion";

    @Autowired MockMvc mockMvc;
    @MockitoBean ExcelPedimentoParser parser;
    @MockitoBean CargarPedimentosUseCase cargarUseCase;
    @MockitoBean ConfirmarCargaPedimentoUseCase useCase;
    @MockitoBean CargaPedimentoRepository repository;

    @Test
    void sinAutenticacionRecibe401() throws Exception {
        mockMvc.perform(post(URL)).andExpect(status().isUnauthorized());
    }

    @Test
    void autenticadoSinPermisoRecibe403() throws Exception {
        mockMvc.perform(post(URL).with(user("consulta"))).andExpect(status().isForbidden());
    }

    @Test
    void confirmacionExitosaDevuelve200() throws Exception {
        when(useCase.ejecutar(eq(1L), any())).thenReturn(new ConfirmacionPedimento(1L, "CONFIRMADA",
                ConfirmacionPedimento.CONFIRMADO, 1, 1, 2, LocalDateTime.parse("2026-05-28T10:15:30")));

        mockMvc.perform(post(URL).with(permiso()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.resultado").value("CONFIRMED"))
                .andExpect(jsonPath("$.estado").value("CONFIRMADA"))
                .andExpect(jsonPath("$.tipoOperacion").value(1))
                .andExpect(jsonPath("$.operacionesProcesadas").value(1))
                .andExpect(jsonPath("$.partidasProcesadas").value(2));
    }

    @Test
    void alreadyConfirmedDevuelve200YNoFalla() throws Exception {
        when(useCase.ejecutar(eq(1L), any())).thenReturn(new ConfirmacionPedimento(1L, "CONFIRMADA",
                ConfirmacionPedimento.YA_CONFIRMADO, 1, 0, 0, LocalDateTime.parse("2026-05-28T10:15:30")));

        mockMvc.perform(post(URL).with(permiso()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.resultado").value("ALREADY_CONFIRMED"));
    }

    @Test
    void cargaMixtaExponeTipoOperacionNulo() throws Exception {
        when(useCase.ejecutar(eq(1L), any())).thenReturn(new ConfirmacionPedimento(1L, "CONFIRMADA",
                ConfirmacionPedimento.CONFIRMADO, null, 2, 2, LocalDateTime.parse("2026-05-28T10:15:30")));

        mockMvc.perform(post(URL).with(permiso()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.tipoOperacion").doesNotExist());
    }

    @Test
    void cargaInexistenteDevuelve404() throws Exception {
        when(useCase.ejecutar(eq(1L), any())).thenThrow(new RecursoNoEncontradoException());

        mockMvc.perform(post(URL).with(permiso())).andExpect(status().isNotFound());
    }

    @Test
    void duplicadoYBloqueoDevuelven409() throws Exception {
        when(useCase.ejecutar(eq(1L), any())).thenThrow(new EstadoIncompatibleException());

        mockMvc.perform(post(URL).with(permiso())).andExpect(status().isConflict());
    }

    @Test
    void cargaNoProcesableDevuelve422() throws Exception {
        when(useCase.ejecutar(eq(1L), any())).thenThrow(new ConfirmacionNoProcesableException());

        mockMvc.perform(post(URL).with(permiso())).andExpect(status().isUnprocessableEntity());
    }

    private static org.springframework.test.web.servlet.request.RequestPostProcessor permiso() {
        return user("admin").authorities(new SimpleGrantedAuthority("PEDIMENTOS_CONFIRMAR"));
    }
}
