package com.jovycandy.anexo24.operations.actas;

import com.jovycandy.anexo24.catalogs.imports.application.command.ExcelCatalogImportParser;
import com.jovycandy.anexo24.catalogs.imports.domain.model.CatalogImportArchivo;
import com.jovycandy.anexo24.catalogs.imports.domain.model.CatalogImportType;
import org.apache.poi.hssf.usermodel.HSSFWorkbook;
import org.apache.poi.ss.usermodel.CellStyle;
import org.apache.poi.ss.usermodel.DataFormat;
import org.apache.poi.ss.usermodel.DateUtil;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayOutputStream;
import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/** Contrato del parser ACTA derivado de dbo.ACTA; no ejecuta SP mutables. */
class ActaImportParserTest {

    private static final List<String> HEADERS = List.of(
            "Folio", "Fecha", "Clave", "Linea", "Cantidad", "Umc", "DescargaDirigida", "ValorComercial");

    private final ExcelCatalogImportParser parser = new ExcelCatalogImportParser();

    private CatalogImportArchivo parse(byte[] bytes) {
        return parser.parsear(CatalogImportType.ACTA, "actas.xlsx", "a".repeat(64), bytes);
    }

    private List<String> codigos(CatalogImportArchivo archivo) {
        return archivo.errores().stream().map(error -> error.codigo()).toList();
    }

    private List<Object> fila(Object folio, Object fecha, Object clave, Object linea, Object cantidad,
                              Object umc, Object descarga, Object valor) {
        return java.util.Arrays.asList(folio, fecha, clave, linea, cantidad, umc, descarga, valor);
    }

    private byte[] workbook(boolean xls, List<List<Object>> filas) throws Exception {
        try (Workbook wb = xls ? new HSSFWorkbook() : new XSSFWorkbook();
             ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            var sheet = wb.createSheet("ACTA");
            var header = sheet.createRow(0);
            for (int i = 0; i < HEADERS.size(); i++) header.createCell(i).setCellValue(HEADERS.get(i));
            DataFormat formatos = wb.getCreationHelper().createDataFormat();
            short estiloFecha = formatos.getFormat("yyyy-mm-dd hh:mm:ss");
            for (int r = 0; r < filas.size(); r++) {
                var row = sheet.createRow(r + 1);
                for (int c = 0; c < filas.get(r).size(); c++) {
                    var cell = row.createCell(c);
                    Object valor = filas.get(r).get(c);
                    if (valor instanceof LocalDateTime momento) {
                        CellStyle estilo = wb.createCellStyle();
                        estilo.setDataFormat(estiloFecha);
                        cell.setCellStyle(estilo);
                        cell.setCellValue(DateUtil.getExcelDate(momento));
                    } else {
                        cell.setCellValue(valor == null ? "" : valor.toString());
                    }
                }
            }
            wb.write(out);
            return out.toByteArray();
        }
    }

    @Test
    void actaXlsxValida() throws Exception {
        CatalogImportArchivo result = parse(workbook(false, List.of(
                fila("ACC-1", LocalDateTime.of(2026, 10, 5, 0, 0), "P001", "1", "5", "PIEZ", "DIR-1", "12.5"))));

        assertThat(result.errores()).isEmpty();
        assertThat(result.totalFilas()).isEqualTo(1);
        assertThat(result.columnas()).isEqualTo(HEADERS);
        assertThat(result.versionContrato()).isEqualTo("ACTA-V1");
    }

    @Test
    void actaXlsValida() throws Exception {
        CatalogImportArchivo result = parse(workbook(true, List.of(
                fila("ACC-2", LocalDateTime.of(2026, 10, 5, 0, 0), "P001", "1", "5", "PIEZ", "DIR-2", "12.5"))));

        assertThat(result.errores()).isEmpty();
        assertThat(result.totalFilas()).isEqualTo(1);
    }

    @Test
    void folio50Accept() throws Exception {
        CatalogImportArchivo result = parse(workbook(false, List.of(
                fila("F".repeat(50), "", "", "", "", "", "", ""))));

        assertThat(codigos(result)).doesNotContain("FOLIO_ACTA_LARGO");
    }

    @Test
    void folio51Reject() throws Exception {
        CatalogImportArchivo result = parse(workbook(false, List.of(
                fila("F".repeat(51), "", "", "", "", "", "", ""))));

        assertThat(codigos(result)).contains("FOLIO_ACTA_LARGO");
    }

    @Test
    void clave50Accept() throws Exception {
        CatalogImportArchivo result = parse(workbook(false, List.of(
                fila("ACC-1", "", "C".repeat(50), "", "", "", "", ""))));

        assertThat(codigos(result)).doesNotContain("CLAVE_ACTA_LARGA");
    }

    @Test
    void clave51Reject() throws Exception {
        CatalogImportArchivo result = parse(workbook(false, List.of(
                fila("ACC-1", "", "C".repeat(51), "", "", "", "", ""))));

        assertThat(codigos(result)).contains("CLAVE_ACTA_LARGA");
    }

    @Test
    void umc50Boundary() throws Exception {
        CatalogImportArchivo acepta = parse(workbook(false, List.of(
                fila("ACC-1", "", "", "", "", "U".repeat(50), "", ""))));
        CatalogImportArchivo rechaza = parse(workbook(false, List.of(
                fila("ACC-1", "", "", "", "", "U".repeat(51), "", ""))));

        assertThat(codigos(acepta)).doesNotContain("UMC_ACTA_LARGA");
        assertThat(codigos(rechaza)).contains("UMC_ACTA_LARGA");
    }

    @Test
    void descargaDirigida50Boundary() throws Exception {
        CatalogImportArchivo acepta = parse(workbook(false, List.of(
                fila("ACC-1", "", "", "", "", "", "D".repeat(50), ""))));
        CatalogImportArchivo rechaza = parse(workbook(false, List.of(
                fila("ACC-1", "", "", "", "", "", "D".repeat(51), ""))));

        assertThat(codigos(acepta)).doesNotContain("DESCARGA_ACTA_LARGA");
        assertThat(codigos(rechaza)).contains("DESCARGA_ACTA_LARGA");
    }

    @Test
    void lineaIntMinMax() throws Exception {
        CatalogImportArchivo result = parse(workbook(false, List.of(
                fila("ACC-MAX", "", "P", "2147483647", "", "", "", ""),
                fila("ACC-MIN", "", "P", "-2147483648", "", "", "", ""))));

        assertThat(codigos(result)).doesNotContain("LINEA_ACTA_INVALIDA");
    }

    @Test
    void lineaOverflowReject() throws Exception {
        CatalogImportArchivo result = parse(workbook(false, List.of(
                fila("ACC-OVF", "", "P", "2147483648", "", "", "", ""))));

        assertThat(codigos(result)).contains("LINEA_ACTA_INVALIDA");
    }

    @Test
    void cantidadFinite() throws Exception {
        CatalogImportArchivo result = parse(workbook(false, List.of(
                fila("ACC-1", "", "", "", "123.45", "", "", ""))));

        assertThat(codigos(result)).doesNotContain("CANTIDAD_ACTA_INVALIDA");
    }

    @Test
    void cantidadNaNReject() throws Exception {
        CatalogImportArchivo result = parse(workbook(false, List.of(
                fila("ACC-1", "", "", "", "NaN", "", "", ""))));

        assertThat(codigos(result)).contains("CANTIDAD_ACTA_INVALIDA");
    }

    @Test
    void cantidadInfinityReject() throws Exception {
        CatalogImportArchivo result = parse(workbook(false, List.of(
                fila("ACC-1", "", "", "", "Infinity", "", "", ""))));

        assertThat(codigos(result)).contains("CANTIDAD_ACTA_INVALIDA");
    }

    @Test
    void valorComercialNumeric1810Boundary() throws Exception {
        CatalogImportArchivo result = parse(workbook(false, List.of(
                fila("ACC-1", "", "", "", "", "", "", "12345678.1234567890"))));

        assertThat(codigos(result)).doesNotContain("VALOR_COMERCIAL_ACTA_INVALIDO");
    }

    @Test
    void valorComercialScale11Reject() throws Exception {
        CatalogImportArchivo result = parse(workbook(false, List.of(
                fila("ACC-1", "", "", "", "", "", "", "1.12345678901"))));

        assertThat(codigos(result)).contains("VALOR_COMERCIAL_ACTA_INVALIDO");
    }

    @Test
    void valorComercialIntegerDigits9Reject() throws Exception {
        CatalogImportArchivo result = parse(workbook(false, List.of(
                fila("ACC-1", "", "", "", "", "", "", "123456789"))));

        assertThat(codigos(result)).contains("VALOR_COMERCIAL_ACTA_INVALIDO");
    }

    @Test
    void valorComercialConMasDe4DecimalesSigueContratoLegacy() throws Exception {
        CatalogImportArchivo result = parse(workbook(false, List.of(
                fila("ACC-1", "", "", "", "", "", "", "1.23456789"))));

        assertThat(codigos(result)).doesNotContain("VALOR_COMERCIAL_ACTA_INVALIDO");
        assertThat(result.filas().getFirst().datos()).containsEntry("ValorComercial", "1.23456789");
    }

    @Test
    void fechaExcelNativa() throws Exception {
        CatalogImportArchivo result = parse(workbook(false, List.of(
                fila("ACC-1", LocalDateTime.of(2026, 10, 5, 0, 0), "", "", "", "", "", ""))));

        assertThat(codigos(result)).doesNotContain("FECHA_ACTA_INVALIDA");
        assertThat(result.filas().getFirst().datos()).containsEntry("Fecha", "2026-10-05T00:00:00");
    }

    @Test
    void fechaIso() throws Exception {
        CatalogImportArchivo result = parse(workbook(false, List.of(
                fila("ACC-1", "2026-10-05", "", "", "", "", "", ""))));

        assertThat(codigos(result)).doesNotContain("FECHA_ACTA_INVALIDA");
        assertThat(result.filas().getFirst().datos()).containsEntry("Fecha", "2026-10-05T00:00:00");
    }

    @Test
    void fechaFueraRangoSqlDatetime() throws Exception {
        CatalogImportArchivo result = parse(workbook(false, List.of(
                fila("ACC-1", "1700-01-01", "", "", "", "", "", ""))));

        assertThat(codigos(result)).contains("FECHA_ACTA_INVALIDA");
    }

    @Test
    void duplicadoFolioLineaReject() throws Exception {
        CatalogImportArchivo result = parse(workbook(false, List.of(
                fila("ACC-DUP", "", "P001", "1", "", "", "", ""),
                fila("ACC-DUP", "", "P001", "1", "", "", "", ""))));

        assertThat(codigos(result)).contains("CLAVE_DUPLICADA");
    }

    @Test
    void mismoFolioDistintaLineaAccept() throws Exception {
        CatalogImportArchivo result = parse(workbook(false, List.of(
                fila("ACC-OK", "", "P001", "1", "", "", "", ""),
                fila("ACC-OK", "", "P001", "2", "", "", "", ""))));

        assertThat(codigos(result)).doesNotContain("CLAVE_DUPLICADA");
    }

    @Test
    void mismoFolioMismaLineaFechaDistintaReject() throws Exception {
        CatalogImportArchivo result = parse(workbook(false, List.of(
                fila("ACC-F", LocalDateTime.of(2026, 10, 5, 0, 0), "P001", "1", "", "", "", ""),
                fila("ACC-F", LocalDateTime.of(2026, 10, 6, 0, 0), "P001", "1", "", "", "", ""))));

        assertThat(codigos(result)).contains("CLAVE_DUPLICADA");
    }
}
