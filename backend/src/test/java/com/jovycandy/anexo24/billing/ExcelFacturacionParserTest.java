package com.jovycandy.anexo24.billing;

import com.jovycandy.anexo24.billing.application.command.ExcelFacturacionParser;
import com.jovycandy.anexo24.billing.domain.model.PlantillaFacturacion;

import org.apache.poi.hssf.usermodel.HSSFWorkbook;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayOutputStream;

import static org.junit.jupiter.api.Assertions.*;

class ExcelFacturacionParserTest {
    private final ExcelFacturacionParser parser = new ExcelFacturacionParser();

    @Test
    void leeLibroXlsConPreviewYSinPersistirValoresEnErrores() throws Exception {
        byte[] bytes;
        try (var workbook = new HSSFWorkbook(); var out = new ByteArrayOutputStream()) {
            var sheet = workbook.createSheet("Datos");
            Row header = sheet.createRow(0);
            header.createCell(0).setCellValue("Documento");
            header.createCell(1).setCellValue("Fecha");
            var data = sheet.createRow(1);
            data.createCell(0).setCellValue("DOC-1");
            data.createCell(1).setCellValue("2026-01-02");
            workbook.write(out);
            bytes = out.toByteArray();
        }
        var result = parser.parsear("prueba.xls", "a".repeat(64), bytes);
        assertEquals(1, result.filas());
        assertEquals("DOC-1", result.preview().getFirst().celdas().getFirst());
        assertTrue(result.errores().isEmpty());
    }

    @Test
    void leeLibroXlsxYDetectaFormulaSinDevolverValorDeCeldaComoError() throws Exception {
        byte[] bytes;
        try (var workbook = new XSSFWorkbook(); var out = new ByteArrayOutputStream()) {
            var sheet = workbook.createSheet("Datos");
            sheet.createRow(0).createCell(0).setCellValue("Cantidad");
            sheet.createRow(1).createCell(0).setCellFormula("1+1");
            workbook.write(out);
            bytes = out.toByteArray();
        }
        var result = parser.parsear("prueba.xlsx", "b".repeat(64), bytes);
        assertEquals("FORMULA", result.errores().getFirst().codigo());
        assertTrue(result.errores().getFirst().mensaje().contains("fórmulas"));
    }

    @Test
    void detectaColumnasDuplicadasIgnorandoAcentosMayusculasYEspacios() throws Exception {
        byte[] bytes;
        try (var workbook = new XSSFWorkbook(); var out = new ByteArrayOutputStream()) {
            var sheet = workbook.createSheet("Datos");
            var header = sheet.createRow(0);
            header.createCell(0).setCellValue("Almacén");
            header.createCell(1).setCellValue("  almacen  ");
            workbook.write(out);
            bytes = out.toByteArray();
        }
        var result = parser.parsear("prueba.xlsx", "d".repeat(64), bytes);
        assertTrue(result.errores().stream().anyMatch(error -> error.codigo().equals("COLUMNA_DUPLICADA")));
    }

    @Test
    void validaLayoutRealYConservaTodasLasFilasParaStaging() throws Exception {
        var columns = java.util.List.of(
                new PlantillaFacturacion.Columna("Documento", true, "TEXTO"),
                new PlantillaFacturacion.Columna("Fecha", true, "FECHA"),
                new PlantillaFacturacion.Columna("Almacen", false, "TEXTO"));
        var template = new PlantillaFacturacion("FACTURACION", "LEGACY-2026-09", "XLSX", "FACTURAS", columns);
        byte[] bytes;
        try (var workbook = new XSSFWorkbook(); var out = new ByteArrayOutputStream()) {
            var sheet = workbook.createSheet("FACTURAS");
            sheet.createRow(0).createCell(0).setCellValue("Documento");
            sheet.getRow(0).createCell(1).setCellValue("Fecha");
            sheet.getRow(0).createCell(2).setCellValue("Almacen");
            for (int i = 1; i <= 2; i++) {
                var row = sheet.createRow(i); row.createCell(0).setCellValue("DOC-" + i);
                row.createCell(1).setCellValue("2026-01-02"); row.createCell(2).setCellValue("A");
            }
            workbook.write(out); bytes = out.toByteArray();
        }
        var result = parser.parsear("Layout_Facturas.xlsx", "f".repeat(64), bytes, template);
        assertTrue(result.errores().isEmpty());
        assertEquals(2, result.filasNormalizadas().size());
        assertEquals(java.util.List.of("Documento", "Fecha", "Almacen"), result.columnas());
    }

    @Test
    void devuelveDiagnosticoControladoParaContenidoCorrupto() {
        var result = parser.parsear("libro.xlsx", "c".repeat(64), new byte[]{1, 2, 3});
        assertTrue(result.fallida());
        assertEquals("ARCHIVO_NO_LEIBLE", result.errores().getFirst().codigo());
        assertNull(result.errores().getFirst().valorEnmascarado());
    }

    @Test
    void validaFechaCantidadFilaVaciaYDuplicadoExactoSinExigirColumnas() throws Exception {
        byte[] bytes;
        try (var workbook = new XSSFWorkbook(); var out = new ByteArrayOutputStream()) {
            var sheet = workbook.createSheet("Datos");
            var header = sheet.createRow(0);
            header.createCell(0).setCellValue("Fecha");
            header.createCell(1).setCellValue("Cantidad");
            sheet.createRow(1).createCell(0).setCellValue("no-fecha");
            sheet.getRow(1).createCell(1).setCellValue("0");
            sheet.createRow(2);
            var duplicate = sheet.createRow(3);
            duplicate.createCell(0).setCellValue("no-fecha");
            duplicate.createCell(1).setCellValue("0");
            workbook.write(out);
            bytes = out.toByteArray();
        }
        var result = parser.parsear("prueba.xlsx", "e".repeat(64), bytes);
        assertTrue(result.errores().stream().anyMatch(error -> error.codigo().equals("FECHA_INVALIDA")));
        assertTrue(result.errores().stream().anyMatch(error -> error.codigo().equals("CANTIDAD_INVALIDA")));
        assertTrue(result.errores().stream().anyMatch(error -> error.codigo().equals("FILA_VACIA")));
        assertTrue(result.errores().stream().anyMatch(error -> error.codigo().equals("FILA_DUPLICADA")));
        assertTrue(result.errores().stream().allMatch(error -> error.valorEnmascarado() == null || error.valorEnmascarado().equals("***")));
    }
}
