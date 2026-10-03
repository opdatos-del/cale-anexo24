package com.jovycandy.anexo24.catalogs.imports.api;

import com.jovycandy.anexo24.Anexo24Application;
import com.jovycandy.anexo24.catalogs.imports.application.command.ExcelCatalogImportParser;
import com.jovycandy.anexo24.catalogs.imports.application.usecase.CargarCatalogoUseCase;
import com.jovycandy.anexo24.catalogs.imports.application.usecase.ConfirmarCargaMaterialUseCase;
import com.jovycandy.anexo24.catalogs.imports.domain.model.ConfirmacionCargaMaterial;
import com.jovycandy.anexo24.catalogs.imports.domain.port.CatalogImportRepository;
import com.jovycandy.anexo24.shared.exception.ConfirmacionNoProcesableException;
import com.jovycandy.anexo24.shared.exception.EstadoIncompatibleException;
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

/** Contrato HTTP y autorización para la confirmación de materiales. */
@SpringBootTest(classes = Anexo24Application.class)
@AutoConfigureMockMvc
@ActiveProfiles("test")
class MaterialImportConfirmationControllerTest {

    private static final String URL = "/api/v1/catalogos/importaciones/materiales/1/confirmacion";

    @Autowired private MockMvc mockMvc;
    @MockitoBean private CatalogImportRepository repository;
    @MockitoBean private ExcelCatalogImportParser parser;
    @MockitoBean private CargarCatalogoUseCase cargarUseCase;
    @MockitoBean private ConfirmarCargaMaterialUseCase useCase;

    @Test
    void sinAutenticacionResponde401() throws Exception {
        mockMvc.perform(post(URL)).andExpect(status().isUnauthorized());
    }

    @Test
    void sinPermisoDeConfirmacionResponde403() throws Exception {
        mockMvc.perform(post(URL).with(user("cargador")
                        .authorities(new SimpleGrantedAuthority("MATERIALES_CARGAR"))))
                .andExpect(status().isForbidden());
    }

    @Test
    void confirmacionDevuelveResumenEstable() throws Exception {
        when(useCase.ejecutar(1L)).thenReturn(new ConfirmacionCargaMaterial(1L, "CONFIRMADA", 2,
                2, 0, LocalDateTime.parse("2026-10-03T09:30:00"), "CONFIRMED"));

        mockMvc.perform(post(URL).with(permiso()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.cargaId").value(1))
                .andExpect(jsonPath("$.estado").value("CONFIRMADA"))
                .andExpect(jsonPath("$.totalFilas").value(2))
                .andExpect(jsonPath("$.filasValidas").value(2))
                .andExpect(jsonPath("$.filasConError").value(0))
                .andExpect(jsonPath("$.confirmadaEn").exists())
                .andExpect(jsonPath("$.resultado").value("CONFIRMED"));
    }

    @Test
    void erroresLegacyPersistidosSonResultadoDeNegocio() throws Exception {
        when(useCase.ejecutar(1L)).thenReturn(new ConfirmacionCargaMaterial(1L, "CON_ERRORES", 2,
                1, 1, null, "BUSINESS_ERRORS"));

        mockMvc.perform(post(URL).with(permiso()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.estado").value("CON_ERRORES"))
                .andExpect(jsonPath("$.filasConError").value(1))
                .andExpect(jsonPath("$.confirmadaEn").doesNotExist())
                .andExpect(jsonPath("$.resultado").value("BUSINESS_ERRORS"));
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

    private static org.springframework.test.web.servlet.request.RequestPostProcessor permiso() {
        return user("confirmador").authorities(new SimpleGrantedAuthority("MATERIALES_CONFIRMAR"));
    }
}
