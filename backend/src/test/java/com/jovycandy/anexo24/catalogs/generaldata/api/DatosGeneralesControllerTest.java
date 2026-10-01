package com.jovycandy.anexo24.catalogs.generaldata.api;

import com.jovycandy.anexo24.Anexo24Application;
import com.jovycandy.anexo24.catalogs.generaldata.application.query.ObtenerDatosGeneralesUseCase;
import com.jovycandy.anexo24.catalogs.generaldata.domain.model.DatosGenerales;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.Optional;

import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/** Pruebas de seguridad y consulta de datos generales. */
@SpringBootTest(classes = Anexo24Application.class)
@AutoConfigureMockMvc
@ActiveProfiles("test")
class DatosGeneralesControllerTest {
    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private ObtenerDatosGeneralesUseCase useCase;

    @Test
    void sinTokenResponde401() throws Exception {
        mockMvc.perform(get("/api/v1/catalogos/datos-generales"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void sinPermisoResponde403() throws Exception {
        mockMvc.perform(get("/api/v1/catalogos/datos-generales")
                        .with(user("usuario").authorities(new SimpleGrantedAuthority("REPORTES_GENERAR"))))
                .andExpect(status().isForbidden());
    }

    @Test
    void fuenteVaciaResponde204SinCuerpo() throws Exception {
        when(useCase.ejecutar()).thenReturn(Optional.empty());

        mockMvc.perform(get("/api/v1/catalogos/datos-generales")
                        .with(user("usuario").authorities(new SimpleGrantedAuthority("CATALOGOS_AUX_CONSULTAR"))))
                .andExpect(status().isNoContent())
                .andExpect(jsonPath("$").doesNotExist());
    }

    @Test
    void autorizadoResponde200SinExponerCamposNoContratados() throws Exception {
        when(useCase.ejecutar()).thenReturn(Optional.of(
                new DatosGenerales("Empresa", "RFC", "IMMEX", "Domicilio")));

        mockMvc.perform(get("/api/v1/catalogos/datos-generales")
                        .with(user("usuario").authorities(new SimpleGrantedAuthority("CATALOGOS_AUX_CONSULTAR"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.razonSocial").value("Empresa"))
                .andExpect(jsonPath("$.rfc").value("RFC"))
                .andExpect(jsonPath("$.registroImmex").value("IMMEX"))
                .andExpect(jsonPath("$.domicilioFiscal").value("Domicilio"));
    }
}
