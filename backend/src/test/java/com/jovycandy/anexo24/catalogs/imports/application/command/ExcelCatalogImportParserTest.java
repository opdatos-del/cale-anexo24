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
