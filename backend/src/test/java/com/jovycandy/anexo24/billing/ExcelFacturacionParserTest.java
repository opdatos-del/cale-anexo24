package com.jovycandy.anexo24.billing;

import com.jovycandy.anexo24.billing.application.command.ExcelFacturacionParser;
import com.jovycandy.anexo24.billing.domain.model.PlantillaFacturacion;
import org.apache.poi.hssf.usermodel.HSSFWorkbook;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayOutputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class ExcelFacturacionParserTest {
    private final ExcelFacturacionParser parser = new ExcelFacturacionParser();

    @Test
    void leeLibroXlsConPreviewYSinPersistirValoresEnErrores() throws Exception {
        byte[] bytes;
        try (var workbook = new HSSFWorkbook(); var out = new ByteArrayOutputStream()) {
            var sheet = workbook.createSheet("FACTURAS");
            Row header = sheet.createRow(0);
            header.createCell(0).setCellValue("Documento");
            header.createCell(1).setCellValue("Fecha");
            var data = sheet.createRow(1);
            data.createCell(0).setCellValue("DOC-1");
            data.createCell(1).setCellValue("2026-01-02");
            workbook.write(out);
            bytes = out.toByteArray();
        }
        var result = parser.parsear("prueba.xls", "a".repeat(64), bytes, template("FACTURAS", "Documento", "Fecha"));
        assertEquals(1, result.filas());
        assertEquals("DOC-1", result.preview().getFirst().celdas().getFirst());
        assertEquals("2026-01-02", result.preview().getFirst().celdas().get(1));
        assertTrue(result.errores().isEmpty());
    }

    @Test
    void leeLibroXlsxYDetectaFormulaSinDevolverValorDeCeldaComoError() throws Exception {
        byte[] bytes;
        try (var workbook = new XSSFWorkbook(); var out = new ByteArrayOutputStream()) {
            var sheet = workbook.createSheet("FACTURAS");
            sheet.createRow(0).createCell(0).setCellValue("Cantidad");
            sheet.createRow(1).createCell(0).setCellFormula("1+1");
            workbook.write(out);
            bytes = out.toByteArray();
        }
        var result = parser.parsear("prueba.xlsx", "b".repeat(64), bytes, template("FACTURAS", "Cantidad"));
        assertTrue(result.errores().stream().anyMatch(error -> error.codigo().equals("FORMULA")));
    }

    @Test
    void detectaColumnasDuplicadasIgnorandoAcentosMayusculasYEspacios() throws Exception {
        byte[] bytes;
        try (var workbook = new XSSFWorkbook(); var out = new ByteArrayOutputStream()) {
            var sheet = workbook.createSheet("FACTURAS");
            var header = sheet.createRow(0);
            header.createCell(0).setCellValue("Almacén");
            header.createCell(1).setCellValue("  almacen  ");
            workbook.write(out);
            bytes = out.toByteArray();
        }
        var result = parser.parsear("prueba.xlsx", "d".repeat(64), bytes, template("FACTURAS", "Almacen"));
        assertTrue(result.errores().stream().anyMatch(error -> error.codigo().equals("COLUMNA_DUPLICADA")));
    }

    @Test
    void validaLayoutRealYConservaTodasLasFilasParaStaging() throws Exception {
        var template = template("FACTURAS", "Documento", "Fecha", "Almacen");
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
        assertEquals(List.of("Documento", "Fecha", "Almacen"), result.columnas());
    }

    @Test
    void mapeaHeadersReordenadosPorNombreYCanonicalizaFechaYCantidad() throws Exception {
        var template = fullTemplate();
        List<String> physical = List.of("Documento", "Fecha", "Observaciones", "Descarga", "Tipo", "Linea", "Clave", "Cantidad", "Unidad", "Almacen", "Lote", "Dirigido", "Cliente");
        var values = new java.util.HashMap<String, String>();
        values.put("Documento", "001-ABC"); values.put("Fecha", "25/09/2026"); values.put("Observaciones", "  nota  ");
        values.put("Descarga", "D"); values.put("Tipo", "T"); values.put("Linea", "L"); values.put("Clave", "K");
        values.put("Cantidad", "10.50"); values.put("Unidad", "PZA"); values.put("Almacen", "A");
        values.put("Lote", "LOTE-1"); values.put("Dirigido", "DIR"); values.put("Cliente", "CLI");
        byte[] bytes = workbookWithRow("FACTURAS", physical, values);

        var result = parser.parsear("reordenado.xlsx", "r".repeat(64), bytes, template);
        var row = result.filasNormalizadas().getFirst().celdas();
        assertTrue(result.errores().isEmpty());
        assertEquals("001-ABC", row.get(0));
        assertEquals("2026-09-25", row.get(1));
        assertEquals("nota", row.get(3));
        assertEquals("10.50", row.get(9));
        assertEquals("PZA", row.get(10));
    }

    @Test
    void columnaOpcionalOmitidaNoDesplazaLasSiguientes() throws Exception {
        var template = fullTemplate();
        List<String> physical = template.nombres().stream().filter(name -> !name.equals("Almacen")).toList();
        Map<String, String> values = valuesFor(physical);
        values = new java.util.HashMap<>(values);
        values.put("Observaciones", "OBS-CORRECTA");
        byte[] bytes = workbookWithRow("FACTURAS", physical, values);

        var result = parser.parsear("sin-almacen.xlsx", "s".repeat(64), bytes, template);
        var row = result.filasNormalizadas().getFirst().celdas();
        assertTrue(result.errores().isEmpty());
        assertEquals("", row.get(2));
        assertEquals("OBS-CORRECTA", row.get(3));
    }

    @Test
    void columnaObligatoriaAusenteInvalidaSinDesplazarLosValores() throws Exception {
        var template = fullTemplate();
        List<String> physical = template.nombres().stream().filter(name -> !name.equals("Cantidad")).toList();
        byte[] bytes = workbookWithRow("FACTURAS", physical, valuesFor(physical));

        var result = parser.parsear("sin-cantidad.xlsx", "m".repeat(64), bytes, template);
        var row = result.filasNormalizadas().getFirst().celdas();
        assertTrue(result.errores().stream().anyMatch(error -> error.codigo().equals("COLUMNA_OBLIGATORIA_AUSENTE")));
        assertEquals("PZA", row.get(10));
        assertEquals("CLI", row.get(12));
    }

    @Test
    void devuelveDiagnosticoControladoParaContenidoCorrupto() {
        var result = parser.parsear("libro.xlsx", "c".repeat(64), new byte[]{1, 2, 3}, template("FACTURAS", "Documento"));
        assertTrue(result.fallida());
        assertEquals("ARCHIVO_NO_LEIBLE", result.errores().getFirst().codigo());
        assertNull(result.errores().getFirst().valorEnmascarado());
    }

    @Test
    void validaFechaCantidadFilaVaciaYDuplicadoExacto() throws Exception {
        byte[] bytes;
        try (var workbook = new XSSFWorkbook(); var out = new ByteArrayOutputStream()) {
            var sheet = workbook.createSheet("FACTURAS");
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
        var result = parser.parsear("prueba.xlsx", "e".repeat(64), bytes, template("FACTURAS", "Fecha", "Cantidad"));
        assertTrue(result.errores().stream().anyMatch(error -> error.codigo().equals("FECHA_INVALIDA")));
        assertTrue(result.errores().stream().anyMatch(error -> error.codigo().equals("CANTIDAD_INVALIDA")));
        assertTrue(result.errores().stream().anyMatch(error -> error.codigo().equals("FILA_VACIA")));
        assertTrue(result.errores().stream().anyMatch(error -> error.codigo().equals("FILA_DUPLICADA")));
    }

    private PlantillaFacturacion fullTemplate() {
        List<String> required = List.of("Documento", "Fecha", "Descarga", "Tipo", "Linea", "Clave", "Cantidad", "Unidad");
        List<String> names = List.of("Documento", "Fecha", "Almacen", "Observaciones", "Descarga", "Tipo", "Linea", "Clave", "Lote", "Cantidad", "Unidad", "Dirigido", "Cliente");
        return new PlantillaFacturacion("FACTURACION", "LEGACY-2026-09", "XLSX", PlantillaFacturacion.HOJA_FACTURAS,
                names.stream().map(name -> new PlantillaFacturacion.Columna(name, required.contains(name), name.equals("Fecha") ? "FECHA" : name.equals("Cantidad") ? "DECIMAL" : "TEXTO")).toList());
    }

    private PlantillaFacturacion template(String sheet, String... names) {
        return new PlantillaFacturacion("FACTURACION", "TEST", "XLSX", PlantillaFacturacion.HOJA_FACTURAS,
                List.of(names).stream().map(name -> new PlantillaFacturacion.Columna(name, true,
                        name.equals("Fecha") ? "FECHA" : name.equals("Cantidad") ? "DECIMAL" : "TEXTO")).toList());
    }

    private byte[] workbookWithRow(String sheetName, List<String> headers, Map<String, String> values) throws Exception {
        try (var workbook = new XSSFWorkbook(); var out = new ByteArrayOutputStream()) {
            var sheet = workbook.createSheet(sheetName);
            var header = sheet.createRow(0);
            var row = sheet.createRow(1);
            for (int i = 0; i < headers.size(); i++) {
                header.createCell(i).setCellValue(headers.get(i));
                row.createCell(i).setCellValue(values.getOrDefault(headers.get(i), ""));
            }
            workbook.write(out);
            return out.toByteArray();
        }
    }

    private Map<String, String> valuesFor(List<String> headers) {
        var values = new java.util.HashMap<String, String>();
        for (String header : headers) values.put(header, switch (header) {
            case "Documento" -> "DOC-1";
            case "Fecha" -> "2026-01-02";
            case "Cantidad" -> "10";
            case "Unidad" -> "PZA";
            case "Cliente" -> "CLI";
            default -> header + "-VALOR";
        });
        return values;
    }
}
