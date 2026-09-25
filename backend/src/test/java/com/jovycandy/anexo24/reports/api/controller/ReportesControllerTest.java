package com.jovycandy.anexo24.reports.api.controller;

import com.jovycandy.anexo24.Anexo24Application;
import com.jovycandy.anexo24.operations.entries.api.dto.EntradaLineaDto;
import com.jovycandy.anexo24.reports.application.query.ConsultarReportesUseCase;
import com.jovycandy.anexo24.shared.api.Pagina;
import com.jovycandy.anexo24.shared.exception.SolicitudInvalidaException;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import java.io.ByteArrayInputStream;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/** Pruebas web de permisos, rango y exportación de Reportes V1. */
@SpringBootTest(classes = Anexo24Application.class)
@AutoConfigureMockMvc
@ActiveProfiles("test")
class ReportesControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private ConsultarReportesUseCase consultarReportesUseCase;

    @Test
    void listarEntradasExigePermisoDeReportes() throws Exception {
        mockMvc.perform(get("/api/v1/reportes/entradas")
                        .param("desde", "2026-01-01")
                        .param("hasta", "2026-01-31")
                        .with(user("usuario").authorities(new SimpleGrantedAuthority("OPERACIONES_CONSULTAR"))))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("ACCESO_DENEGADO"));
    }

    @Test
    void rangoFaltanteResponde400Controlado() throws Exception {
        mockMvc.perform(get("/api/v1/reportes/entradas")
                        .param("hasta", "2026-01-31")
                        .with(user("usuario").authorities(new SimpleGrantedAuthority("REPORTES_GENERAR"))))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("SOLICITUD_INVALIDA"))
                .andExpect(jsonPath("$.correlationId").exists());
    }

    @Test
    void rangoInvertidoResponde400Controlado() throws Exception {
        when(consultarReportesUseCase.entradas(any(), any(), any(), any(), any(), any(), anyInt(), anyInt()))
                .thenThrow(new SolicitudInvalidaException("rango inválido"));

        mockMvc.perform(get("/api/v1/reportes/entradas")
                        .param("desde", "2026-02-01")
                        .param("hasta", "2026-01-31")
                        .with(user("usuario").authorities(new SimpleGrantedAuthority("REPORTES_GENERAR"))))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("SOLICITUD_INVALIDA"))
                .andExpect(jsonPath("$.correlationId").exists());
    }

    @Test
    void tamanoInvalidoResponde400Controlado() throws Exception {
        when(consultarReportesUseCase.entradas(any(), any(), any(), any(), any(), any(), anyInt(), anyInt()))
                .thenThrow(new SolicitudInvalidaException("paginación inválida"));

        mockMvc.perform(get("/api/v1/reportes/entradas")
                        .param("desde", "2026-01-01")
                        .param("hasta", "2026-01-31")
                        .param("tamano", "101")
                        .with(user("usuario").authorities(new SimpleGrantedAuthority("REPORTES_GENERAR"))))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("SOLICITUD_INVALIDA"));
    }

    @Test
    void listarEntradasDevuelvePaginaConPermisoDeGenerar() throws Exception {
        when(consultarReportesUseCase.entradas(any(), any(), any(), any(), any(), any(), anyInt(), anyInt()))
                .thenReturn(new Pagina<>(List.of(), 0, 1, 20));

        mockMvc.perform(get("/api/v1/reportes/entradas")
                        .param("desde", "2026-01-01")
                        .param("hasta", "2026-01-31")
                        .with(user("usuario").authorities(new SimpleGrantedAuthority("REPORTES_GENERAR"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.items").isArray())
                .andExpect(jsonPath("$.pagina").value(1));
    }

    @Test
    void exportarEntradasExigePermisoDeExportar() throws Exception {
        mockMvc.perform(get("/api/v1/reportes/entradas/exportacion")
                        .param("desde", "2026-01-01")
                        .param("hasta", "2026-01-31")
                        .with(user("usuario").authorities(new SimpleGrantedAuthority("REPORTES_GENERAR"))))
                .andExpect(status().isForbidden());
    }

    @Test
    void exportarEntradasGeneraXlsxYSanitizaFormulas() throws Exception {
        List<EntradaLineaDto> entradas = List.of("=expr", "+expr", "-expr", "@expr").stream()
                .map(texto -> new EntradaLineaDto(BigDecimal.ONE, BigDecimal.TWO, texto,
                        "A1", LocalDateTime.of(2026, 1, 1, 10, 0), "01010101", "PZA",
                        BigDecimal.TEN, "PARTE", LocalDateTime.of(2026, 1, 2, 10, 0)))
                .toList();
        when(consultarReportesUseCase.exportarEntradas(any(), any(), any(), any(), any(), any()))
                .thenReturn(entradas);

        MvcResult resultado = mockMvc.perform(get("/api/v1/reportes/entradas/exportacion")
                        .param("desde", "2026-01-01")
                        .param("hasta", "2026-01-31")
                        .with(user("usuario").authorities(new SimpleGrantedAuthority("REPORTES_EXPORTAR"))))
                .andExpect(status().isOk())
                .andExpect(header().string("Content-Disposition", org.hamcrest.Matchers.containsString("entradas.xlsx")))
                .andReturn();

        try (XSSFWorkbook libro = new XSSFWorkbook(new ByteArrayInputStream(resultado.getResponse().getContentAsByteArray()))) {
            var hoja = libro.getSheet("entradas");
            for (int indice = 0; indice < entradas.size(); indice++) {
                var celda = hoja.getRow(indice + 1).getCell(2);
                org.junit.jupiter.api.Assertions.assertEquals("'" + entradas.get(indice).pedimento(), celda.getStringCellValue());
                org.junit.jupiter.api.Assertions.assertEquals(org.apache.poi.ss.usermodel.CellType.STRING, celda.getCellType());
            }
        }
    }

    @Test
    void exportarEntradasSinResultadosDevuelve204() throws Exception {
        when(consultarReportesUseCase.exportarEntradas(any(), any(), any(), any(), any(), any()))
                .thenReturn(List.of());

        mockMvc.perform(get("/api/v1/reportes/entradas/exportacion")
                        .param("desde", "2026-01-01")
                        .param("hasta", "2026-01-31")
                        .with(user("usuario").authorities(new SimpleGrantedAuthority("REPORTES_EXPORTAR"))))
                .andExpect(status().isNoContent());
    }
}
