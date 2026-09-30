package com.jovycandy.anexo24.operations.pediments.application.command;

import com.jovycandy.anexo24.operations.pediments.domain.model.CargaPedimentoArchivo;
import com.jovycandy.anexo24.operations.pediments.domain.model.PedimentoError;
import com.jovycandy.anexo24.operations.pediments.domain.model.PedimentoFila;
import org.apache.poi.openxml4j.util.ZipSecureFile;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellType;
import org.apache.poi.ss.usermodel.DataFormatter;
import org.apache.poi.ss.usermodel.DateUtil;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.ss.usermodel.WorkbookFactory;
import org.springframework.stereotype.Component;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

/** Parser de la superficie confirmada por metadata de CargaPedimentosIE. */
@Component
public class ExcelPedimentoParser {
    public static final String VERSION_CONTRATO = "LEGACY-STAGE-DERIVED-V1";
    public static final int MAX_FILAS = 2_000;
    public static final int MAX_COLUMNAS = 80;
    public static final int MAX_CELDAS = 100_000;
    private static final int MAX_ERRORES = 500;

    /** Campos físicos confirmados en dbo.CargaPedimentosIE; no implica que todos sean obligatorios. */
    public static final List<String> CAMPOS_CONFIRMADOS = List.of(
            "Aduana", "Patente", "NumeroPedimento", "ClavePedimento", "TipoOperacion", "TipoPedimento", "tc",
            "FechaPago", "ClaveCP", "NombreCP", "Sec", "Clave", "Descripcion", "Fraccion", "CantidadComercial",
            "UnidadComercial", "CantidadTarifa", "UnidadTarifa", "PaisOD", "PaisCV", "ValorDolares",
            "ValorComercial", "ValorAduanal", "ValorME", "Factura", "FechaFactura", "PedimentoOriginal",
            "DescargaDirigida", "TASAIGIE", "FPIGIE", "TASAIVA", "FPIVA", "CNT", "COVE", "FACTORINC", "FME",
            "ESACTIVO", "PESOBRUTO", "APARTADO", "Descarga", "INCOTERM", "FECHAENTRADA", "NICO", "lote",
            "complemento3", "complemento2", "DIVISION", "complemento1", "CATEGORIA", "marca", "modelo", "serie",
            "IVA_PRE", "MULTAS", "RECARGOS", "IVA_PRV");

    private static final List<String> OBLIGATORIOS = List.of(
            "Aduana", "Patente", "NumeroPedimento", "ClavePedimento", "TipoOperacion", "FechaPago", "Sec",
            "Clave", "Descripcion", "Fraccion", "CantidadComercial", "UnidadComercial");
    private static final Set<String> DECIMALES = Set.of(
            "tc", "CantidadComercial", "CantidadTarifa", "ValorDolares",
            "ValorComercial", "ValorAduanal", "ValorME", "TASAIGIE", "TASAIVA", "CNT", "FACTORINC", "FME",
            "PESOBRUTO", "IVA_PRE", "MULTAS", "RECARGOS", "IVA_PRV");
    private static final Set<String> ENTEROS = Set.of("TipoOperacion", "TipoPedimento", "Sec", "FPIGIE", "FPIVA");
    private static final Set<String> FECHAS = Set.of("FechaPago", "FechaFactura", "FECHAENTRADA");

    static {
        ZipSecureFile.setMinInflateRatio(0.01d);
        ZipSecureFile.setMaxEntrySize(10L * 1024 * 1024);
        ZipSecureFile.setMaxTextSize(10L * 1024 * 1024);
        ZipSecureFile.setMaxFileCount(200);
    }

    /**
     * Parsea una hoja de Excel sin ejecutar procedimientos legacy.
     *
     * @param filename nombre recibido, saneado al persistir
     * @param hash SHA-256 calculado antes de invocar el parser
     * @param bytes contenido del archivo
     * @return resultado normalizado y validado
     */
    public CargaPedimentoArchivo parsear(String filename, String hash, byte[] bytes) {
        List<PedimentoError> errores = new ArrayList<>();
        List<PedimentoFila> filas = new ArrayList<>();
        List<String> columnas = new ArrayList<>(CAMPOS_CONFIRMADOS);
        try (Workbook workbook = WorkbookFactory.create(new ByteArrayInputStream(bytes))) {
            if (workbook.getNumberOfSheets() < 1) {
                agregarError(errores, error(null, null, null, "SIN_HOJA", "El libro no contiene una hoja."));
                return resultado(filename, hash, columnas, filas, errores, true);
            }
            if (workbook.getNumberOfSheets() > 1)
                agregarError(errores, error(null, null, null, "HOJAS_NO_SOPORTADAS", "El contrato V1 admite una sola hoja."));

            Sheet sheet = workbook.getSheetAt(0);
            if (sheet.getLastRowNum() > MAX_FILAS) throw new ArchivoInvalidoException();
            Row header = sheet.getRow(sheet.getFirstRowNum());
            if (header == null || header.getLastCellNum() <= 0) {
                agregarError(errores, error(sheet.getSheetName(), null, null, "ENCABEZADO_AUSENTE", "La hoja no contiene encabezado."));
                return resultado(filename, hash, columnas, filas, errores, false);
            }
            if (header.getLastCellNum() > MAX_COLUMNAS) throw new ArchivoInvalidoException();

            DataFormatter formatter = new DataFormatter(Locale.ROOT);
            Map<String, Integer> indices = new LinkedHashMap<>();
            Set<String> duplicados = new HashSet<>();
            for (int column = 0; column < header.getLastCellNum(); column++) {
                Cell cell = header.getCell(column);
                String label = cell == null ? "" : formatter.formatCellValue(cell).trim();
                String normalized = normalize(label);
                if (!normalized.isBlank() && indices.putIfAbsent(normalized, column) != null) {
                    duplicados.add(normalized);
                    agregarError(errores, error(sheet.getSheetName(), null, label, "COLUMNA_DUPLICADA",
                            "El encabezado se repite después de normalizarlo."));
                }
                if (!normalized.isBlank() && CAMPOS_CONFIRMADOS.stream().noneMatch(field -> normalize(field).equals(normalized)))
                    agregarError(errores, error(sheet.getSheetName(), null, label, "COLUMNA_NO_CONFIRMADA",
                            "La columna no forma parte del contrato derivado del staging legacy."));
                if (cell != null && cell.getCellType() == CellType.FORMULA)
                    agregarError(errores, error(sheet.getSheetName(), null, label, "FORMULA", "No se admiten fórmulas."));
            }
            for (String required : OBLIGATORIOS) {
                if (!indices.containsKey(normalize(required)))
                    agregarError(errores, error(sheet.getSheetName(), null, required, "COLUMNA_OBLIGATORIA_AUSENTE",
                            "Falta una columna requerida por el contrato V1."));
            }

            int totalCells = Math.max(0, header.getLastCellNum());
            Set<String> seenRows = new HashSet<>();
            for (int rowNumber = header.getRowNum() + 1; rowNumber <= sheet.getLastRowNum(); rowNumber++) {
                Row row = sheet.getRow(rowNumber);
                totalCells += row == null ? 0 : Math.max(0, row.getLastCellNum());
                if (totalCells > MAX_CELDAS) throw new ArchivoInvalidoException();
                Map<String, String> values = new LinkedHashMap<>();
                boolean blank = true;
                boolean rowHasError = false;
                for (String field : CAMPOS_CONFIRMADOS) {
                    Integer index = indices.get(normalize(field));
                    Cell cell = index == null || row == null ? null : row.getCell(index);
                    String value = cell == null ? "" : formatter.formatCellValue(cell).trim();
                    String canonical = canonical(field, cell, value);
                    values.put(field, canonical);
                    blank &= canonical.isBlank();
                    if (cell != null && cell.getCellType() == CellType.FORMULA) {
                        rowHasError = true;
                        agregarError(errores, error(sheet.getSheetName(), rowNumber + 1, field, "FORMULA", "No se admiten fórmulas."));
                    }
                    if (indices.containsKey(normalize(field)) && !duplicados.contains(normalize(field))) {
                        if (OBLIGATORIOS.contains(field) && canonical.isBlank()) {
                            rowHasError = true;
                            agregarError(errores, error(sheet.getSheetName(), rowNumber + 1, field, "VALOR_OBLIGATORIO_AUSENTE", "El campo obligatorio está vacío."));
                        }
                        if (!value.isBlank() && (DECIMALES.contains(field) || ENTEROS.contains(field) || FECHAS.contains(field)) && canonical.isBlank()) {
                            rowHasError = true;
                            agregarError(errores, error(sheet.getSheetName(), rowNumber + 1, field, "TIPO_INVALIDO", "El valor no coincide con el tipo del contrato."));
                        }
                    }
                }
                if (blank) {
                    rowHasError = true;
                    agregarError(errores, error(sheet.getSheetName(), rowNumber + 1, null, "FILA_VACIA", "La fila no contiene datos."));
                }
                validarTipos(values, sheet.getSheetName(), rowNumber + 1, errores);
                if (!blank && !seenRows.add(rowKey(values))) {
                    rowHasError = true;
                    agregarError(errores, error(sheet.getSheetName(), rowNumber + 1, null, "FILA_DUPLICADA", "La fila duplica otra fila del archivo."));
                }
                if (row != null) {
                    for (int column = 0; column < row.getLastCellNum(); column++) {
                        Cell cell = row.getCell(column);
                        if (cell != null && cell.getCellType() == CellType.FORMULA)
                            agregarError(errores, error(sheet.getSheetName(), rowNumber + 1, headerName(header, column, formatter), "FORMULA", "No se admiten fórmulas."));
                    }
                }
                filas.add(new PedimentoFila(safeText(sheet.getSheetName(), 80), rowNumber + 1, Map.copyOf(values)));
            }
            if (filas.isEmpty()) agregarError(errores, error(sheet.getSheetName(), null, null, "SIN_REGISTROS", "El archivo no contiene filas de datos."));
            return resultado(filename, hash, columnas, filas, errores, false);
        } catch (IOException | RuntimeException exception) {
            String code = exception instanceof ArchivoInvalidoException ? "LIMITES_ARCHIVO" : "ARCHIVO_NO_LEIBLE";
            return resultado(filename, hash, columnas, filas,
                    List.of(error(null, null, null, code, exception instanceof ArchivoInvalidoException
                            ? "El archivo excede los límites de procesamiento permitidos."
                            : "No fue posible leer el libro de Excel.")), true);
        }
    }

    private void validarTipos(Map<String, String> values, String sheet, int row, List<PedimentoError> errors) {
        for (String field : DECIMALES) {
            String value = values.getOrDefault(field, "");
            if (!value.isBlank() && canonicalDecimal(value, field.equals("CantidadComercial") || field.equals("CantidadTarifa")) == null)
                agregarError(errors, error(sheet, row, field, "DECIMAL_INVALIDO", "El valor decimal no tiene formato válido."));
        }
        for (String field : ENTEROS) {
            String value = values.getOrDefault(field, "");
            if (!value.isBlank()) {
                try {
                    Integer.parseInt(value);
                } catch (NumberFormatException exception) {
                    agregarError(errors, error(sheet, row, field, "ENTERO_INVALIDO", "El valor entero no tiene formato válido."));
                }
            }
        }
        for (String field : FECHAS) {
            String value = values.getOrDefault(field, "");
            if (!value.isBlank() && parseDate(value) == null)
                agregarError(errors, error(sheet, row, field, "FECHA_INVALIDA", "La fecha no tiene un formato válido."));
        }
        String operation = values.getOrDefault("TipoOperacion", "");
        if (!operation.isBlank() && !operation.equals("1") && !operation.equals("2"))
            agregarError(errors, error(sheet, row, "TipoOperacion", "OPERACION_NO_CONFIRMADA", "El contrato auditado sólo documenta operaciones 1 y 2."));
    }

    private String canonical(String field, Cell cell, String value) {
        if (value.isBlank()) return "";
        if (DECIMALES.contains(field)) {
            BigDecimal decimal = canonicalDecimal(value, field.equals("CantidadComercial") || field.equals("CantidadTarifa"));
            return decimal == null ? "" : decimal.toPlainString();
        }
        if (FECHAS.contains(field)) {
            LocalDate date = cell != null && cell.getCellType() == CellType.NUMERIC && DateUtil.isCellDateFormatted(cell)
                    ? DateUtil.getLocalDateTime(cell.getNumericCellValue()).toLocalDate() : parseDate(value);
            return date == null ? "" : date.toString();
        }
        if (ENTEROS.contains(field)) {
            try { return Integer.toString(Integer.parseInt(value)); }
            catch (NumberFormatException exception) { return ""; }
        }
        return safeText(value, field.equals("Descripcion") ? 250 : 150);
    }

    private BigDecimal canonicalDecimal(String value, boolean positive) {
        try {
            if (!value.matches("[+]?(?:0|[1-9]\\d*)(?:\\.\\d+)?")) return null;
            BigDecimal decimal = new BigDecimal(value);
            return positive && decimal.signum() <= 0 ? null : decimal;
        } catch (NumberFormatException exception) {
            return null;
        }
    }

    private LocalDate parseDate(String value) {
        for (DateTimeFormatter formatter : List.of(DateTimeFormatter.ISO_LOCAL_DATE,
                DateTimeFormatter.ofPattern("d/M/uuuu"), DateTimeFormatter.ofPattern("d-M-uuuu"))) {
            try { return LocalDate.parse(value.trim(), formatter); }
            catch (DateTimeParseException ignored) { }
        }
        return null;
    }

    private String rowKey(Map<String, String> values) {
        return OBLIGATORIOS.stream().map(values::get).collect(Collectors.joining("\u0001"));
    }

    private CargaPedimentoArchivo resultado(String filename, String hash, List<String> columns,
                                            List<PedimentoFila> rows, List<PedimentoError> errors, boolean failed) {
        return new CargaPedimentoArchivo(safeFilename(filename), hash, VERSION_CONTRATO,
                List.copyOf(columns), List.copyOf(rows), List.copyOf(errors), failed);
    }

    private void agregarError(List<PedimentoError> errors, PedimentoError error) {
        if (errors.size() < MAX_ERRORES) errors.add(error);
    }

    private PedimentoError error(String sheet, Integer row, String column, String code, String message) {
        return new PedimentoError(safeText(sheet, 80), row, safeText(column, 80), "no almacenado", code, message);
    }

    private String headerName(Row header, int column, DataFormatter formatter) {
        Cell cell = header.getCell(column);
        return cell == null ? null : safeText(formatter.formatCellValue(cell).trim(), 80);
    }

    public static String normalize(String value) {
        return java.text.Normalizer.normalize(value == null ? "" : value, java.text.Normalizer.Form.NFD)
                .replaceAll("\\p{M}+", "").replaceAll("[^a-zA-Z0-9]", "").toLowerCase(Locale.ROOT);
    }

    private String safeFilename(String filename) {
        return safeText((filename == null ? "archivo" : filename).replaceAll("[\\r\\n\\\\/]", "_"), 255);
    }

    private String safeText(String value, int max) {
        return value == null ? null : value.substring(0, Math.min(value.length(), max));
    }

    public static class ArchivoInvalidoException extends RuntimeException {
    }
}
