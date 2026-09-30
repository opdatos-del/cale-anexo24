package com.jovycandy.anexo24.catalogs.imports.api;

import com.jovycandy.anexo24.Anexo24Application;
import com.jovycandy.anexo24.catalogs.imports.api.controller.CatalogImportController;
import com.jovycandy.anexo24.catalogs.imports.application.command.ExcelCatalogImportParser;
import com.jovycandy.anexo24.catalogs.imports.application.usecase.CargarCatalogoUseCase;
import com.jovycandy.anexo24.catalogs.imports.domain.model.CatalogImportDetalle;
import com.jovycandy.anexo24.catalogs.imports.domain.model.CatalogImportError;
import com.jovycandy.anexo24.catalogs.imports.domain.model.CatalogImportFila;
import com.jovycandy.anexo24.catalogs.imports.domain.port.CatalogImportRepository;
import com.jovycandy.anexo24.catalogs.imports.domain.model.CatalogImportType;
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
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/** Verifica la frontera 401/403/autorizado y el endpoint separado de errores. */
@SpringBootTest(classes = Anexo24Application.class)
@AutoConfigureMockMvc
@ActiveProfiles("test")
class CatalogImportControllerSecurityTest {
    @Autowired private MockMvc mockMvc;
    @MockitoBean private CatalogImportRepository repository;
    @MockitoBean private ExcelCatalogImportParser parser;
    @MockitoBean private CargarCatalogoUseCase useCase;

    @Test
    void uploadSinAutenticacionResponde401() throws Exception {
        mockMvc.perform(multipart("/api/v1/catalogos/importaciones/materiales")
                        .file("archivo", new byte[]{1}))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void usuarioSinPermisoResponde403() throws Exception {
        mockMvc.perform(get("/api/v1/catalogos/importaciones/materiales/1")
                        .with(user("consulta")))
                .andExpect(status().isForbidden());
    }

    @Test
    void usuarioConPermisoPuedeConsultarDetalle() throws Exception {
        when(repository.findById(CatalogImportType.MATERIAL, 1, 1, 100)).thenReturn(Optional.of(detail()));

        mockMvc.perform(get("/api/v1/catalogos/importaciones/materiales/1")
                        .with(user("cargador").authorities(new SimpleGrantedAuthority("MATERIALES_CARGAR"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1));
    }

    @Test
    void endpointDeErroresDevuelveSóloErroresPersistidos() throws Exception {
        when(repository.findById(CatalogImportType.PRODUCTO, 2, 1, 1)).thenReturn(Optional.of(detailProducto()));
        when(repository.findErrors(CatalogImportType.PRODUCTO, 2, 1, 20)).thenReturn(List.of(
                new CatalogImportError("CATALOGO", 2, "NOMBRE", "no almacenado", "NOMBRE_CORTO", "Nombre inválido")));

        mockMvc.perform(get("/api/v1/catalogos/importaciones/productos/2/errores")
                        .param("tamano", "20")
                        .with(user("cargador").authorities(new SimpleGrantedAuthority("PRODUCTOS_CARGAR"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].codigo").value("NOMBRE_CORTO"))
                .andExpect(jsonPath("$[0].fila").value(2))
                .andExpect(jsonPath("$.id").doesNotExist());
        verify(repository).findErrors(CatalogImportType.PRODUCTO, 2, 1, 20);
    }

    private CatalogImportDetalle detail() {
        return new CatalogImportDetalle(1, CatalogImportType.MATERIAL, "m.xlsx", "a".repeat(64),
                "PREVISUALIZADA", 1, 1, 0, "V1", List.of(new CatalogImportFila("CATALOGO", 2,
                Map.of("ClaveMaterial", "MAT001"))), 1, List.of());
    }

    private CatalogImportDetalle detailProducto() {
        return new CatalogImportDetalle(2, CatalogImportType.PRODUCTO, "p.xlsx", "b".repeat(64),
                "CON_ERRORES", 1, 0, 1, "V1", List.of(), 0, List.of());
    }
}
