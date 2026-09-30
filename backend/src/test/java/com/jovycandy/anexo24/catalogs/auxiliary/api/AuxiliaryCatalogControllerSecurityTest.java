package com.jovycandy.anexo24.catalogs.auxiliary.api;

import com.jovycandy.anexo24.Anexo24Application;
import com.jovycandy.anexo24.catalogs.auxiliary.categories.application.query.ListarCategoriasUseCase;
import com.jovycandy.anexo24.catalogs.auxiliary.materialtypes.application.query.ListarTiposMaterialUseCase;
import com.jovycandy.anexo24.catalogs.auxiliary.units.application.query.ListarUnidadesUseCase;
import com.jovycandy.anexo24.catalogs.auxiliary.warehouses.application.query.ListarAlmacenesUseCase;
import com.jovycandy.anexo24.shared.api.Pagina;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;

import org.springframework.security.core.authority.SimpleGrantedAuthority;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/** Verifica el permiso común y los cuatro contratos HTTP auxiliares. */
@SpringBootTest(classes = Anexo24Application.class)
@AutoConfigureMockMvc
@ActiveProfiles("test")
class AuxiliaryCatalogControllerSecurityTest {
    @Autowired MockMvc mockMvc;
    @MockitoBean ListarUnidadesUseCase unidades;
    @MockitoBean ListarTiposMaterialUseCase tiposMaterial;
    @MockitoBean ListarAlmacenesUseCase almacenes;
    @MockitoBean ListarCategoriasUseCase categorias;

    @Test
    void todosLosEndpointsRequierenElPermisoAuxiliar() throws Exception {
        for (String path : List.of("unidades", "tipos-material", "almacenes", "categorias")) {
            mockMvc.perform(get("/api/v1/catalogos/" + path))
                    .andExpect(status().isUnauthorized());
        }
    }

    @Test
    void usuarioAutenticadoSinPermisoRecibe403() throws Exception {
        mockMvc.perform(get("/api/v1/catalogos/unidades").with(user("sin-permiso")))
                .andExpect(status().isForbidden());
    }

    @Test
    void permisoAuxiliarPermiteConsultarLosCuatroContratos() throws Exception {
        when(unidades.ejecutar(null, 1, 20)).thenReturn(new Pagina<>(List.of(), 0, 1, 20));
        when(tiposMaterial.ejecutar(null, 1, 20)).thenReturn(new Pagina<>(List.of(), 0, 1, 20));
        when(almacenes.ejecutar(null, 1, 20)).thenReturn(new Pagina<>(List.of(), 0, 1, 20));
        when(categorias.ejecutar(null, 1, 20)).thenReturn(new Pagina<>(List.of(), 0, 1, 20));

        for (String path : List.of("unidades", "tipos-material", "almacenes", "categorias")) {
            mockMvc.perform(get("/api/v1/catalogos/" + path)
                            .with(user("consulta").authorities(new SimpleGrantedAuthority("CATALOGOS_AUX_CONSULTAR"))))
                    .andExpect(status().isOk());
        }
    }
}
