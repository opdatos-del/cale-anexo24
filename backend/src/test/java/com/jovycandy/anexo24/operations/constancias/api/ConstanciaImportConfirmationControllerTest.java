package com.jovycandy.anexo24.operations.constancias.api;

import com.jovycandy.anexo24.Anexo24Application;
import com.jovycandy.anexo24.catalogs.imports.application.command.ExcelCatalogImportParser;
import com.jovycandy.anexo24.catalogs.imports.application.usecase.CargarCatalogoUseCase;
import com.jovycandy.anexo24.catalogs.imports.domain.port.CatalogImportRepository;
import com.jovycandy.anexo24.operations.constancias.application.usecase.ConfirmarCargaConstanciaUseCase;
import com.jovycandy.anexo24.operations.constancias.domain.model.ConfirmacionCargaConstancia;
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

import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/** Contrato HTTP y autorizacion para la confirmacion de constancias. */
@SpringBootTest(classes = Anexo24Application.class)
@AutoConfigureMockMvc
@ActiveProfiles("test")
class ConstanciaImportConfirmationControllerTest {

    private static final String URL = "/api/v1/operaciones/constancias/1/confirmacion";

    @Autowired private MockMvc mockMvc;
    @MockitoBean private CatalogImportRepository repository;
    @MockitoBean private ExcelCatalogImportParser parser;
    @MockitoBean private CargarCatalogoUseCase cargarUseCase;
    @MockitoBean private ConfirmarCargaConstanciaUseCase useCase;

    @Test
    void sinAutenticacionResponde401() throws Exception {
        mockMvc.perform(post(URL)).andExpect(status().isUnauthorized());
    }

    @Test
    void sinPermisoDeConfirmacionResponde403() throws Exception {
        mockMvc.perform(post(URL).with(user("cargador")
                        .authorities(new SimpleGrantedAuthority("CONSTANCIAS_CARGAR"))))
                .andExpect(status().isForbidden());
    }

    @Test
    void confirmacionDevuelveResumenEstable() throws Exception {
        when(useCase.ejecutar(1L)).thenReturn(new ConfirmacionCargaConstancia(1L, "CONFIRMADA", 2, 2, 0,
                LocalDateTime.parse("2026-10-05T09:30:00"), "CONFIRMED"));

        mockMvc.perform(post(URL).with(permiso()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.cargaId").value(1))
                .andExpect(jsonPath("$.estado").value("CONFIRMADA"))
                .andExpect(jsonPath("$.totalFilas").value(2))
                .andExpect(jsonPath("$.filasConError").value(0))
                .andExpect(jsonPath("$.confirmadaEn").exists())
                .andExpect(jsonPath("$.resultado").value("CONFIRMED"));
    }

    @Test
    void idInvalidoDevuelve400() throws Exception {
        mockMvc.perform(post("/api/v1/operaciones/constancias/0/confirmacion").with(permiso()))
                .andExpect(status().isBadRequest());
    }

    @Test
    void etapaOcupadaDevuelve409() throws Exception {
        when(useCase.ejecutar(1L)).thenThrow(new EstadoIncompatibleException());
        mockMvc.perform(post(URL).with(permiso())).andExpect(status().isConflict());
    }

    @Test
    void cargaNoProcesableDevuelve422() throws Exception {
        when(useCase.ejecutar(1L)).thenThrow(new ConfirmacionNoProcesableException());
        mockMvc.perform(post(URL).with(permiso())).andExpect(status().isUnprocessableEntity());
    }

    @Test
    void cargaNoEncontradaDevuelve404() throws Exception {
        when(useCase.ejecutar(1L)).thenThrow(new RecursoNoEncontradoException());
        mockMvc.perform(post(URL).with(permiso())).andExpect(status().isNotFound());
    }

    private static org.springframework.test.web.servlet.request.RequestPostProcessor permiso() {
        return user("confirmador").authorities(new SimpleGrantedAuthority("CONSTANCIAS_CONFIRMAR"));
    }
}
