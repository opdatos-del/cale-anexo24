package com.jovycandy.anexo24.billing.application.command;


import com.jovycandy.anexo24.billing.domain.model.ArchivoFacturacion;
import org.apache.poi.openxml4j.util.ZipSecureFile;
import org.apache.poi.ss.usermodel.*;
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

    public ArchivoFacturacion parsear(String filename, String hash, byte[] bytes) {
        List<ArchivoFacturacion.Error> errors = new ArrayList<>();
        List<ArchivoFacturacion.Fila> preview = new ArrayList<>();
        List<String> columns = new ArrayList<>();
        int rows = 0;
        int totalCells = 0;
        int sheets = 0;
        List<String> firstSheetHeader = null;
        try (Workbook workbook = WorkbookFactory.create(new ByteArrayInputStream(bytes))) {
            sheets = workbook.getNumberOfSheets();
            if (sheets < 1 || sheets > MAX_HOJAS) throw new ArchivoInvalidoException();
            DataFormatter formatter = new DataFormatter(Locale.ROOT);
            Map<String, String> seenRows = new HashMap<>();
            for (int si = 0; si < sheets; si++) {
                Sheet sheet = workbook.getSheetAt(si);
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
                Map<String, Integer> seen = new HashMap<>();
                Map<Integer, String> aliases = new HashMap<>();
                List<String> normalizedHeader = new ArrayList<>();
                for (int ci = 0; ci < header.getLastCellNum(); ci++) {
                    Cell cell = header.getCell(ci);
                    String text = cell == null ? "" : formatter.formatCellValue(cell).trim();
                    if (si == 0 && columns.size() < MAX_COLUMNAS) columns.add(text);
                    String normalized = normalize(text);
                    normalizedHeader.add(normalized);
                    if (!normalized.isBlank() && seen.putIfAbsent(normalized, ci) != null)
                        addError(errors, error(sheet.getSheetName(), null, text, "COLUMNA_DUPLICADA", "Encabezado duplicado tras normalizar espacios, acentos y mayúsculas."));
                    if (normalized.equals("fecha") || normalized.equals("cantidad")) aliases.put(ci, normalized);
                    if (cell != null && cell.getCellType() == CellType.FORMULA)
                        addError(errors, error(sheet.getSheetName(), null, text, "FORMULA", "No se admiten fórmulas."));
                }
                if (normalizedHeader.stream().noneMatch(value -> !value.isBlank()))
                    addError(errors, error(sheet.getSheetName(), null, null, "ENCABEZADO_VACIO", "La hoja requiere al menos un encabezado."));
                if (firstSheetHeader == null) firstSheetHeader = List.copyOf(normalizedHeader);
                else if (!firstSheetHeader.equals(normalizedHeader))
                    addError(errors, error(sheet.getSheetName(), null, null, "COLUMNAS_INCOMPATIBLES", "Las hojas del archivo deben conservar la misma estructura para generar vista previa."));
                for (int ri = header.getRowNum() + 1; ri <= sheet.getLastRowNum(); ri++) {
                    Row row = sheet.getRow(ri);
                    if (row == null || row.getFirstCellNum() < 0) {
                        rows++;
                        addError(errors, error(sheet.getSheetName(), ri + 1, null, "FILA_VACIA", "La fila no contiene datos."));
                        if (preview.size() < MAX_PREVIEW)
                            preview.add(new ArchivoFacturacion.Fila(sheet.getSheetName(), ri + 1,
                                    Collections.nCopies(columns.size(), "")));
                        continue;
                    }
                    rows++;
                    if (row.getLastCellNum() > MAX_COLUMNAS) throw new ArchivoInvalidoException();
                    totalCells += Math.max(0, row.getLastCellNum());
                    if (totalCells > MAX_CELDAS) throw new ArchivoInvalidoException();
                    List<String> cells = new ArrayList<>();
                    boolean blank = true;
                    for (int ci = 0; ci < Math.max(0, row.getLastCellNum()); ci++) {
                        Cell cell = row.getCell(ci);
                        String value = cell == null ? "" : formatter.formatCellValue(cell);
                        blank &= value.isBlank();
                        cells.add(safeValue(value));
                        if (cell != null && cell.getCellType() == CellType.FORMULA)
                            addError(errors, error(sheet.getSheetName(), ri + 1, columnName(header, ci, formatter), "FORMULA", "No se admiten fórmulas."));
                        String alias = aliases.get(ci);
                        if (alias != null) {
                            if (alias.equals("fecha") && (cell == null || value.isBlank() || !validDate(cell, value)))
                                addError(errors, error(sheet.getSheetName(), ri + 1, "Fecha", "FECHA_INVALIDA", "La fecha no tiene un formato válido."));
                            if (alias.equals("cantidad") && (cell == null || !positiveQuantity(value)))
                                addError(errors, error(sheet.getSheetName(), ri + 1, "Cantidad", "CANTIDAD_INVALIDA", "La cantidad debe ser un número mayor que cero."));
                        }
                    }
                    if (blank) addError(errors, error(sheet.getSheetName(), ri + 1, null, "FILA_VACIA", "La fila no contiene datos."));
                    String key = rowKey(cells);
                    if (!blank && seenRows.putIfAbsent(key, "1") != null)
                        addError(errors, error(sheet.getSheetName(), ri + 1, null, "FILA_DUPLICADA", "La fila duplica exactamente una fila anterior del archivo."));
                    if (preview.size() < MAX_PREVIEW) preview.add(new ArchivoFacturacion.Fila(sheet.getSheetName(), ri + 1, List.copyOf(cells)));
                }

            }
            if (rows == 0) addError(errors, error(null, null, null, "SIN_REGISTROS", "El archivo no contiene filas de datos."));
            return new ArchivoFacturacion(safeFilename(filename), hash, sheets, rows, List.copyOf(columns),
                    List.copyOf(preview), List.copyOf(errors), false);
        } catch (IOException | RuntimeException e) {
            String message = e instanceof ArchivoInvalidoException ? e.getMessage()
                    : "No fue posible leer la estructura del libro Excel; el archivo quedó marcado como fallido.";
            List<ArchivoFacturacion.Error> failed = List.of(error(null, null, null, "ARCHIVO_NO_LEIBLE", message));
            return new ArchivoFacturacion(safeFilename(filename), hash, 0, 0, List.of(), List.of(), failed, true);
        }
    }

    private boolean validDate(Cell cell, String value) {
        if (cell.getCellType() == CellType.NUMERIC) return DateUtil.isCellDateFormatted(cell);
        for (DateTimeFormatter formatter : List.of(DateTimeFormatter.ISO_LOCAL_DATE,
                DateTimeFormatter.ofPattern("d/M/uuuu"), DateTimeFormatter.ofPattern("d-M-uuuu"))) {
            try { LocalDate.parse(value.trim(), formatter); return true; }
            catch (DateTimeParseException ignored) { }
        }
        return false;
    }

    private boolean positiveQuantity(String value) {
        String normalized = value.trim();
        if (normalized.contains(",") && !normalized.contains(".")) normalized = normalized.replace(',', '.');
        else normalized = normalized.replace(",", "");
        try { return new BigDecimal(normalized).compareTo(BigDecimal.ZERO) > 0; }
        catch (NumberFormatException e) { return false; }
    }

    private String columnName(Row header, int column, DataFormatter formatter) {
        Cell cell = header.getCell(column);
        return cell == null ? null : safeText(formatter.formatCellValue(cell).trim(), 80);
    }
    private String rowKey(List<String> cells) { return String.join("\u0001", cells); }
    private void addError(List<ArchivoFacturacion.Error> errors, ArchivoFacturacion.Error error) {
        if (errors.size() < MAX_ERRORES - 1) {
            errors.add(error);
        } else if (errors.size() == MAX_ERRORES - 1) {
            errors.add(this.error(null, null, null, "ERRORES_LIMITADOS",
                    "Se alcanzó el máximo de diagnósticos; el archivo no se considera validado."));
        }
    }
    public static String normalize(String value) {
        return Normalizer.normalize(value == null ? "" : value, Normalizer.Form.NFD)
                .replaceAll("\\p{M}+", "").trim().replaceAll("\\s+", " ").toLowerCase(Locale.ROOT);
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
