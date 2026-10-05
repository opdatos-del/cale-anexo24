package com.jovycandy.anexo24.catalogs.imports.application.command;

import com.jovycandy.anexo24.catalogs.imports.domain.model.CatalogImportArchivo;
import com.jovycandy.anexo24.catalogs.imports.domain.model.CatalogImportType;
import org.apache.poi.hssf.usermodel.HSSFWorkbook;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.apache.poi.ss.usermodel.Workbook;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/** Pruebas del contrato derivado de los stages legacy sin ejecutar sus SP mutables. */
class ExcelCatalogImportParserTest {
    private final ExcelCatalogImportParser parser = new ExcelCatalogImportParser();

    @Test
    void aceptaMaterialXlsxValido() throws Exception {
        CatalogImportArchivo result = parser.parsear(CatalogImportType.MATERIAL, "materiales.xlsx", hash(),
                workbook(false, List.of("ClaveMaterial", "UnidadComercial", "Fraccion"),
                        List.of("MAT001", "KG", "12345678")));

        assertThat(result.errores()).isEmpty();
        assertThat(result.totalFilas()).isEqualTo(1);
        assertThat(result.filasValidas()).isEqualTo(1);
    }

    @Test
    void aceptaMaterialXlsValido() throws Exception {
        CatalogImportArchivo result = parser.parsear(CatalogImportType.MATERIAL, "materiales.xls", hash(),
                workbook(true, List.of("ClaveMaterial", "UnidadComercial", "Fraccion"),
                        List.of("MAT001", "KG", "12345678")));

        assertThat(result.errores()).isEmpty();
        assertThat(result.totalFilas()).isEqualTo(1);
    }

    @Test
    void aceptaProductoXlsValido() throws Exception {
        CatalogImportArchivo result = parser.parsear(CatalogImportType.PRODUCTO, "productos.xls", hash(),
                workbook(true, List.of("CVE_PRODUCTO", "NOMBRE", "UNIDAD", "fraccion"),
                        List.of("PROD001", "Producto sintético", "PZA", "12345678")));

        assertThat(result.errores()).isEmpty();
        assertThat(result.filas()).singleElement().satisfies(row ->
                assertThat(row.datos()).containsEntry("CVE_PRODUCTO", "PROD001"));
    }

    @Test
    void aceptaProductoXlsxValido() throws Exception {
        CatalogImportArchivo result = parser.parsear(CatalogImportType.PRODUCTO, "productos.xlsx", hash(),
                workbook(false, List.of("CVE_PRODUCTO", "NOMBRE", "UNIDAD", "fraccion"),
                        List.of("PROD001", "Producto sintético", "PZA", "12345678")));

        assertThat(result.errores()).isEmpty();
        assertThat(result.totalFilas()).isEqualTo(1);
    }

    @Test
    void informaEncabezadoObligatorioAusente() throws Exception {
        CatalogImportArchivo result = parser.parsear(CatalogImportType.MATERIAL, "materiales.xlsx", hash(),
                workbook(false, List.of("ClaveMaterial", "UnidadComercial"), List.of("MAT001", "KG")));

        assertThat(result.errores()).extracting(error -> error.codigo())
                .contains("COLUMNA_OBLIGATORIA_AUSENTE", "FRACCION_INVALIDA");
    }

    @Test
    void rechazaDecimalInvalidoYSolicitaCorreccion() throws Exception {
        CatalogImportArchivo result = parser.parsear(CatalogImportType.MATERIAL, "materiales.xlsx", hash(),
                workbook(false, List.of("ClaveMaterial", "UnidadComercial", "Fraccion", "KG"),
                        List.of("MAT001", "KG", "12345678", "no-decimal")));

        assertThat(result.errores()).extracting(error -> error.codigo()).contains("DECIMAL_INVALIDO");
        assertThat(result.filasInvalidas()).isEqualTo(1);
    }

    @Test
    void rechazaClaveDuplicadaSinPersistirNada() throws Exception {
        CatalogImportArchivo result = parser.parsear(CatalogImportType.PRODUCTO, "productos.xlsx", hash(),
                workbook(false, List.of("CVE_PRODUCTO", "NOMBRE", "UNIDAD", "fraccion"),
                        List.of("PROD001", "Producto", "PZA", "12345678"),
                        List.of("PROD001", "Otro producto", "PZA", "12345678")));

        assertThat(result.errores()).extracting(error -> error.codigo()).contains("CLAVE_DUPLICADA");
        assertThat(result.filas()).hasSize(2);
        assertThat(result.filasInvalidas()).isEqualTo(1);
    }

    @Test
    void aceptaClienteXlsxValido() throws Exception {
        CatalogImportArchivo result = parser.parsear(CatalogImportType.CLIENTE, "clientes.xlsx", hash(),
                workbook(false,
                        List.of("Clave", "Nombre", "IdFiscal", "TipoNE"),
                        List.of("CLI001", "Cliente sintético", "XAXX010101000", "01")));

        assertThat(result.errores()).isEmpty();
        assertThat(result.totalFilas()).isEqualTo(1);
        assertThat(result.filasValidas()).isEqualTo(1);
    }

    @Test
    void aceptaClienteXlsValido() throws Exception {
        CatalogImportArchivo result = parser.parsear(CatalogImportType.CLIENTE, "clientes.xls", hash(),
                workbook(true,
                        List.of("Clave", "Nombre", "IdFiscal", "TipoNE"),
                        List.of("CLI001", "Cliente sintético", "XAXX010101000", "01")));

        assertThat(result.errores()).isEmpty();
        assertThat(result.totalFilas()).isEqualTo(1);
    }

    @Test
    void rechazaClaveClienteMayorA15() throws Exception {
        String claveLarga = "A".repeat(16);
        CatalogImportArchivo result = parser.parsear(CatalogImportType.CLIENTE, "clientes.xlsx", hash(),
                workbook(false,
                        List.of("Clave", "Nombre", "IdFiscal", "TipoNE"),
                        List.of(claveLarga, "Cliente grande", "XAXX010101000", "01")));

        assertThat(result.errores()).extracting(error -> error.codigo())
                .contains("CLAVE_CLIENTE_LARGA");
        assertThat(result.filasInvalidas()).isEqualTo(1);
    }

    @Test
    void rechazaClaveClienteDuplicada() throws Exception {
        CatalogImportArchivo result = parser.parsear(CatalogImportType.CLIENTE, "clientes.xlsx", hash(),
                workbook(false,
                        List.of("Clave", "Nombre", "IdFiscal", "TipoNE"),
                        List.of("CLI001", "Cliente A", "XAXX010101000", "01"),
                        List.of("CLI001", "Cliente B", "XAXX010101000", "01")));

        assertThat(result.errores()).extracting(error -> error.codigo())
                .contains("CLAVE_DUPLICADA");
        assertThat(result.filasInvalidas()).isEqualTo(1);
    }

    @Test
    void rechazaIdFiscalVacio() throws Exception {
        CatalogImportArchivo result = parser.parsear(CatalogImportType.CLIENTE, "clientes.xlsx", hash(),
                workbook(false,
                        List.of("Clave", "Nombre", "IdFiscal", "TipoNE"),
                        List.of("CLI001", "Cliente sin fiscal", "", "01")));

        // IdFiscal no es required explícito pero no puede ir vacío por restricción legacy;
        // el parser lo marca como VALOR_OBLIGATORIO_AUSENTE en cualquier caso.
        assertThat(result.errores()).extracting(error -> error.codigo())
                .contains("VALOR_OBLIGATORIO_AUSENTE");
        assertThat(result.filasInvalidas()).isEqualTo(1);
    }

    private static final List<String> AGENTE_HEADERS = List.of("Clave", "Nombre", "Domicilio", "Rfc", "Patente");

    @Test
    void aceptaAgenteXlsxValido() throws Exception {
        CatalogImportArchivo result = parser.parsear(CatalogImportType.AGENTE, "agentes.xlsx", hash(),
                workbook(false, AGENTE_HEADERS, filaAgenteValida()));

        assertThat(result.errores()).isEmpty();
        assertThat(result.totalFilas()).isEqualTo(1);
        assertThat(result.filasValidas()).isEqualTo(1);
        assertThat(result.columnas()).isEqualTo(AGENTE_HEADERS);
    }

    @Test
    void aceptaAgenteXlsValido() throws Exception {
        CatalogImportArchivo result = parser.parsear(CatalogImportType.AGENTE, "agentes.xls", hash(),
                workbook(true, AGENTE_HEADERS, filaAgenteValida()));

        assertThat(result.errores()).isEmpty();
        assertThat(result.totalFilas()).isEqualTo(1);
    }

    @Test
    void rechazaClaveAgenteMayorA10() throws Exception {
        CatalogImportArchivo result = parser.parsear(CatalogImportType.AGENTE, "agentes.xlsx", hash(),
                workbook(false, AGENTE_HEADERS,
                        List.of("A".repeat(11), "Agente largo", "Calle 1", "AAA010101AAA0", "1234")));

        assertThat(result.errores()).extracting(error -> error.codigo())
                .contains("CLAVE_AGENTE_LARGA");
        assertThat(result.filasInvalidas()).isEqualTo(1);
    }

    @Test
    void rechazaPatenteVacia() throws Exception {
        CatalogImportArchivo result = parser.parsear(CatalogImportType.AGENTE, "agentes.xlsx", hash(),
                workbook(false, AGENTE_HEADERS,
                        List.of("AGE001", "Agente sin patente", "Calle 1", "AAA010101AAA0", "")));

        // Patente es required y además el legacy exige LEN = 4.
        assertThat(result.errores()).extracting(error -> error.codigo())
                .contains("VALOR_OBLIGATORIO_AUSENTE", "PATENTE_AGENTE_INVALIDA");
        assertThat(result.filasInvalidas()).isEqualTo(1);
    }

    @Test
    void rechazaPatenteConLongitudDistintaDe4() throws Exception {
        CatalogImportArchivo result = parser.parsear(CatalogImportType.AGENTE, "agentes.xlsx", hash(),
                workbook(false, AGENTE_HEADERS,
                        List.of("AGE001", "Agente patente corta", "Calle 1", "AAA010101AAA0", "123"),
                        List.of("AGE002", "Agente patente larga", "Calle 2", "AAA010101AAA1", "12345")));

        assertThat(result.errores()).extracting(error -> error.codigo())
                .containsOnly("PATENTE_AGENTE_INVALIDA");
        assertThat(result.filasInvalidas()).isEqualTo(2);
    }

    @Test
    void mensajePatenteInvalidaHablaDePatenteYNoDeFraccion() throws Exception {
        CatalogImportArchivo result = parser.parsear(CatalogImportType.AGENTE, "agentes.xlsx", hash(),
                workbook(false, AGENTE_HEADERS,
                        List.of("AGE001", "Agente patente corta", "Calle 1", "AAA010101AAA0", "123")));

        assertThat(result.errores()).extracting(error -> error.mensaje())
                .contains("La patente debe tener exactamente 4 caracteres en el contrato legacy.");
        assertThat(result.errores()).noneMatch(error -> error.mensaje().contains("fracción"));
    }

    @Test
    void rechazaClaveAgenteDuplicada() throws Exception {
        CatalogImportArchivo result = parser.parsear(CatalogImportType.AGENTE, "agentes.xlsx", hash(),
                workbook(false, AGENTE_HEADERS,
                        List.of("AGE001", "Agente A", "Calle 1", "AAA010101AAA0", "1234"),
                        List.of("AGE001", "Agente B", "Calle 2", "AAA010101AAA1", "5678")));

        assertThat(result.errores()).extracting(error -> error.codigo())
                .contains("CLAVE_DUPLICADA");
        assertThat(result.filasInvalidas()).isEqualTo(1);
    }

    @Test
    void rechazaNombreMayorA40() throws Exception {
        CatalogImportArchivo result = parser.parsear(CatalogImportType.AGENTE, "agentes.xlsx", hash(),
                workbook(false, AGENTE_HEADERS,
                        List.of("AGE001", "N".repeat(41), "Calle 1", "AAA010101AAA0", "1234")));

        assertThat(result.errores()).extracting(error -> error.codigo())
                .contains("NOMBRE_AGENTE_LARGO");
        assertThat(result.filasInvalidas()).isEqualTo(1);
    }

    @Test
    void rechazaRfcMayorA20() throws Exception {
        CatalogImportArchivo result = parser.parsear(CatalogImportType.AGENTE, "agentes.xlsx", hash(),
                workbook(false, AGENTE_HEADERS,
                        List.of("AGE001", "Agente rfc largo", "Calle 1", "R".repeat(21), "1234")));

        assertThat(result.errores()).extracting(error -> error.codigo())
                .contains("RFC_AGENTE_LARGO");
        assertThat(result.filasInvalidas()).isEqualTo(1);
    }

    @Test
    void mensajeFraccionInvalidaSeMantieneParaMateriales() throws Exception {
        CatalogImportArchivo result = parser.parsear(CatalogImportType.MATERIAL, "materiales.xlsx", hash(),
                workbook(false,
                        List.of("ClaveMaterial", "UnidadComercial", "Fraccion"),
                        List.of("MAT001", "PZA", "123")));

        assertThat(result.errores()).extracting(error -> error.codigo())
                .contains("FRACCION_INVALIDA");
        assertThat(result.errores()).extracting(error -> error.mensaje())
                .contains("La fracción debe tener exactamente 8 caracteres en el contrato legacy.");
    }

    private List<String> filaAgenteValida() {
        return List.of("AGE001", "Agente Aduanal Uno", "Calle 1 numero 2", "AAA010101AAA0", "1234");
    }

    private byte[] workbook(boolean legacyXls, List<String> headers, List<String>... rows) throws IOException {
        try (Workbook workbook = legacyXls ? new HSSFWorkbook() : new XSSFWorkbook();
             ByteArrayOutputStream output = new ByteArrayOutputStream()) {
            var sheet = workbook.createSheet("CATALOGO");
            var header = sheet.createRow(0);
            for (int i = 0; i < headers.size(); i++) header.createCell(i).setCellValue(headers.get(i));
            for (int rowIndex = 0; rowIndex < rows.length; rowIndex++) {
                var row = sheet.createRow(rowIndex + 1);
                for (int column = 0; column < rows[rowIndex].size(); column++)
                    row.createCell(column).setCellValue(rows[rowIndex].get(column));
            }
            workbook.write(output);
            return output.toByteArray();
        }
    }

    private String hash() { return "a".repeat(64); }
}
