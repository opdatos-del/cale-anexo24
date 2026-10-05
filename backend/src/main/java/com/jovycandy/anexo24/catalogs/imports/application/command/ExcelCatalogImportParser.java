package com.jovycandy.anexo24.catalogs.imports.application.command;

import com.jovycandy.anexo24.catalogs.imports.domain.model.CatalogImportArchivo;
import com.jovycandy.anexo24.catalogs.imports.domain.model.CatalogImportError;
import com.jovycandy.anexo24.catalogs.imports.domain.model.CatalogImportFila;
import com.jovycandy.anexo24.catalogs.imports.domain.model.CatalogImportType;
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
import java.text.Normalizer;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
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
    // Mismas columnas que el stage TMPPROVEEDORES auditado en CALE_IMMEX LIVE 2026-10-03.
    private static final List<String> PROVIDER_COLUMNS = List.of(
            "Clave", "Nombre", "IdFiscal", "TipoNE", "Programa", "CalleNumero", "Codigo", "Colonia",
            "Entidad", "Pais", "Telefono", "Correo", "Fax",
            "ApellidoPaterno", "ApellidoMaterno", "Calle", "CalleNumeroInterior",
            "Localidad", "Referencia", "Municipio", "TipoIdentificador", "CodigoPostal");
    // Mismas columnas que el stage TMPagentes auditado en CALE_IMMEX LIVE 2026-10-05.
    private static final List<String> AGENT_COLUMNS = List.of(
            "Clave", "Nombre", "Domicilio", "Rfc", "Patente");
    // Mismas columnas que el stage dbo.TMPSUBMAQUILA auditado en CALE_IMMEX LIVE 2026-10-05.
    // El SP legacy dbo.CARGA_SUBMAQUILA agrupa por FOLIO/FECHA/SUBMAQUILADOR y lee
    // CLAVE, CANTIDAD, UNIDAD y DESCRIPCION renglón por renglón.
    private static final List<String> SUBMAQUILA_COLUMNS = List.of(
            "Folio", "Fecha", "Submaquilador", "Clave", "Cantidad", "Unidad", "Descripcion", "Linea");
    private static final int CLIENT_KEY_MAX_LENGTH = 15;
    private static final int PROVIDER_KEY_MAX_LENGTH = 15;
    // Restricciones físicas auditadas de dbo.TMPagentes y dbo.agentes (ambos CHAR):
    //   Clave CHAR(10) NOT NULL, Nombre CHAR(40), Domicilio CHAR(60),
    //   Rfc CHAR(20), Patente CHAR(10). Sin ensanchamiento stage -> destino.
    private static final int AGENT_KEY_MAX_LENGTH = 10;
    private static final int AGENT_NOMBRE_MAX_LENGTH = 40;
    private static final int AGENT_DOMICILIO_MAX_LENGTH = 60;
    private static final int AGENT_RFC_MAX_LENGTH = 20;
    private static final int AGENT_PATENTE_LENGTH = 4;
    private static final String MENSAJE_FRACCION_INVALIDA =
            "La fracción debe tener exactamente 8 caracteres en el contrato legacy.";
    private static final String MENSAJE_PATENTE_INVALIDA =
            "La patente debe tener exactamente " + AGENT_PATENTE_LENGTH + " caracteres en el contrato legacy.";
    // Restricciones físicas auditadas de dbo.TMPSUBMAQUILA (compat level 100):
    //   FOLIO VARCHAR(50), FECHA DATE, SUBMAQUILADOR VARCHAR(50), CLAVE VARCHAR(50),
    //   CANTIDAD NUMERIC(18,4), UNIDAD VARCHAR(5), DESCRIPCION VARCHAR(250),
    //   LINEA INT.
    private static final int SUBMAQUILA_FOLIO_MAX_LENGTH = 50;
    private static final int SUBMAQUILA_SUBMAQUILADOR_MAX_LENGTH = 50;
    private static final int SUBMAQUILA_CLAVE_MAX_LENGTH = 50;
    private static final int SUBMAQUILA_UNIDAD_MAX_LENGTH = 5;
    private static final int SUBMAQUILA_DESCRIPCION_MAX_LENGTH = 250;
    // dbo.TMPSUBMAQUILA.CANTIDAD es NUMERIC(18,4): 14 dígitos enteros + 4 decimales.
    // No hay evidencia de layout ni de validación en dbo.CARGA_SUBMAQUILA que imponga
    // positividad, por lo que sólo se valida la representabilidad física del tipo.
    private static final int SUBMAQUILA_CANTIDAD_ENTEROS_MAX = 14;
    private static final int SUBMAQUILA_CANTIDAD_DECIMALES_MAX = 4;
    /**
     * El mapa canónico trunca a 150 caracteres por defensa. DESCRIPCION admite 250 en el
     * destino, así que su tope canónico queda por encima del límite físico: de lo contrario
     * una descripción de 251 caracteres se truncaría en silencio en lugar de reportarse
     * como error y el dato llegaría incompleto a dbo.TMPSUBMAQUILA.
     */
    private static final int LIMITE_CANONICO_DESCRIPCION = SUBMAQUILA_DESCRIPCION_MAX_LENGTH + 150;
    private static final String MENSAJE_CANTIDAD_INVALIDA =
            "La cantidad debe ser un decimal representable como NUMERIC(18,4) "
                    + "(máximo 14 dígitos enteros y 4 decimales).";
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
                    // El contrato legacy alimenta FECHA DATE: si el archivo trae una celda
                    // de fecha nativa de Excel, se normaliza a ISO-8601 para que el stage la
                    // reciba sin depender del formato de despliegue ni del locale.
                    if (type == CatalogImportType.SUBMAQUILA && "Fecha".equals(column) && esCeldaFecha(cell))
                        canonical = cell.getLocalDateTimeCellValue().toLocalDate()
                                .format(DateTimeFormatter.ISO_LOCAL_DATE);
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
                String claveDuplicada = claveDuplicada(type, values);
                if (!claveDuplicada.isBlank() && !seenKeys.add(normalize(claveDuplicada)))
                    add(errors, error(sheet.getSheetName(), rowNumber + 1, columnaClave(type),
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
                validateExactLength(values, "Fraccion", 8, sheet, row, errors, "FRACCION_INVALIDA", MENSAJE_FRACCION_INVALIDA);
            }
            case PRODUCTO -> {
                validateLength(values, "CVE_PRODUCTO", 3, sheet, row, errors, "CLAVE_PRODUCTO_CORTA");
                validateLength(values, "NOMBRE", 3, sheet, row, errors, "NOMBRE_PRODUCTO_CORTO");
                validateExactLength(values, "fraccion", 8, sheet, row, errors, "FRACCION_INVALIDA", MENSAJE_FRACCION_INVALIDA);
                validateLengthIfPresent(values, "CVE_PRODUCTO_CLIENTE", 3, sheet, row, errors, "CLAVE_CLIENTE_CORTA");
            }
            case CLIENTE -> {
                // Restricción física del destino: clientes.CLAVE = CHAR(15). Aplicar LEFT(Clave,15) en el
                // SP legacy hace que claves >15 choquen. Validación preventiva para que nunca lleguen a confirmación.
                validateMaxLength(values, "Clave", CLIENT_KEY_MAX_LENGTH, sheet, row, errors, "CLAVE_CLIENTE_LARGA");
                validateLength(values, "Clave", 3, sheet, row, errors, "CLAVE_CLIENTE_CORTA");
                validateLengthIfPresent(values, "IdFiscal", 1, sheet, row, errors, "ID_FISCAL_VACIO");
            }
            case PROVEEDOR -> {
                // Proveedores.CLAVE = CHAR(15). LEFT(Clave,15) en el SP legacy hace que claves >15 choquen.
                validateMaxLength(values, "Clave", PROVIDER_KEY_MAX_LENGTH, sheet, row, errors, "CLAVE_PROVEEDOR_LARGA");
                validateLength(values, "Clave", 3, sheet, row, errors, "CLAVE_PROVEEDOR_CORTA");
                validateLengthIfPresent(values, "IdFiscal", 1, sheet, row, errors, "ID_FISCAL_VACIO");
            }
            case AGENTE -> {
                // Agentes: TMPagentes.Clave/agentes.Clave = CHAR(10). LEFT(Clave,10) evita el error de
                // truncado del INSERT legacy; la validación reproduce el mismo límite en el parser.
                validateMaxLength(values, "Clave", AGENT_KEY_MAX_LENGTH, sheet, row, errors, "CLAVE_AGENTE_LARGA");
                validateLengthIfPresent(values, "Clave", 1, sheet, row, errors, "CLAVE_AGENTE_VACIA");
                validateMaxLength(values, "Nombre", AGENT_NOMBRE_MAX_LENGTH, sheet, row, errors, "NOMBRE_AGENTE_LARGO");
                validateMaxLength(values, "Domicilio", AGENT_DOMICILIO_MAX_LENGTH, sheet, row, errors, "DOMICILIO_AGENTE_LARGO");
                validateMaxLength(values, "Rfc", AGENT_RFC_MAX_LENGTH, sheet, row, errors, "RFC_AGENTE_LARGO");
                // El legacy exige LEN(Patente) = 4; se adelanta para no generar CON_ERRORES.
                validateExactLength(values, "Patente", AGENT_PATENTE_LENGTH, sheet, row, errors, "PATENTE_AGENTE_INVALIDA", MENSAJE_PATENTE_INVALIDA);
            }
            case SUBMAQUILA -> {
                // dbo.CARGA_SUBMAQUILA no valida nada: todas estas verificaciones existen para que
                // una carga que llega a confirmación sea ejecutable sin truncar ni fallar en destino.
                validateLength(values, "Folio", 1, sheet, row, errors, "FOLIO_SUBMAQUILA_VACIO");
                validateMaxLength(values, "Folio", SUBMAQUILA_FOLIO_MAX_LENGTH, sheet, row, errors, "FOLIO_SUBMAQUILA_LARGO");
                validateLength(values, "Submaquilador", 1, sheet, row, errors, "SUBMAQUILADOR_VACIO");
                validateMaxLength(values, "Submaquilador", SUBMAQUILA_SUBMAQUILADOR_MAX_LENGTH, sheet, row, errors, "SUBMAQUILADOR_LARGO");
                validateLength(values, "Clave", 1, sheet, row, errors, "CLAVE_SUBMAQUILA_VACIA");
                validateMaxLength(values, "Clave", SUBMAQUILA_CLAVE_MAX_LENGTH, sheet, row, errors, "CLAVE_SUBMAQUILA_LARGA");
                validateCantidadRepresentable(values, "Cantidad", sheet, row, errors);
                validateMaxLength(values, "Unidad", SUBMAQUILA_UNIDAD_MAX_LENGTH, sheet, row, errors, "UNIDAD_SUBMAQUILA_LARGA");
                validateMaxLength(values, "Descripcion", SUBMAQUILA_DESCRIPCION_MAX_LENGTH, sheet, row, errors, "DESCRIPCION_SUBMAQUILA_LARGA");
                validateEnteroInt(values, "Linea", sheet, row, errors);
                validateFechaIso(values, "Fecha", sheet, row, errors);
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

    /**
     * La cantidad alimenta un NUMERIC(18,4): sólo se exige que sea representable en ese
     * tipo (máximo 14 dígitos enteros y 4 decimales). No se impone positividad porque no
     * existe evidencia de layout ni validación legacy que la respalde.
     */
    private void validateCantidadRepresentable(Map<String, String> values, String key, String sheet,
                                               int row, List<CatalogImportError> errors) {
        String value = values.getOrDefault(key, "");
        if (value.isBlank()) {
            add(errors, error(sheet, row, key, "CANTIDAD_SUBMAQUILA_VACIA", MENSAJE_CANTIDAD_INVALIDA));
            return;
        }
        if (!value.matches("[+-]?\\d+(?:\\.\\d+)?")) {
            add(errors, error(sheet, row, key, "CANTIDAD_SUBMAQUILA_INVALIDA", MENSAJE_CANTIDAD_INVALIDA));
            return;
        }
        try {
            BigDecimal cantidad = new BigDecimal(value);
            int decimales = Math.max(0, cantidad.scale());
            int enteros = cantidad.precision() - decimales;
            if (decimales > SUBMAQUILA_CANTIDAD_DECIMALES_MAX || enteros > SUBMAQUILA_CANTIDAD_ENTEROS_MAX)
                add(errors, error(sheet, row, key, "CANTIDAD_SUBMAQUILA_INVALIDA", MENSAJE_CANTIDAD_INVALIDA));
        } catch (NumberFormatException noRepresentable) {
            add(errors, error(sheet, row, key, "CANTIDAD_SUBMAQUILA_INVALIDA", MENSAJE_CANTIDAD_INVALIDA));
        }
    }

    /**
     * LINEA alimenta una columna INT: se exige un entero representable en {@code INT} de
     * SQL Server. No se impone positividad porque no existe evidencia de layout que la
     * respalde; el legacy no la valida.
     */
    private void validateEnteroInt(Map<String, String> values, String key, String sheet,
                                   int row, List<CatalogImportError> errors) {
        String value = values.getOrDefault(key, "");
        String mensaje = "La línea debe ser un entero representable como INT de SQL Server.";
        if (value.isBlank() || !value.matches("[+-]?\\d+")) {
            add(errors, error(sheet, row, key, "LINEA_SUBMAQUILA_INVALIDA", mensaje));
            return;
        }
        try {
            long numero = Long.parseLong(value);
            if (numero < Integer.MIN_VALUE || numero > Integer.MAX_VALUE)
                add(errors, error(sheet, row, key, "LINEA_SUBMAQUILA_INVALIDA", mensaje));
        } catch (NumberFormatException fueraDeRango) {
            add(errors, error(sheet, row, key, "LINEA_SUBMAQUILA_INVALIDA", mensaje));
        }
    }

    /** La fecha alimenta una columna DATE: se exige ISO-8601 (yyyy-MM-dd) para no depender de locale. */
    private void validateFechaIso(Map<String, String> values, String key, String sheet,
                                  int row, List<CatalogImportError> errors) {
        String value = values.getOrDefault(key, "");
        if (value.isBlank()) {
            add(errors, error(sheet, row, key, "FECHA_SUBMAQUILA_VACIA", "La fecha es obligatoria y debe venir en formato ISO-8601 (yyyy-MM-dd)."));
            return;
        }
        try {
            LocalDate.parse(value, DateTimeFormatter.ISO_LOCAL_DATE);
        } catch (DateTimeParseException error) {
            add(errors, error(sheet, row, key, "FECHA_SUBMAQUILA_INVALIDA", "La fecha debe venir en formato ISO-8601 (yyyy-MM-dd)."));
        }
    }

    private void validateExactLength(Map<String, String> values, String key, int length, String sheet,                                     int row, List<CatalogImportError> errors, String code, String message) {
        if (values.getOrDefault(key, "").length() != length)
            add(errors, error(sheet, row, key, code, message));
    }

    private void validateMaxLength(Map<String, String> values, String key, int max, String sheet, int row,
                                   List<CatalogImportError> errors, String code) {
        if (values.getOrDefault(key, "").length() > max)
            add(errors, error(sheet, row, key, code, "El valor supera la longitud máxima física del destino (" + max + ")."));
    }

    private static boolean esCeldaFecha(Cell cell) {
        return cell != null && cell.getCellType() == CellType.NUMERIC && DateUtil.isCellDateFormatted(cell);
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
        return safe(trimmed, switch (column) {
            case "DescripcionComercial", "NOMBRE" -> 250;
            case "Descripcion" -> LIMITE_CANONICO_DESCRIPCION;
            default -> 150;
        });
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
                case PROVEEDOR -> PROVIDER_COLUMNS;
            case AGENTE -> AGENT_COLUMNS;
            case SUBMAQUILA -> SUBMAQUILA_COLUMNS;
            };
        }

    private List<String> required(CatalogImportType type) {
        return switch (type) {
            case MATERIAL -> List.of("ClaveMaterial", "UnidadComercial", "Fraccion");
            case PRODUCTO -> List.of("CVE_PRODUCTO", "NOMBRE", "UNIDAD", "fraccion");
            case CLIENTE -> List.of("Clave", "Nombre", "IdFiscal", "TipoNE");
            case PROVEEDOR -> List.of("Clave", "Nombre", "IdFiscal", "TipoNE");
            case AGENTE -> List.of("Clave", "Nombre", "Patente");
            case SUBMAQUILA -> List.of("Folio", "Fecha", "Submaquilador", "Clave", "Cantidad", "Unidad", "Linea");
        };
    }

    private String keyColumn(CatalogImportType type) {
        return switch (type) {
            case MATERIAL -> "ClaveMaterial";
            case PRODUCTO -> "CVE_PRODUCTO";
            case CLIENTE -> "Clave";
            case PROVEEDOR -> "Clave";
            case AGENTE -> "Clave";
            // Sin clave natural en el contrato legacy: la unicidad de un renglón de submaquila la
            //define el par FOLIO + LINEA, que es lo que dbo.CARGA_SUBMAQUILA conserva como PARTIDA.
            case SUBMAQUILA -> "Folio";
        };
    }

    /**
     * Clave de unicidad dentro del archivo. Las submaquilas legítimamente repiten folio
     * (una salida por FOLIO/FECHA/SUBMAQUILADOR con varios renglones), por lo que su clave
     * es compuesta FOLIO + FECHA + LINEA y no el folio aislado: es exactamente el renglón
     * que dbo.CARGA_SUBMAQUILA conserva como partida.
     */
    private String claveDuplicada(CatalogImportType type, Map<String, String> values) {
        if (type != CatalogImportType.SUBMAQUILA) return values.getOrDefault(keyColumn(type), "");
        String folio = values.getOrDefault("Folio", "").trim();
        String fecha = values.getOrDefault("Fecha", "").trim();
        String linea = values.getOrDefault("Linea", "").trim();
        if (folio.isBlank() || linea.isBlank()) return "";
        return folio + "|" + fecha + "|" + linea;
    }

    private String columnaClave(CatalogImportType type) {
        return type == CatalogImportType.SUBMAQUILA ? "Folio/Linea" : keyColumn(type);
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
