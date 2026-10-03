package com.jovycandy.anexo24.catalogs.imports.application.command;

import com.jovycandy.anexo24.catalogs.imports.domain.model.CatalogImportArchivo;
import com.jovycandy.anexo24.catalogs.imports.domain.model.CatalogImportError;
import com.jovycandy.anexo24.catalogs.imports.domain.model.CatalogImportFila;
import com.jovycandy.anexo24.catalogs.imports.domain.model.CatalogImportType;
import org.apache.poi.openxml4j.util.ZipSecureFile;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellType;
import org.apache.poi.ss.usermodel.DataFormatter;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.ss.usermodel.WorkbookFactory;
import org.springframework.stereotype.Component;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.math.BigDecimal;
import java.text.Normalizer;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

/** Parser explícito para los stages legacy CargaMaterial y tmpproductos. */
@Component
public class ExcelCatalogImportParser {
    public static final String VERSION_CONTRATO = "LEGACY-CATALOG-STAGE-DERIVED-V1";
    public static final int MAX_FILAS = 2_000;
    public static final int MAX_COLUMNAS = 40;
    public static final int MAX_CELDAS = 100_000;
    private static final int MAX_ERRORES = 500;

    private static final List<String> MATERIAL_COLUMNS = List.of(
            "ClaveMaterial", "ClaveMaterialProveedor", "DescripcionComercial", "UnidadComercial",
            "UnidadTarifa", "Fraccion", "KG", "GR", "ML", "MCUA", "MCUB", "PZA", "LT", "PAR",
            "MI", "JGO", "TON", "BAR", "GRN", "DECE", "CIEN", "DOCE", "CAJA", "BOTELLA",
            "DIVISION", "ENTIDAD", "TIPOM", "NumeroSerie", "MARCA", "MODELO");
    private static final List<String> PRODUCT_COLUMNS = List.of(
            "CVE_PRODUCTO", "NOMBRE", "UNIDAD", "fraccion", "DIVISION", "CVE_PRODUCTO_CLIENTE", "AUXILIAR");
    private static final List<String> CLIENT_COLUMNS = List.of(
            "Clave", "Nombre", "IdFiscal", "TipoNE", "Programa", "CalleNumero", "Codigo", "Colonia",
            "Entidad", "Pais", "Telefono", "Correo", "Fax",
            "ApellidoPaterno", "ApellidoMaterno", "Calle", "CalleNumeroInterior",
            "Localidad", "Referencia", "Municipio", "TipoIdentificador", "CodigoPostal");
    private static final int CLIENT_KEY_MAX_LENGTH = 15;
    private static final Set<String> MATERIAL_DECIMALS = Set.of(
            "KG", "GR", "ML", "MCUA", "MCUB", "PZA", "LT", "PAR", "MI", "JGO", "TON", "BAR",
            "GRN", "DECE", "CIEN", "DOCE", "CAJA", "BOTELLA");

    static {
        ZipSecureFile.setMinInflateRatio(0.01d);
        ZipSecureFile.setMaxEntrySize(10L * 1024 * 1024);
        ZipSecureFile.setMaxTextSize(10L * 1024 * 1024);
        ZipSecureFile.setMaxFileCount(200);
    }

    public CatalogImportArchivo parsear(CatalogImportType type, String filename, String hash, byte[] bytes) {
        List<CatalogImportError> errors = new ArrayList<>();
        List<CatalogImportFila> rows = new ArrayList<>();
        List<String> columns = columns(type);
        try (Workbook workbook = WorkbookFactory.create(new ByteArrayInputStream(bytes))) {
            if (workbook.getNumberOfSheets() != 1) {
                add(errors, error(null, null, null, "HOJAS_NO_SOPORTADAS", "El contrato V1 admite exactamente una hoja."));
                return result(type, filename, hash, columns, rows, errors, false);
            }
            Sheet sheet = workbook.getSheetAt(0);
            if (sheet.getLastRowNum() > MAX_FILAS) throw new ArchivoInvalidoException();
            Row header = sheet.getRow(sheet.getFirstRowNum());
            if (header == null || header.getLastCellNum() <= 0) {
                add(errors, error(sheet.getSheetName(), null, null, "ENCABEZADO_AUSENTE", "La hoja no contiene encabezado."));
                return result(type, filename, hash, columns, rows, errors, false);
            }
            if (header.getLastCellNum() > MAX_COLUMNAS) throw new ArchivoInvalidoException();
            DataFormatter formatter = new DataFormatter(Locale.ROOT);
            Map<String, Integer> indexes = new LinkedHashMap<>();
            Set<String> duplicateHeaders = new HashSet<>();
            Set<String> expected = columns.stream().map(ExcelCatalogImportParser::normalize).collect(Collectors.toSet());
            for (int i = 0; i < header.getLastCellNum(); i++) {
                Cell cell = header.getCell(i);
                String label = cell == null ? "" : formatter.formatCellValue(cell).trim();
                String normalized = normalize(label);
                if (!normalized.isBlank() && indexes.putIfAbsent(normalized, i) != null) {
                    duplicateHeaders.add(normalized);
                    add(errors, error(sheet.getSheetName(), null, label, "COLUMNA_DUPLICADA", "El encabezado se repite."));
                }
                if (!normalized.isBlank() && !expected.contains(normalized))
                    add(errors, error(sheet.getSheetName(), null, label, "COLUMNA_NO_CONFIRMADA", "La columna no pertenece al stage auditado."));
                if (cell != null && cell.getCellType() == CellType.FORMULA)
                    add(errors, error(sheet.getSheetName(), null, label, "FORMULA", "No se admiten fórmulas."));
            }
            for (String required : required(type))
                if (!indexes.containsKey(normalize(required)))
                    add(errors, error(sheet.getSheetName(), null, required, "COLUMNA_OBLIGATORIA_AUSENTE", "Falta una columna confirmada como requerida."));

            int totalCells = Math.max(0, header.getLastCellNum());
            Set<String> seenKeys = new HashSet<>();
            for (int rowNumber = header.getRowNum() + 1; rowNumber <= sheet.getLastRowNum(); rowNumber++) {
                Row row = sheet.getRow(rowNumber);
                totalCells += row == null ? 0 : Math.max(0, row.getLastCellNum());
                if (totalCells > MAX_CELDAS) throw new ArchivoInvalidoException();
                Map<String, String> values = new LinkedHashMap<>();
                boolean blank = true;
                for (String column : columns) {
                    Integer index = indexes.get(normalize(column));
                    Cell cell = index == null || row == null ? null : row.getCell(index);
                    String value = cell == null ? "" : formatter.formatCellValue(cell).trim();
                    String canonical = canonical(column, value);
                    values.put(column, canonical);
                    blank &= canonical.isBlank();
                    if (cell != null && cell.getCellType() == CellType.FORMULA)
                        add(errors, error(sheet.getSheetName(), rowNumber + 1, column, "FORMULA", "No se admiten fórmulas."));
                    if (index != null && !duplicateHeaders.contains(normalize(column)) && required(type).contains(column)
                            && canonical.isBlank())
                        add(errors, error(sheet.getSheetName(), rowNumber + 1, column, "VALOR_OBLIGATORIO_AUSENTE", "El valor requerido está vacío."));
                    if (type == CatalogImportType.MATERIAL && MATERIAL_DECIMALS.contains(column)
                            && !value.isBlank() && canonical.isBlank())
                        add(errors, error(sheet.getSheetName(), rowNumber + 1, column, "DECIMAL_INVALIDO", "El factor debe ser decimal positivo."));
                }
                if (blank) add(errors, error(sheet.getSheetName(), rowNumber + 1, null, "FILA_VACIA", "La fila no contiene datos."));
                validateBusiness(type, values, sheet.getSheetName(), rowNumber + 1, errors);
                String keyColumn = keyColumn(type);
                String key = values.getOrDefault(keyColumn, "");
                if (!key.isBlank() && !seenKeys.add(normalize(key)))
                    add(errors, error(sheet.getSheetName(), rowNumber + 1, keyColumn,
                            "CLAVE_DUPLICADA", "La clave se repite en el archivo."));
                rows.add(new CatalogImportFila(safe(sheet.getSheetName(), 80), rowNumber + 1, Map.copyOf(values)));
            }
            if (rows.isEmpty()) add(errors, error(sheet.getSheetName(), null, null, "SIN_REGISTROS", "El archivo no contiene filas de datos."));
            return result(type, filename, hash, columns, rows, errors, false);
        } catch (IOException | RuntimeException exception) {
            return result(type, filename, hash, columns, rows,
                    List.of(error(null, null, null, "ARCHIVO_NO_LEIBLE", exception instanceof ArchivoInvalidoException
                            ? "El archivo excede los límites técnicos." : "No fue posible leer el libro Excel.")), true);
        }
    }

    private void validateBusiness(CatalogImportType type, Map<String, String> values, String sheet,
                                  int row, List<CatalogImportError> errors) {
        switch (type) {
            case MATERIAL -> {
                validateLength(values, "ClaveMaterial", 5, sheet, row, errors, "CLAVE_MATERIAL_CORTA");
                validateLengthIfPresent(values, "ClaveMaterialProveedor", 5, sheet, row, errors, "CLAVE_PROVEEDOR_CORTA");
                validateExactLength(values, "Fraccion", 8, sheet, row, errors, "FRACCION_INVALIDA");
            }
            case PRODUCTO -> {
                validateLength(values, "CVE_PRODUCTO", 3, sheet, row, errors, "CLAVE_PRODUCTO_CORTA");
                validateLength(values, "NOMBRE", 3, sheet, row, errors, "NOMBRE_PRODUCTO_CORTO");
                validateExactLength(values, "fraccion", 8, sheet, row, errors, "FRACCION_INVALIDA");
                validateLengthIfPresent(values, "CVE_PRODUCTO_CLIENTE", 3, sheet, row, errors, "CLAVE_CLIENTE_CORTA");
            }
            case CLIENTE -> {
                // Restricción física del destino: clientes.CLAVE = CHAR(15). Aplicar LEFT(Clave,15) en el
                // SP legacy hace que claves >15 choquen. Validación preventiva para que nunca lleguen a confirmación.
                validateMaxLength(values, "Clave", CLIENT_KEY_MAX_LENGTH, sheet, row, errors, "CLAVE_CLIENTE_LARGA");
                validateLength(values, "Clave", 3, sheet, row, errors, "CLAVE_CLIENTE_CORTA");
                validateLengthIfPresent(values, "IdFiscal", 1, sheet, row, errors, "ID_FISCAL_VACIO");
            }
        }
    }

    private void validateLength(Map<String, String> values, String key, int min, String sheet, int row,
                                List<CatalogImportError> errors, String code) {
        if (values.getOrDefault(key, "").length() < min)
            add(errors, error(sheet, row, key, code, "El valor no alcanza la longitud mínima auditada."));
    }

    private void validateLengthIfPresent(Map<String, String> values, String key, int min, String sheet, int row,
                                         List<CatalogImportError> errors, String code) {
        String value = values.getOrDefault(key, "");
        if (!value.isBlank() && value.length() < min) add(errors, error(sheet, row, key, code, "El valor informado no alcanza la longitud mínima auditada."));
    }

    private void validateExactLength(Map<String, String> values, String key, int length, String sheet,
                                    int row, List<CatalogImportError> errors, String code) {
        if (values.getOrDefault(key, "").length() != length)
            add(errors, error(sheet, row, key, code, "La fracción debe tener exactamente 8 caracteres en el contrato legacy."));
    }

    private void validateMaxLength(Map<String, String> values, String key, int max, String sheet, int row,
                                   List<CatalogImportError> errors, String code) {
        if (values.getOrDefault(key, "").length() > max)
            add(errors, error(sheet, row, key, code, "El valor supera la longitud máxima física del destino (" + max + ")."));
    }

    private String canonical(String column, String value) {
        String trimmed = value == null ? "" : value.trim();
        if (trimmed.isBlank()) return "";
        if (MATERIAL_DECIMALS.contains(column)) {
            try {
                if (!trimmed.matches("[+]?(?:0|[1-9]\\d*)(?:\\.\\d+)?")) return "";
                BigDecimal decimal = new BigDecimal(trimmed);
                return decimal.signum() > 0 ? decimal.toPlainString() : "";
            } catch (NumberFormatException ignored) { return ""; }
        }
        return safe(trimmed, column.equals("DescripcionComercial") || column.equals("NOMBRE") ? 250 : 150);
    }

    private CatalogImportArchivo result(CatalogImportType type, String filename, String hash, List<String> columns,
                                        List<CatalogImportFila> rows, List<CatalogImportError> errors, boolean failed) {
        return new CatalogImportArchivo(type, safe(filename == null ? "archivo" : filename.replaceAll("[\\r\\n\\\\/]", "_"), 255),
                hash, VERSION_CONTRATO, List.copyOf(columns), List.copyOf(rows), List.copyOf(errors), failed);
    }

    private List<String> columns(CatalogImportType type) {
            return switch (type) {
                case MATERIAL -> MATERIAL_COLUMNS;
                case PRODUCTO -> PRODUCT_COLUMNS;
                case CLIENTE -> CLIENT_COLUMNS;
            };
        }

    private List<String> required(CatalogImportType type) {
        return switch (type) {
            case MATERIAL -> List.of("ClaveMaterial", "UnidadComercial", "Fraccion");
            case PRODUCTO -> List.of("CVE_PRODUCTO", "NOMBRE", "UNIDAD", "fraccion");
            case CLIENTE -> List.of("Clave", "Nombre", "IdFiscal", "TipoNE");
        };
    }

    private String keyColumn(CatalogImportType type) {
        return switch (type) {
            case MATERIAL -> "ClaveMaterial";
            case PRODUCTO -> "CVE_PRODUCTO";
            case CLIENTE -> "Clave";
        };
    }

    private void add(List<CatalogImportError> errors, CatalogImportError error) {
        if (errors.size() < MAX_ERRORES) errors.add(error);
    }

    private CatalogImportError error(String sheet, Integer row, String column, String code, String message) {
        return new CatalogImportError(safe(sheet, 80), row, safe(column, 80), "no almacenado", code, message);
    }

    private static String safe(String value, int max) { return value == null ? null : value.substring(0, Math.min(value.length(), max)); }

    public static String normalize(String value) {
        return Normalizer.normalize(value == null ? "" : value, Normalizer.Form.NFD)
                .replaceAll("\\p{M}+", "").replaceAll("[^a-zA-Z0-9]", "").toLowerCase(Locale.ROOT);
    }

    public static class ArchivoInvalidoException extends RuntimeException { }
}
