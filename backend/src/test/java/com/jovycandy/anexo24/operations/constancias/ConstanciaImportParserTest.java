package com.jovycandy.anexo24.operations.constancias;

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

/** Contrato del parser CONSTANCIA derivado de dbo.Constanciatransf (14 columnas consumidas). */
class ConstanciaImportParserTest {

    private static final List<String> HEADERS = List.of(
            "NUMERODEFOLIO", "SEC", "FECHACREACION", "PROV", "LIN", "NOPARTE", "DESCRIPCION",
            "CANTIDAD", "PEDIMENTO", "ADUANA", "MER", "PERIODO", "Val_dolares", "Val_Comercial");

    private final ExcelCatalogImportParser parser = new ExcelCatalogImportParser();

    private CatalogImportArchivo parse(byte[] bytes) {
        return parser.parsear(CatalogImportType.CONSTANCIA, "constancias.xlsx", "a".repeat(64), bytes);
    }

    private List<String> codigos(CatalogImportArchivo archivo) {
        return archivo.errores().stream().map(error -> error.codigo()).toList();
    }

    private List<Object> fila(Object folio, Object sec, Object fecha, Object prov, Object lin, Object noparte,
                              Object descripcion, Object cantidad, Object pedimento, Object aduana,
                              Object mer, Object periodo, Object valDolares, Object valComercial) {
        return java.util.Arrays.asList(folio, sec, fecha, prov, lin, noparte, descripcion, cantidad,
                pedimento, aduana, mer, periodo, valDolares, valComercial);
    }

    private List<Object> base() {
        return fila("CONST-1", "1", "2026-10-06", "PROV-1", "1", "P001", "Producto", "5",
                "PED-1", "ADU", "M", "2026-10-06", "10", "20");
    }

    private byte[] workbook(boolean xls, List<List<Object>> filas) throws Exception {
        try (Workbook wb = xls ? new HSSFWorkbook() : new XSSFWorkbook();
             ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            var sheet = wb.createSheet("CONSTANCIA");
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
    void constanciaXlsxValida() throws Exception {
        CatalogImportArchivo result = parse(workbook(false, List.of(base())));

        assertThat(result.errores()).isEmpty();
        assertThat(result.totalFilas()).isEqualTo(1);
        assertThat(result.columnas()).isEqualTo(HEADERS);
        assertThat(result.versionContrato()).isEqualTo("CONSTANCIA-V1");
    }

    @Test
    void constanciaXlsValida() throws Exception {
        CatalogImportArchivo result = parse(workbook(true, List.of(base())));

        assertThat(result.errores()).isEmpty();
        assertThat(result.totalFilas()).isEqualTo(1);
    }

    @Test
    void headerObligatorioAusente() throws Exception {
        List<String> sinCola = HEADERS.subList(0, HEADERS.size() - 1);
        List<Object> fila = base().subList(0, base().size() - 1);
        CatalogImportArchivo result = parse(libroXlsx(sinCola, List.of(fila)));

        assertThat(codigos(result)).contains("COLUMNA_OBLIGATORIA_AUSENTE");
    }

    @Test
    void valoresObligatoriosNoSeExigen() throws Exception {
        CatalogImportArchivo result = parse(workbook(false, List.of(
                fila("CONST-1", "", "", "", "", "", "", "", "", "", "", "", "", ""))));

        assertThat(codigos(result)).doesNotContain("VALOR_OBLIGATORIO_AUSENTE");
    }

    @Test
    void numerodefolio50AcceptY51Reject() throws Exception {
        CatalogImportArchivo acepta = parse(workbook(false, List.of(
                fila("F".repeat(50), "", "", "", "", "", "", "", "", "", "", "", "", ""))));
        CatalogImportArchivo rechaza = parse(workbook(false, List.of(
                fila("F".repeat(51), "", "", "", "", "", "", "", "", "", "", "", "", ""))));

        assertThat(codigos(acepta)).doesNotContain("NUMERODEFOLIO_CONSTANCIA_LARGO");
        assertThat(codigos(rechaza)).contains("NUMERODEFOLIO_CONSTANCIA_LARGO");
    }

    @Test
    void sec5Boundary() throws Exception {
        CatalogImportArchivo acepta = parse(workbook(false, List.of(
                fila("CONST-1", "12345", "", "", "", "", "", "", "", "", "", "", "", ""))));
        CatalogImportArchivo rechaza = parse(workbook(false, List.of(
                fila("CONST-1", "123456", "", "", "", "", "", "", "", "", "", "", "", ""))));

        assertThat(codigos(acepta)).doesNotContain("SEC_CONSTANCIA_LARGA");
        assertThat(codigos(rechaza)).contains("SEC_CONSTANCIA_LARGA");
    }

    @Test
    void descripcion100Boundary() throws Exception {
        CatalogImportArchivo acepta = parse(workbook(false, List.of(
                fila("CONST-1", "", "", "", "", "", "D".repeat(100), "", "", "", "", "", "", ""))));
        CatalogImportArchivo rechaza = parse(workbook(false, List.of(
                fila("CONST-1", "", "", "", "", "", "D".repeat(101), "", "", "", "", "", "", ""))));

        assertThat(codigos(acepta)).doesNotContain("DESCRIPCION_CONSTANCIA_LARGA");
        assertThat(codigos(rechaza)).contains("DESCRIPCION_CONSTANCIA_LARGA");
    }

    @Test
    void linEntero18BoundaryYOverflow() throws Exception {
        CatalogImportArchivo acepta = parse(workbook(false, List.of(
                fila("CONST-1", "", "", "", "999999999999999999", "", "", "", "", "", "", "", "", ""))));
        CatalogImportArchivo rechaza = parse(workbook(false, List.of(
                fila("CONST-1", "", "", "", "1000000000000000000", "", "", "", "", "", "", "", "", ""))));

        assertThat(codigos(acepta)).doesNotContain("LIN_CONSTANCIA_INVALIDO");
        assertThat(codigos(rechaza)).contains("LIN_CONSTANCIA_INVALIDO");
    }

    @Test
    void valDolares1810BoundaryYRechazos() throws Exception {
        CatalogImportArchivo boundary = parse(workbook(false, List.of(
                fila("CONST-1", "", "", "", "", "", "", "", "", "", "", "", "12345678.1234567890", ""))));
        CatalogImportArchivo escala = parse(workbook(false, List.of(
                fila("CONST-1", "", "", "", "", "", "", "", "", "", "", "", "1.12345678901", ""))));
        CatalogImportArchivo enteros = parse(workbook(false, List.of(
                fila("CONST-1", "", "", "", "", "", "", "", "", "", "", "", "123456789", ""))));

        assertThat(codigos(boundary)).doesNotContain("VAL_DOLARES_CONSTANCIA_INVALIDO");
        assertThat(codigos(escala)).contains("VAL_DOLARES_CONSTANCIA_INVALIDO");
        assertThat(codigos(enteros)).contains("VAL_DOLARES_CONSTANCIA_INVALIDO");
    }

    @Test
    void fechaExcelNativaYFechaIso() throws Exception {
        CatalogImportArchivo nativa = parse(workbook(false, List.of(
                fila("CONST-1", "", LocalDateTime.of(2026, 10, 6, 0, 0), "", "", "", "", "", "", "", "", "", "", ""))));
        CatalogImportArchivo iso = parse(workbook(false, List.of(
                fila("CONST-1", "", "2026-10-06", "", "", "", "", "", "", "", "", "", "", ""))));

        assertThat(codigos(nativa)).doesNotContain("FECHA_CONSTANCIA_INVALIDA");
        assertThat(nativa.filas().getFirst().datos()).containsEntry("FECHACREACION", "2026-10-06T00:00:00");
        assertThat(codigos(iso)).doesNotContain("FECHA_CONSTANCIA_INVALIDA");
        assertThat(iso.filas().getFirst().datos()).containsEntry("FECHACREACION", "2026-10-06T00:00:00");
    }

    @Test
    void fechaFueraDeRangoRechaza() throws Exception {
        CatalogImportArchivo result = parse(workbook(false, List.of(
                fila("CONST-1", "", "1700-01-01", "", "", "", "", "", "", "", "", "1500-01-01", "", ""))));

        assertThat(codigos(result)).contains("FECHA_CONSTANCIA_INVALIDA", "PERIODO_CONSTANCIA_INVALIDO");
    }

    @Test
    void duplicadosNoSeRechazanSegunContratoLegacy() throws Exception {
        CatalogImportArchivo result = parse(workbook(false, List.of(base(), base())));

        assertThat(codigos(result)).doesNotContain("CLAVE_DUPLICADA");
        assertThat(result.filasInvalidas()).isZero();
    }

    private byte[] libroXlsx(List<String> headers, List<List<Object>> filas) throws Exception {
        try (Workbook wb = new XSSFWorkbook(); ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            var sheet = wb.createSheet("CONSTANCIA");
            var header = sheet.createRow(0);
            for (int i = 0; i < headers.size(); i++) header.createCell(i).setCellValue(headers.get(i));
            for (int r = 0; r < filas.size(); r++) {
                var row = sheet.createRow(r + 1);
                for (int c = 0; c < filas.get(r).size(); c++) row.createCell(c).setCellValue(String.valueOf(filas.get(r).get(c)));
            }
            wb.write(out);
            return out.toByteArray();
        }
    }
}
