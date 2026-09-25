package com.jovycandy.anexo24.billing.api.controller;

import com.jovycandy.anexo24.billing.api.dto.FacturacionTemplateResponse;
import com.jovycandy.anexo24.billing.domain.model.PlantillaFacturacion;
import com.jovycandy.anexo24.billing.domain.port.PlantillaFacturacionRepository;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.io.ByteArrayOutputStream;

@RestController
@RequestMapping("/api/v1/facturacion/plantilla")
public class FacturacionPlantillaController {
    private static final MediaType XLSX = MediaType.parseMediaType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");
    private final PlantillaFacturacionRepository repository;

    public FacturacionPlantillaController(PlantillaFacturacionRepository repository) { this.repository = repository; }

    @GetMapping
    @PreAuthorize("hasAuthority('FACTURACION_CARGAR')")
    public ResponseEntity<FacturacionTemplateResponse> obtener() {
        PlantillaFacturacion template = repository.findActive().orElseThrow(
                BillingUploadExceptionHandler.PlantillaFacturacionNoConfiguradaException::new);
        return ResponseEntity.ok(FacturacionTemplateResponse.from(template));
    }

    @GetMapping("/archivo")
    @PreAuthorize("hasAuthority('FACTURACION_CARGAR')")
    public ResponseEntity<byte[]> descargar() {
        PlantillaFacturacion template = repository.findActive().orElseThrow(
                BillingUploadExceptionHandler.PlantillaFacturacionNoConfiguradaException::new);
        try (var workbook = new XSSFWorkbook(); var output = new ByteArrayOutputStream()) {
            var sheet = workbook.createSheet(template.hoja());
            var header = sheet.createRow(0);
            for (int i = 0; i < template.columnas().size(); i++) header.createCell(i).setCellValue(template.columnas().get(i).nombre());
            workbook.write(output);
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(XLSX);
            headers.setContentDisposition(ContentDisposition.attachment().filename("Layout_Facturas.xlsx").build());
            return ResponseEntity.ok().headers(headers).body(output.toByteArray());
        } catch (Exception exception) {
            throw new IllegalStateException("No fue posible generar el layout de Facturación", exception);
        }
    }
}
