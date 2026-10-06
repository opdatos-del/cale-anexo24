package com.jovycandy.anexo24.operations.constancias.api;

import com.jovycandy.anexo24.Anexo24Application;
import com.jovycandy.anexo24.catalogs.imports.application.command.ExcelCatalogImportParser;
import com.jovycandy.anexo24.catalogs.imports.application.usecase.CargarCatalogoUseCase;
import com.jovycandy.anexo24.catalogs.imports.domain.model.CatalogImportDetalle;
import com.jovycandy.anexo24.catalogs.imports.domain.model.CatalogImportError;
import com.jovycandy.anexo24.catalogs.imports.domain.model.CatalogImportFila;
import com.jovycandy.anexo24.catalogs.imports.domain.model.CatalogImportType;
import com.jovycandy.anexo24.catalogs.imports.domain.port.CatalogImportRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/** Frontera 401/403/autorizado y endpoints de la operacion de constancias (nunca bajo /catalogos). */
@SpringBootTest(classes = Anexo24Application.class)
@AutoConfigureMockMvc
@ActiveProfiles("test")
class ConstanciaImportControllerTest {
    @Autowired private MockMvc mockMvc;
    @MockitoBean private CatalogImportRepository repository;
    @MockitoBean private ExcelCatalogImportParser parser;
    @MockitoBean private CargarCatalogoUseCase useCase;

    @Test
    void detalleSinAutenticacionResponde401() throws Exception {
        mockMvc.perform(get("/api/v1/operaciones/constancias/importaciones/1"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void usuarioSinPermisoResponde403() throws Exception {
        mockMvc.perform(get("/api/v1/operaciones/constancias/importaciones/1").with(user("consulta")))
                .andExpect(status().isForbidden());
    }

    @Test
    void usuarioConPermisoPuedeConsultarDetalle() throws Exception {
        when(repository.findById(CatalogImportType.CONSTANCIA, 1, 1, 100)).thenReturn(Optional.of(detail()));

        mockMvc.perform(get("/api/v1/operaciones/constancias/importaciones/1")
                        .with(user("cargador").authorities(new SimpleGrantedAuthority("CONSTANCIAS_CARGAR"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.tipo").value("CONSTANCIA"));
    }

    @Test
    void endpointDeErroresDevuelveSoloErroresPersistidos() throws Exception {
        when(repository.findById(CatalogImportType.CONSTANCIA, 2, 1, 1)).thenReturn(Optional.of(detail()));
        when(repository.findErrors(CatalogImportType.CONSTANCIA, 2, 1, 20)).thenReturn(List.of(
                new CatalogImportError("A", 2, "Linea", "no almacenado", "LIN_CONSTANCIA_INVALIDO", "Linea invalida")));

        mockMvc.perform(get("/api/v1/operaciones/constancias/importaciones/2/errores")
                        .param("tamano", "20")
                        .with(user("cargador").authorities(new SimpleGrantedAuthority("CONSTANCIAS_CARGAR"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].codigo").value("LIN_CONSTANCIA_INVALIDO"))
                .andExpect(jsonPath("$[0].fila").value(2));
        verify(repository).findErrors(CatalogImportType.CONSTANCIA, 2, 1, 20);
    }

    private CatalogImportDetalle detail() {
        return new CatalogImportDetalle(1, CatalogImportType.CONSTANCIA, "constancias.xlsx", "a".repeat(64),
                "PREVISUALIZADA", 1, 1, 0, "CONSTANCIA-V1", List.of(new CatalogImportFila("A", 2,
                Map.of("NUMERODEFOLIO", "CONST-1"))), 1, List.of());
    }
}
