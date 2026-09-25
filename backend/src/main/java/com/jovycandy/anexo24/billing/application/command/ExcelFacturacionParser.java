package com.jovycandy.anexo24.billing.application.command;

import com.jovycandy.anexo24.billing.domain.model.ArchivoFacturacion;
import com.jovycandy.anexo24.billing.domain.model.PlantillaFacturacion;
import org.apache.poi.openxml4j.util.ZipSecureFile;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.ss.util.NumberToTextConverter;
import org.springframework.stereotype.Component;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.math.BigDecimal;
import java.text.Normalizer;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.*;

@Component
public class ExcelFacturacionParser {
    public static final int MAX_HOJAS = 5;
    public static final int MAX_FILAS = 2_000;
    public static final int MAX_COLUMNAS = 50;
    private static final int MAX_PREVIEW = 100;
    private static final int MAX_ERRORES = 500;
    private static final int MAX_CELDAS = 100_000;

    static {
        ZipSecureFile.setMinInflateRatio(0.01d);
        ZipSecureFile.setMaxEntrySize(10L * 1024 * 1024);
        ZipSecureFile.setMaxTextSize(10L * 1024 * 1024);
        ZipSecureFile.setMaxFileCount(200);
    }

    public ArchivoFacturacion parsear(String filename, String hash, byte[] bytes, PlantillaFacturacion plantilla) {
        Objects.requireNonNull(plantilla, "La plantilla activa es obligatoria");
        List<ArchivoFacturacion.Error> errors = new ArrayList<>();
        List<ArchivoFacturacion.Fila> preview = new ArrayList<>();
        List<ArchivoFacturacion.Fila> rows = new ArrayList<>();
        List<String> columns = plantilla.nombres();
        int totalRows = 0;
        int totalCells = 0;
        int sheets = 0;
        try (Workbook workbook = WorkbookFactory.create(new ByteArrayInputStream(bytes))) {
            sheets = workbook.getNumberOfSheets();
            if (sheets < 1 || sheets > MAX_HOJAS) throw new ArchivoInvalidoException();
            DataFormatter formatter = new DataFormatter(Locale.ROOT);
            Map<String, String> seenRows = new HashMap<>();
            for (int si = 0; si < sheets; si++) {
                Sheet sheet = workbook.getSheetAt(si);
                if (!plantilla.hoja().equals(sheet.getSheetName())) {
                    addError(errors, error(sheet.getSheetName(), null, null, "HOJA_NO_CONFIGURADA",
                            "La plantilla activa sólo admite la hoja " + plantilla.hoja() + "."));
                    continue;
                }
                if (sheet.getPhysicalNumberOfRows() > MAX_FILAS + 1 || sheet.getLastRowNum() > MAX_FILAS)
                    throw new ArchivoInvalidoException();
                Row header = sheet.getRow(sheet.getFirstRowNum());
                if (header == null) {
                    addError(errors, error(sheet.getSheetName(), null, null, "ESTRUCTURA", "La hoja no contiene encabezado."));
                    continue;
                }
                if (header.getLastCellNum() > MAX_COLUMNAS) throw new ArchivoInvalidoException();
                totalCells += Math.max(0, header.getLastCellNum());
                if (totalCells > MAX_CELDAS) throw new ArchivoInvalidoException();

                Map<String, Integer> headerIndexes = new LinkedHashMap<>();
                Set<String> duplicateHeaders = new HashSet<>();
                List<String> normalizedHeader = new ArrayList<>();
                for (int ci = 0; ci < header.getLastCellNum(); ci++) {
                    Cell cell = header.getCell(ci);
                    String text = cell == null ? "" : formatter.formatCellValue(cell).trim();
                    String normalized = normalize(text);
                    normalizedHeader.add(normalized);
                    if (!normalized.isBlank() && headerIndexes.putIfAbsent(normalized, ci) != null) {
                        duplicateHeaders.add(normalized);
                        addError(errors, error(sheet.getSheetName(), null, text, "COLUMNA_DUPLICADA",
                                "Encabezado duplicado tras normalizar espacios, acentos y mayúsculas."));
                    }
                    if (cell != null && cell.getCellType() == CellType.FORMULA)
                        addError(errors, error(sheet.getSheetName(), null, text, "FORMULA", "No se admiten fórmulas."));
                }
                validarEncabezado(plantilla, normalizedHeader, sheet.getSheetName(), errors);
                if (normalizedHeader.stream().noneMatch(value -> !value.isBlank()))
                    addError(errors, error(sheet.getSheetName(), null, null, "ENCABEZADO_VACIO", "La hoja requiere al menos un encabezado."));

                for (int ri = header.getRowNum() + 1; ri <= sheet.getLastRowNum(); ri++) {
                    Row row = sheet.getRow(ri);
                    totalRows++;
                    List<String> cells = new ArrayList<>(columns.size());
                    boolean blank = true;
                    boolean hasInput = false;
                    for (PlantillaFacturacion.Columna column : plantilla.columnas()) {
                        String normalizedName = normalize(column.nombre());
                        Integer physicalIndex = headerIndexes.get(normalizedName);
                        Cell cell = physicalIndex == null || row == null ? null : row.getCell(physicalIndex);
                        String value = cell == null ? "" : formatter.formatCellValue(cell).trim();
                        String canonical = canonicalValue(column, cell, value);
                        hasInput |= !value.isBlank();
                        blank &= canonical.isBlank();
                        cells.add(safeValue(canonical));
                        if (cell != null && cell.getCellType() == CellType.FORMULA)
                            addError(errors, error(sheet.getSheetName(), ri + 1, column.nombre(), "FORMULA", "No se admiten fórmulas."));
                        if (physicalIndex != null && duplicateHeaders.contains(normalizedName)) continue;
                        if (physicalIndex == null) continue;
                        if (column.obligatoria() && value.isBlank())
                            addError(errors, error(sheet.getSheetName(), ri + 1, column.nombre(),
                                    "VALOR_OBLIGATORIO_AUSENTE", "La columna obligatoria no puede estar vacía."));
                        if (column.tipo().equalsIgnoreCase("FECHA") && !value.isBlank() && canonical.isBlank())
                            addError(errors, error(sheet.getSheetName(), ri + 1, column.nombre(), "FECHA_INVALIDA", "La fecha no tiene un formato válido."));
                        if (column.tipo().equalsIgnoreCase("DECIMAL") && !value.isBlank() && canonical.isBlank())
                            addError(errors, error(sheet.getSheetName(), ri + 1, column.nombre(), "CANTIDAD_INVALIDA", "La cantidad debe ser un número decimal mayor que cero."));
                    }
                    if (row != null) {
                        totalCells += Math.max(0, row.getLastCellNum());
                        if (totalCells > MAX_CELDAS) throw new ArchivoInvalidoException();
                        for (int ci = 0; ci < row.getLastCellNum(); ci++) {
                            Cell cell = row.getCell(ci);
                            if (cell != null && cell.getCellType() == CellType.FORMULA)
                                addError(errors, error(sheet.getSheetName(), ri + 1, columnName(header, ci, formatter), "FORMULA", "No se admiten fórmulas."));
                        }
                    }
                    if (blank) addError(errors, error(sheet.getSheetName(), ri + 1, null, "FILA_VACIA", "La fila no contiene datos."));
                    String key = rowKey(cells);
                    if (hasInput && seenRows.putIfAbsent(key, "1") != null)
                        addError(errors, error(sheet.getSheetName(), ri + 1, null, "FILA_DUPLICADA", "La fila duplica exactamente una fila anterior del archivo."));
                    addRow(rows, preview, new ArchivoFacturacion.Fila(sheet.getSheetName(), ri + 1, List.copyOf(cells)));
                }
            }
            if (totalRows == 0) addError(errors, error(null, null, null, "SIN_REGISTROS", "El archivo no contiene filas de datos."));
            return new ArchivoFacturacion(safeFilename(filename), hash, sheets, totalRows, List.copyOf(columns),
                    List.copyOf(preview), List.copyOf(rows), List.copyOf(errors), false);
        } catch (IOException | RuntimeException e) {
            String message = e instanceof ArchivoInvalidoException ? e.getMessage()
                    : "No fue posible leer la estructura del libro Excel; el archivo quedó marcado como fallido.";
            return new ArchivoFacturacion(safeFilename(filename), hash, 0, 0, List.of(), List.of(), List.of(),
                    List.of(error(null, null, null, "ARCHIVO_NO_LEIBLE", message)), true);
        }
    }

    private String canonicalValue(PlantillaFacturacion.Columna column, Cell cell, String value) {
        if (value.isBlank()) return "";
        if (column.tipo().equalsIgnoreCase("FECHA")) return canonicalDate(cell, value);
        if (column.tipo().equalsIgnoreCase("DECIMAL")) return canonicalDecimal(cell, value);
        return value.trim();
    }

    private String canonicalDate(Cell cell, String value) {
        try {
            if (cell != null && cell.getCellType() == CellType.NUMERIC && DateUtil.isCellDateFormatted(cell))
                return DateUtil.getLocalDateTime(cell.getNumericCellValue()).toLocalDate().toString();
            for (DateTimeFormatter formatter : List.of(DateTimeFormatter.ISO_LOCAL_DATE,
                    DateTimeFormatter.ofPattern("d/M/uuuu"), DateTimeFormatter.ofPattern("d-M-uuuu"))) {
                try { return LocalDate.parse(value.trim(), formatter).toString(); }
                catch (DateTimeParseException ignored) { }
            }
        } catch (RuntimeException ignored) { }
        return "";
    }

    private String canonicalDecimal(Cell cell, String value) {
        try {
            String candidate = cell != null && cell.getCellType() == CellType.NUMERIC
                    ? NumberToTextConverter.toText(cell.getNumericCellValue()) : value.trim();
            if (!candidate.matches("[+]?(?:0|[1-9]\\d*)(?:\\.\\d+)?")) return "";
            BigDecimal decimal = new BigDecimal(candidate);
            return decimal.compareTo(BigDecimal.ZERO) > 0 ? decimal.toPlainString() : "";
        } catch (NumberFormatException ignored) { return ""; }
    }

    private void validarEncabezado(PlantillaFacturacion plantilla, List<String> actual, String hoja,
                                   List<ArchivoFacturacion.Error> errors) {
        Set<String> configured = plantilla.columnas().stream().map(column -> normalize(column.nombre())).collect(java.util.stream.Collectors.toSet());
        for (PlantillaFacturacion.Columna column : plantilla.columnas()) {
            if (!actual.contains(normalize(column.nombre())) && column.obligatoria())
                addError(errors, error(hoja, null, column.nombre(), "COLUMNA_OBLIGATORIA_AUSENTE", "Falta una columna obligatoria."));
        }
        for (String column : actual) if (!column.isBlank() && !configured.contains(column))
            addError(errors, error(hoja, null, column, "COLUMNA_NO_CONFIGURADA", "La columna no pertenece a la plantilla activa."));
    }

    private void addRow(List<ArchivoFacturacion.Fila> rows, List<ArchivoFacturacion.Fila> preview, ArchivoFacturacion.Fila row) {
        rows.add(row);
        if (preview.size() < MAX_PREVIEW) preview.add(row);
    }

    private String columnName(Row header, int column, DataFormatter formatter) {
        Cell cell = header.getCell(column);
        return cell == null ? null : safeText(formatter.formatCellValue(cell).trim(), 80);
    }

    private String rowKey(List<String> cells) { return String.join("\u0001", cells); }

    private void addError(List<ArchivoFacturacion.Error> errors, ArchivoFacturacion.Error error) {
        if (errors.size() < MAX_ERRORES - 1) errors.add(error);
        else if (errors.size() == MAX_ERRORES - 1) errors.add(error(null, null, null, "ERRORES_LIMITADOS", "Se alcanzó el máximo de diagnósticos."));
    }

    public static String normalize(String value) {
        return Normalizer.normalize(value == null ? "" : value, Normalizer.Form.NFD).replaceAll("\\p{M}+", "")
                .trim().replaceAll("\\s+", " ").toLowerCase(Locale.ROOT);
    }

    private ArchivoFacturacion.Error error(String sheet, Integer row, String column, String code, String message) {
        return new ArchivoFacturacion.Error(safeText(sheet, 80), row, safeText(column, 80), null, code, message);
    }

    private String safeValue(String value) { return value.length() > 500 ? value.substring(0, 500) : value; }
    private String safeText(String value, int max) { return value == null ? null : value.substring(0, Math.min(value.length(), max)); }
    private String safeFilename(String name) { return safeText(name == null ? "archivo" : name.replaceAll("[\\r\\n\\\\/]", "_"), 255); }

    public static class ArchivoInvalidoException extends RuntimeException {
        public ArchivoInvalidoException() { super("El libro Excel excede los límites de procesamiento permitidos."); }
    }
}
