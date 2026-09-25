package com.jovycandy.anexo24.billing;

import com.jovycandy.anexo24.billing.api.controller.CargaFacturacionController;
import com.jovycandy.anexo24.billing.application.command.ExcelFacturacionParser;
import com.jovycandy.anexo24.billing.application.usecase.CargarFacturacionUseCase;
import com.jovycandy.anexo24.billing.domain.model.ArchivoFacturacion;
import com.jovycandy.anexo24.billing.domain.model.PlantillaFacturacion;
import com.jovycandy.anexo24.billing.domain.port.PlantillaFacturacionRepository;
import com.jovycandy.anexo24.security.AuthenticatedUserPrincipal;
import com.jovycandy.anexo24.shared.api.GlobalExceptionHandler;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockMultipartFile;

import java.io.ByteArrayOutputStream;
import java.security.MessageDigest;
import java.util.HexFormat;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class CargaFacturacionControllerTest {
    @Test
    void devuelveContratoDePlantillaConCorrelationIdYConteos() throws Exception {
        byte[] xlsx;
        try (var workbook = new XSSFWorkbook(); var out = new ByteArrayOutputStream()) {
            var sheet = workbook.createSheet(PlantillaFacturacion.HOJA_FACTURAS);
            sheet.createRow(0).createCell(0).setCellValue("Documento");
            sheet.createRow(1).createCell(0).setCellValue("DOC-1");
            workbook.write(out);
            xlsx = out.toByteArray();
        }
        var useCase = mock(CargarFacturacionUseCase.class);
        when(useCase.ejecutarLote(anyList(), eq(7L), eq("corr-1"))).thenReturn(List.of(42L));
        var templateRepository = mock(PlantillaFacturacionRepository.class);
        var template = new PlantillaFacturacion("FACTURACION", "LEGACY-2026-09", "XLSX",
                PlantillaFacturacion.HOJA_FACTURAS, List.of(new PlantillaFacturacion.Columna("Documento", true, "TEXTO")));
        when(templateRepository.findActive()).thenReturn(Optional.of(template));
        var controller = new CargaFacturacionController(new ExcelFacturacionParser(), useCase, templateRepository);
        var file = new MockMultipartFile("archivos", "test.xlsx",
                "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet", xlsx);
        var request = new MockHttpServletRequest();
        request.setAttribute(GlobalExceptionHandler.CORRELATION_ID_ATTR, "corr-1");

        var response = controller.cargar(List.of(file), new AuthenticatedUserPrincipal(7L, "qa"), request).getBody();
        var captor = org.mockito.ArgumentCaptor.forClass(List.class);
        verify(useCase).ejecutarLote(captor.capture(), eq(7L), eq("corr-1"));

        assertNotNull(response);
        assertEquals("corr-1", response.correlationId());
        assertEquals("FACTURACION:LEGACY-2026-09", response.plantilla());
        assertFalse(response.confirmacionDisponible());
        assertEquals("VALIDADA", response.cargas().getFirst().estado());
        assertEquals("DOC-1", response.cargas().getFirst().preview().filas().getFirst().get("Documento"));
        var parsed = (List<ArchivoFacturacion>) captor.getValue();
        assertEquals(HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(xlsx)), parsed.getFirst().hash());
    }

    @Test
    void rechazaCargaSinPlantillaActivaConErrorEspecifico() throws Exception {
        var repository = mock(PlantillaFacturacionRepository.class);
        when(repository.findActive()).thenReturn(Optional.empty());
        var controller = new CargaFacturacionController(new ExcelFacturacionParser(), mock(CargarFacturacionUseCase.class), repository);
        var request = new MockHttpServletRequest();
        request.setAttribute(GlobalExceptionHandler.CORRELATION_ID_ATTR, "corr-template");
        byte[] bytes;
        try (var workbook = new XSSFWorkbook(); var out = new ByteArrayOutputStream()) {
            workbook.createSheet(PlantillaFacturacion.HOJA_FACTURAS).createRow(0).createCell(0).setCellValue("Documento");
            workbook.write(out);
            bytes = out.toByteArray();
        }
        var file = new MockMultipartFile("archivos", "test.xlsx", "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet", bytes);
        assertThrows(com.jovycandy.anexo24.billing.api.controller.BillingUploadExceptionHandler.PlantillaFacturacionNoConfiguradaException.class,
                () -> controller.cargar(List.of(file), new AuthenticatedUserPrincipal(7L, "qa"), request));
    }

    @Test
    void anotacionRestringeCargaAlPermisoFacturacion() throws Exception {
        var method = CargaFacturacionController.class.getMethod("cargar", List.class,
                AuthenticatedUserPrincipal.class, jakarta.servlet.http.HttpServletRequest.class);
        assertEquals("hasAuthority('FACTURACION_CARGAR')",
                method.getAnnotation(org.springframework.security.access.prepost.PreAuthorize.class).value());
    }
}
