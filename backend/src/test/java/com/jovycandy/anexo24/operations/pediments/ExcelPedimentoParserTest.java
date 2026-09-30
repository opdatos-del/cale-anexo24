package com.jovycandy.anexo24.operations.pediments;

import com.jovycandy.anexo24.operations.pediments.application.command.ExcelPedimentoParser;
import com.jovycandy.anexo24.operations.pediments.domain.model.CargaPedimentoArchivo;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayOutputStream;
import java.math.BigDecimal;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** Pruebas de parser sin documentos empresariales ni base real. */
class ExcelPedimentoParserTest {
    private final ExcelPedimentoParser parser = new ExcelPedimentoParser();

    @Test
    void aceptaFilaConfirmadaYCanonizaDecimalFecha() throws Exception {
        byte[] workbook = workbook(List.of("Aduana", "Patente", "NumeroPedimento", "ClavePedimento", "TipoOperacion",
                "FechaPago", "Sec", "Clave", "Descripcion", "Fraccion", "CantidadComercial", "UnidadComercial"),
                List.of("190", "3302", "5003971", "A1", "1", "2026-05-28", "1", "MAT-1", "Material sintético", "17019999", "12.50", "KG"));

        CargaPedimentoArchivo result = parser.parsear("sintetico.xlsx", "a".repeat(64), workbook);

        assertEquals(1, result.totalFilas());
        assertEquals(1, result.filasValidas());
        assertTrue(result.errores().isEmpty());
        assertEquals("12.50", result.filas().getFirst().datos().get("CantidadComercial"));
        assertEquals("2026-05-28", result.filas().getFirst().datos().get("FechaPago"));
        assertEquals(new BigDecimal("12.50"), new BigDecimal(result.filas().getFirst().datos().get("CantidadComercial")));
    }

    @Test
    void conservaOperacionExportacionSinCrearStagingParalelo() throws Exception {
        byte[] workbook = workbook(List.of("Aduana", "Patente", "NumeroPedimento", "ClavePedimento", "TipoOperacion",
                "FechaPago", "Sec", "Clave", "Descripcion", "Fraccion", "CantidadComercial", "UnidadComercial"),
                List.of("190", "3302", "5003972", "A1", "2", "2026-05-28", "1", "PROD-1", "Producto sintético", "17019999", "12.50", "KG"));

        CargaPedimentoArchivo result = parser.parsear("exportacion-sintetica.xlsx", "c".repeat(64), workbook);

        assertEquals(1, result.totalFilas());
        assertEquals("2", result.filas().getFirst().datos().get("TipoOperacion"));
        assertTrue(result.errores().isEmpty());
    }

    @Test
    void reportaHeaderFaltanteYFechaInvalidaSinGuardarValor() throws Exception {
        byte[] workbook = workbook(List.of("Aduana", "Patente", "NumeroPedimento", "ClavePedimento", "TipoOperacion",
                "FechaPago", "Sec", "Clave", "Descripcion", "Fraccion", "CantidadComercial"),
                List.of("190", "3302", "5003971", "A1", "1", "fecha-no-valida", "1", "MAT-1", "Material", "17019999", "0"));

        CargaPedimentoArchivo result = parser.parsear("invalido.xlsx", "b".repeat(64), workbook);

        assertEquals(1, result.totalFilas());
        assertEquals(0, result.filasValidas());
        assertTrue(result.errores().stream().anyMatch(error -> "COLUMNA_OBLIGATORIA_AUSENTE".equals(error.codigo())));
        assertTrue(result.errores().stream().anyMatch(error -> "TIPO_INVALIDO".equals(error.codigo()) || "FECHA_INVALIDA".equals(error.codigo())));
        assertTrue(result.errores().stream().allMatch(error -> "no almacenado".equals(error.valorEnmascarado())));
    }

    private byte[] workbook(List<String> headers, List<String> values) throws Exception {
        try (XSSFWorkbook workbook = new XSSFWorkbook(); ByteArrayOutputStream output = new ByteArrayOutputStream()) {
            var sheet = workbook.createSheet("PEDIMENTOS");
            var header = sheet.createRow(0);
            for (int i = 0; i < headers.size(); i++) header.createCell(i).setCellValue(headers.get(i));
            var row = sheet.createRow(1);
            for (int i = 0; i < values.size(); i++) row.createCell(i).setCellValue(values.get(i));
            workbook.write(output);
            return output.toByteArray();
        }
    }
}
