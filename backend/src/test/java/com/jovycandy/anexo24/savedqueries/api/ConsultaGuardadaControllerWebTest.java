package com.jovycandy.anexo24.savedqueries.api;

import com.jovycandy.anexo24.Anexo24Application;
import com.jovycandy.anexo24.savedqueries.application.ConsultaGuardadaService;
import com.jovycandy.anexo24.savedqueries.domain.model.ConsultaGuardada;
import com.jovycandy.anexo24.savedqueries.domain.model.ConsultaGuardadaAlcance;
import com.jovycandy.anexo24.shared.exception.RecursoNoEncontradoException;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDateTime;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(classes = Anexo24Application.class)
@AutoConfigureMockMvc
@ActiveProfiles("test")
class ConsultaGuardadaControllerWebTest {
    private static final String ENDPOINT = "/api/v1/consultas-guardadas";
    @Autowired private MockMvc mockMvc;
    @MockitoBean private ConsultaGuardadaService service;

    @Test
    void requiereAutenticacion() throws Exception {
        mockMvc.perform(get(ENDPOINT)).andExpect(status().isUnauthorized());
        mockMvc.perform(post(ENDPOINT).contentType(MediaType.APPLICATION_JSON).content(payload()))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void exponeCrudSoloComoMetadatosDeFiltros() throws Exception {
        ConsultaGuardada entity = entity();
        when(service.listar(ConsultaGuardadaAlcance.ENTRADAS)).thenReturn(List.of(entity));
        when(service.crear(anyString(), isNull(String.class), eq(ConsultaGuardadaAlcance.ENTRADAS), any(), any()))
                .thenReturn(entity);
        when(service.actualizar(eq(7L), anyString(), isNull(String.class), eq(ConsultaGuardadaAlcance.ENTRADAS), any(), any()))
                .thenReturn(entity);

        mockMvc.perform(get(ENDPOINT).param("alcance", "ENTRADAS").with(user("usuario")))
                .andExpect(status().isOk()).andExpect(jsonPath("$[0].id").value(7))
                .andExpect(jsonPath("$[0].criterios.from").value("2026-10-01"));
        mockMvc.perform(post(ENDPOINT).with(user("usuario")).contentType(MediaType.APPLICATION_JSON).content(payload()))
                .andExpect(status().isCreated()).andExpect(jsonPath("$.alcance").value("ENTRADAS"));
        mockMvc.perform(put(ENDPOINT + "/7").with(user("usuario")).contentType(MediaType.APPLICATION_JSON).content(payload()))
                .andExpect(status().isOk());
        mockMvc.perform(delete(ENDPOINT + "/7").with(user("usuario"))).andExpect(status().isNoContent());
    }

    @Test
    void ocultaIdAjenoComoNotFound() throws Exception {
        doThrow(new RecursoNoEncontradoException()).when(service)
                .actualizar(eq(999L), anyString(), isNull(String.class), eq(ConsultaGuardadaAlcance.ENTRADAS), any(), any());
        mockMvc.perform(put(ENDPOINT + "/999").with(user("usuario")).contentType(MediaType.APPLICATION_JSON).content(payload()))
                .andExpect(status().isNotFound());
    }

    private String payload() {
        return "{\"nombre\":\"Octubre\",\"descripcion\":null,\"alcance\":\"ENTRADAS\",\"criterios\":{\"from\":\"2026-10-01\",\"to\":\"2026-10-31\"}}";
    }

    private ConsultaGuardada entity() {
        LocalDateTime now = LocalDateTime.parse("2026-10-01T00:00:00");
        return new ConsultaGuardada(7L, "Octubre", null, ConsultaGuardadaAlcance.ENTRADAS,
                "{\"from\":\"2026-10-01\",\"to\":\"2026-10-31\"}", now, now);
    }
}
