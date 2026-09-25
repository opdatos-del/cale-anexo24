package com.jovycandy.anexo24.billing;

import com.jovycandy.anexo24.billing.application.command.ExcelFacturacionParser;

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
