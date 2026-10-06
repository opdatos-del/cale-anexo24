package com.jovycandy.anexo24.reports.api.controller;

import com.jovycandy.anexo24.Anexo24Application;
import com.jovycandy.anexo24.operations.entries.api.dto.EntradaLineaDto;
import com.jovycandy.anexo24.reports.application.query.ConsultarReportesUseCase;
import com.jovycandy.anexo24.reports.extended.application.query.ListarAnalisisDescargasUseCase;
import com.jovycandy.anexo24.reports.extended.application.query.ListarOperacionesBloqueadasUseCase;
import com.jovycandy.anexo24.reports.extended.application.query.ListarCompulsaUseCase;
import com.jovycandy.anexo24.reports.extended.application.query.ListarLineasF4UseCase;
import com.jovycandy.anexo24.reports.extended.application.query.ListarOperacionesDirigidasUseCase;
import com.jovycandy.anexo24.reports.extended.application.query.ListarRectificacionesUseCase;
import com.jovycandy.anexo24.reports.extended.application.query.ListarVencimientosUseCase;
import com.jovycandy.anexo24.reports.extended.domain.model.AnalisisDescarga;
import com.jovycandy.anexo24.reports.extended.domain.model.LineaF4;
import com.jovycandy.anexo24.reports.extended.domain.model.OperacionDirigida;
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
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
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

    @MockitoBean
    private ListarAnalisisDescargasUseCase listarAnalisisDescargasUseCase;

    @MockitoBean
    private ListarOperacionesBloqueadasUseCase listarOperacionesBloqueadasUseCase;

    @MockitoBean
    private ListarCompulsaUseCase listarCompulsaUseCase;

    @MockitoBean
    private ListarOperacionesDirigidasUseCase listarOperacionesDirigidasUseCase;

    @MockitoBean
    private ListarRectificacionesUseCase listarRectificacionesUseCase;

    @MockitoBean
    private ListarVencimientosUseCase listarVencimientosUseCase;

    @MockitoBean
    private ListarLineasF4UseCase listarLineasF4UseCase;

    @Test
    void listarAnalisisDescargasSinAutenticacionResponde401() throws Exception {
        mockMvc.perform(get("/api/v1/reportes/analisis-descargas"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void listarAnalisisDescargasExigePermisoDeReportes() throws Exception {
        mockMvc.perform(get("/api/v1/reportes/analisis-descargas")
                        .with(user("usuario").authorities(new SimpleGrantedAuthority("OPERACIONES_CONSULTAR"))))
                .andExpect(status().isForbidden());
    }

    @Test
    void listarAnalisisDescargasRespondePaginaConPermiso() throws Exception {
        when(listarAnalisisDescargasUseCase.ejecutar(any(), anyInt(), anyInt()))
                .thenReturn(new Pagina<>(List.of(), 0, 1, 20));
        mockMvc.perform(get("/api/v1/reportes/analisis-descargas")
                        .param("filtro", "190-1562")
                        .with(user("usuario").authorities(new SimpleGrantedAuthority("REPORTES_GENERAR"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.items").isArray())
                .andExpect(jsonPath("$.pagina").value(1));
    }

    @Test
    void exportarAnalisisDescargasSinAutenticacionResponde401() throws Exception {
        mockMvc.perform(get("/api/v1/reportes/analisis-descargas/exportacion"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void exportarAnalisisDescargasExigePermisoDeExportar() throws Exception {
        mockMvc.perform(get("/api/v1/reportes/analisis-descargas/exportacion")
                        .with(user("usuario").authorities(new SimpleGrantedAuthority("REPORTES_GENERAR"))))
                .andExpect(status().isForbidden());
    }

    @Test
    void exportarAnalisisDescargasSinFilasResponde204() throws Exception {
        when(listarAnalisisDescargasUseCase.exportar(any())).thenReturn(List.of());

        mockMvc.perform(get("/api/v1/reportes/analisis-descargas/exportacion")
                        .with(user("usuario").authorities(new SimpleGrantedAuthority("REPORTES_EXPORTAR"))))
                .andExpect(status().isNoContent())
                .andExpect(content().string(""));
    }

    @Test
    void exportarAnalisisDescargasGeneraXlsxConProyeccionActual() throws Exception {
        when(listarAnalisisDescargasUseCase.exportar(any())).thenReturn(List.of(new AnalisisDescarga(1L,
                "IMP-1", "1", "EXP-1", "2", "MAT-1", "PROD-1", LocalDateTime.of(2026, 1, 1, 0, 0),
                LocalDateTime.of(2026, 1, 2, 0, 0), LocalDateTime.of(2027, 1, 1, 0, 0), BigDecimal.TEN,
                BigDecimal.ONE, BigDecimal.ONE, BigDecimal.ZERO, null, "KG")));

        MvcResult resultado = mockMvc.perform(get("/api/v1/reportes/analisis-descargas/exportacion")
                        .param("filtro", "MAT-1")
                        .with(user("usuario").authorities(new SimpleGrantedAuthority("REPORTES_EXPORTAR"))))
                .andExpect(status().isOk())
                .andExpect(header().string("Content-Disposition", org.hamcrest.Matchers.containsString("analisis-descargas.xlsx")))
                .andReturn();

        try (XSSFWorkbook libro = new XSSFWorkbook(new ByteArrayInputStream(resultado.getResponse().getContentAsByteArray()))) {
            var hoja = libro.getSheet("analisis-descargas");
            org.junit.jupiter.api.Assertions.assertEquals("Descarga", hoja.getRow(0).getCell(0).getStringCellValue());
            org.junit.jupiter.api.Assertions.assertEquals("IMP-1", hoja.getRow(1).getCell(1).getStringCellValue());
        }
    }

    @Test
    void listarOperacionesBloqueadasSinAutenticacionResponde401() throws Exception {
        mockMvc.perform(get("/api/v1/reportes/operaciones-bloqueadas"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void listarOperacionesBloqueadasExigePermisoDeReportes() throws Exception {
        mockMvc.perform(get("/api/v1/reportes/operaciones-bloqueadas")
                        .with(user("usuario").authorities(new SimpleGrantedAuthority("OPERACIONES_CONSULTAR"))))
                .andExpect(status().isForbidden());
    }

    @Test
    void listarOperacionesBloqueadasRespondePaginaConPermiso() throws Exception {
        when(listarOperacionesBloqueadasUseCase.ejecutar(any(), anyInt(), anyInt()))
                .thenReturn(new Pagina<>(List.of(), 0, 1, 20));
        mockMvc.perform(get("/api/v1/reportes/operaciones-bloqueadas")
                        .param("filtro", "26")
                        .with(user("usuario").authorities(new SimpleGrantedAuthority("REPORTES_GENERAR"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.items").isArray())
                .andExpect(jsonPath("$.pagina").value(1));
    }

    @Test
    void listarVencimientosExigePermisoDeReportes() throws Exception {
        mockMvc.perform(get("/api/v1/reportes/vencimientos")
                        .with(user("usuario").authorities(new SimpleGrantedAuthority("OPERACIONES_CONSULTAR"))))
                .andExpect(status().isForbidden());
    }

    @Test
    void listarVencimientosRespondePaginaConPermiso() throws Exception {
        when(listarVencimientosUseCase.ejecutar(any(), anyInt(), anyInt()))
                .thenReturn(new Pagina<>(List.of(), 0, 1, 20));
        mockMvc.perform(get("/api/v1/reportes/vencimientos")
                        .param("filtro", "26")
                        .with(user("usuario").authorities(new SimpleGrantedAuthority("REPORTES_GENERAR"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.items").isArray())
                .andExpect(jsonPath("$.pagina").value(1));
    }

    @Test
    void listarDirigidosSinAutenticacionResponde401() throws Exception {
        mockMvc.perform(get("/api/v1/reportes/dirigidos"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void listarDirigidosExigePermisoDeReportes() throws Exception {
        mockMvc.perform(get("/api/v1/reportes/dirigidos")
                        .with(user("usuario").authorities(new SimpleGrantedAuthority("OPERACIONES_CONSULTAR"))))
                .andExpect(status().isForbidden());
    }

    @Test
    void listarDirigidosRespondePaginaConPermiso() throws Exception {
        when(listarOperacionesDirigidasUseCase.ejecutar(any(), anyInt(), anyInt()))
                .thenReturn(new Pagina<>(List.of(), 0, 1, 20));
        mockMvc.perform(get("/api/v1/reportes/dirigidos")
                        .param("filtro", "26")
                        .with(user("usuario").authorities(new SimpleGrantedAuthority("REPORTES_GENERAR"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.items").isArray())
                .andExpect(jsonPath("$.pagina").value(1));
    }

    @Test
    void exportarDirigidosSinAutenticacionResponde401() throws Exception {
        mockMvc.perform(get("/api/v1/reportes/dirigidos/exportacion"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void exportarDirigidosExigePermisoDeExportar() throws Exception {
        mockMvc.perform(get("/api/v1/reportes/dirigidos/exportacion")
                        .with(user("usuario").authorities(new SimpleGrantedAuthority("REPORTES_GENERAR"))))
                .andExpect(status().isForbidden());
    }

    @Test
    void exportarDirigidosSinFilasResponde204() throws Exception {
        when(listarOperacionesDirigidasUseCase.exportar(any())).thenReturn(List.of());

        mockMvc.perform(get("/api/v1/reportes/dirigidos/exportacion")
                        .with(user("usuario").authorities(new SimpleGrantedAuthority("REPORTES_EXPORTAR"))))
                .andExpect(status().isNoContent())
                .andExpect(content().string(""));
    }

    @Test
    void exportarDirigidosGeneraXlsxConContratoActual() throws Exception {
        when(listarOperacionesDirigidasUseCase.exportar(any())).thenReturn(List.of(new OperacionDirigida(10L, 20L,
                "EXP-1", LocalDateTime.of(2026, 1, 15, 0, 0), "A1", 2, "PROD-1", BigDecimal.TEN,
                "FAC-1", "SI", "SI", BigDecimal.ONE, BigDecimal.TWO, "SI", 3)));

        MvcResult resultado = mockMvc.perform(get("/api/v1/reportes/dirigidos/exportacion")
                        .param("filtro", "PROD-1")
                        .with(user("usuario").authorities(new SimpleGrantedAuthority("REPORTES_EXPORTAR"))))
                .andExpect(status().isOk())
                .andExpect(header().string("Content-Disposition", org.hamcrest.Matchers.containsString("dirigidos.xlsx")))
                .andReturn();

        try (XSSFWorkbook libro = new XSSFWorkbook(new ByteArrayInputStream(resultado.getResponse().getContentAsByteArray()))) {
            var hoja = libro.getSheet("dirigidos");
            org.junit.jupiter.api.Assertions.assertEquals("Salida", hoja.getRow(0).getCell(0).getStringCellValue());
            org.junit.jupiter.api.Assertions.assertEquals("EXP-1", hoja.getRow(1).getCell(2).getStringCellValue());
        }
    }

    @Test
    void listarRectificacionesExigePermisoDeReportes() throws Exception {
        mockMvc.perform(get("/api/v1/reportes/rectificaciones")
                        .with(user("usuario").authorities(new SimpleGrantedAuthority("OPERACIONES_CONSULTAR"))))
                .andExpect(status().isForbidden());
    }

    @Test
    void listarRectificacionesRespondePaginaConPermiso() throws Exception {
        when(listarRectificacionesUseCase.ejecutar(any(), anyInt(), anyInt()))
                .thenReturn(new Pagina<>(List.of(), 0, 1, 20));
        mockMvc.perform(get("/api/v1/reportes/rectificaciones")
                        .param("filtro", "26")
                        .with(user("usuario").authorities(new SimpleGrantedAuthority("REPORTES_GENERAR"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.items").isArray())
                .andExpect(jsonPath("$.pagina").value(1));
    }

    @Test
    void listarCompulsaExigePermisoDeReportes() throws Exception {
        mockMvc.perform(get("/api/v1/reportes/compulsa")
                        .with(user("usuario").authorities(new SimpleGrantedAuthority("OPERACIONES_CONSULTAR"))))
                .andExpect(status().isForbidden());
    }

    @Test
    void listarCompulsaRespondePaginaConPermiso() throws Exception {
        when(listarCompulsaUseCase.ejecutar(any(), anyInt(), anyInt()))
                .thenReturn(new Pagina<>(List.of(), 0, 1, 20));
        mockMvc.perform(get("/api/v1/reportes/compulsa")
                        .with(user("usuario").authorities(new SimpleGrantedAuthority("REPORTES_GENERAR"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.items").isArray())
                .andExpect(jsonPath("$.pagina").value(1));
    }

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

    @Test
    void listarF4ExigePermisoDeReportes() throws Exception {
        mockMvc.perform(get("/api/v1/reportes/f4")
                        .with(user("usuario").authorities(new SimpleGrantedAuthority("OPERACIONES_CONSULTAR"))))
                .andExpect(status().isForbidden());
    }

    @Test
    void listarF4RespondePaginaConPermiso() throws Exception {
        when(listarLineasF4UseCase.ejecutar(any(), anyInt(), anyInt()))
                .thenReturn(new Pagina<>(List.of(), 0, 1, 20));
        mockMvc.perform(get("/api/v1/reportes/f4")
                        .param("filtro", "CTMAPAA")
                        .with(user("usuario").authorities(new SimpleGrantedAuthority("REPORTES_GENERAR"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.items").isArray())
                .andExpect(jsonPath("$.pagina").value(1));
    }

    @Test
    void exportarF4ExigePermisoDeExportar() throws Exception {
        mockMvc.perform(get("/api/v1/reportes/f4/exportacion")
                        .with(user("usuario").authorities(new SimpleGrantedAuthority("REPORTES_GENERAR"))))
                .andExpect(status().isForbidden());
    }

    @Test
    void exportarF4GeneraXlsxConProyeccionV1() throws Exception {
        when(listarLineasF4UseCase.exportar(any())).thenReturn(List.of(new LineaF4("CTMAPAA", "F4-1",
                LocalDateTime.of(2026, 1, 15, 10, 0), "IMP-1", "MAT-1", BigDecimal.TEN, BigDecimal.ONE)));

        MvcResult resultado = mockMvc.perform(get("/api/v1/reportes/f4/exportacion")
                        .with(user("usuario").authorities(new SimpleGrantedAuthority("REPORTES_EXPORTAR"))))
                .andExpect(status().isOk())
                .andExpect(header().string("Content-Disposition", org.hamcrest.Matchers.containsString("f4.xlsx")))
                .andReturn();

        try (XSSFWorkbook libro = new XSSFWorkbook(new ByteArrayInputStream(resultado.getResponse().getContentAsByteArray()))) {
            var hoja = libro.getSheet("f4");
            org.junit.jupiter.api.Assertions.assertEquals("CTMAPAA", hoja.getRow(1).getCell(0).getStringCellValue());
            org.junit.jupiter.api.Assertions.assertEquals("F4-1", hoja.getRow(1).getCell(1).getStringCellValue());
        }
    }
}
