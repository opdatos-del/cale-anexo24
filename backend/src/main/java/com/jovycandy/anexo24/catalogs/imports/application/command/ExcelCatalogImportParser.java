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
import java.math.BigInteger;
import java.text.Normalizer;
import java.time.LocalDate;
import java.time.LocalDateTime;
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
    public static final String VERSION_CONTRATO_ACTA = "ACTA-V1";
    public static final String VERSION_CONTRATO_CONSTANCIA = "CONSTANCIA-V1";
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
    // Contrato dbo.ACTA (LIVE 2026-10-05): 8 columnas de negocio. actakey es IDENTITY
    // tecnico legacy y NO forma parte del layout moderno.
    private static final List<String> ACTA_COLUMNS = List.of(
            "Folio", "Fecha", "Clave", "Linea", "Cantidad", "Umc", "DescargaDirigida", "ValorComercial");
    private static final int ACTA_TEXTO_MAX_LENGTH = 50;
    private static final int ACTA_VALOR_ESCALA_MAX = 10;
    private static final int ACTA_VALOR_ENTEROS_MAX = 8;
    private static final LocalDate ACTA_FECHA_MIN = LocalDate.of(1753, 1, 1);
    private static final LocalDate ACTA_FECHA_MAX = LocalDate.of(9999, 12, 31);
    private static final DateTimeFormatter ACTA_ISO = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss");
    // Contrato dbo.Constanciatransf (LIVE 2026-10-06): 14 columnas consumidas por
    // dbo.CARGACONSTANCIAS. CONSTANCIAKEY es IDENTITY tecnico y NO forma parte del layout.
    private static final List<String> CONSTANCIA_COLUMNS = List.of(
            "NUMERODEFOLIO", "SEC", "FECHACREACION", "PROV", "LIN", "NOPARTE", "DESCRIPCION",
            "CANTIDAD", "PEDIMENTO", "ADUANA", "MER", "PERIODO", "Val_dolares", "Val_Comercial");
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
            for (String required : requiredHeaders(type))
                if (!indexes.containsKey(normalize(required)))
                    add(errors, error(sheet.getSheetName(), null, required, "COLUMNA_OBLIGATORIA_AUSENTE", "Falta una columna confirmada como requerida."));

            int totalCells = Math.max(0, header.getLastCellNum());
            Set<String> seenKeys = new HashSet<>();
            for (int rowNumber = header.getRowNum() + 1; rowNumber <= sheet.getLastRowNum(); rowNumber++) {
                Row row = sheet.getRow(rowNumber);
                totalCells += row == null ? 0 : Math.max(0, row.getLastCellNum());
                if (totalCells > MAX_CELDAS) throw new ArchivoInvalidoException();
                Map<String, String> values = new LinkedHashMap<>();
                Map<String, String> crudos = new LinkedHashMap<>();
                boolean blank = true;
                for (String column : columns) {
                    Integer index = indexes.get(normalize(column));
                    Cell cell = index == null || row == null ? null : row.getCell(index);
                    String value = rawValue(type, column, cell, formatter);
                    String canonical = canonical(type, column, value);
                    values.put(column, canonical);
                    crudos.put(column, value);
                    blank &= value.isBlank();
                    if (cell != null && cell.getCellType() == CellType.FORMULA)
                        add(errors, error(sheet.getSheetName(), rowNumber + 1, column, "FORMULA", "No se admiten fórmulas."));
                    if (index != null && !duplicateHeaders.contains(normalize(column)) && requiredValues(type).contains(column)
                            && canonical.isBlank())
                        add(errors, error(sheet.getSheetName(), rowNumber + 1, column, "VALOR_OBLIGATORIO_AUSENTE", "El valor requerido está vacío."));
                    if (type == CatalogImportType.MATERIAL && MATERIAL_DECIMALS.contains(column)
                            && !value.isBlank() && canonical.isBlank())
                        add(errors, error(sheet.getSheetName(), rowNumber + 1, column, "DECIMAL_INVALIDO", "El factor debe ser decimal positivo."));
                }
                if (blank) add(errors, error(sheet.getSheetName(), rowNumber + 1, null, "FILA_VACIA", "La fila no contiene datos."));
                validateBusiness(type, values, crudos, sheet.getSheetName(), rowNumber + 1, errors);
                String duplicado = duplicateKey(type, values);
                if (duplicado != null && !seenKeys.add(duplicado))
                    add(errors, error(sheet.getSheetName(), rowNumber + 1, duplicateColumn(type),
                            "CLAVE_DUPLICADA", mensajeDuplicado(type)));
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

    private void validateBusiness(CatalogImportType type, Map<String, String> values, Map<String, String> crudos,
                                  String sheet, int row, List<CatalogImportError> errors) {
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
            case ACTA -> validateActa(values, crudos, sheet, row, errors);
            case CONSTANCIA -> validateConstancia(values, crudos, sheet, row, errors);
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
                                     int row, List<CatalogImportError> errors, String code, String message) {
        if (values.getOrDefault(key, "").length() != length)
            add(errors, error(sheet, row, key, code, message));
    }

    private void validateMaxLength(Map<String, String> values, String key, int max, String sheet, int row,
                                   List<CatalogImportError> errors, String code) {
        if (values.getOrDefault(key, "").length() > max)
            add(errors, error(sheet, row, key, code, "El valor supera la longitud máxima física del destino (" + max + ")."));
    }

    private void validateActa(Map<String, String> values, Map<String, String> crudos, String sheet,
                              int row, List<CatalogImportError> errors) {
        validateMaxLength(values, "Folio", ACTA_TEXTO_MAX_LENGTH, sheet, row, errors, "FOLIO_ACTA_LARGO");
        validateMaxLength(values, "Clave", ACTA_TEXTO_MAX_LENGTH, sheet, row, errors, "CLAVE_ACTA_LARGA");
        validateMaxLength(values, "Umc", ACTA_TEXTO_MAX_LENGTH, sheet, row, errors, "UMC_ACTA_LARGA");
        validateMaxLength(values, "DescargaDirigida", ACTA_TEXTO_MAX_LENGTH, sheet, row, errors, "DESCARGA_ACTA_LARGA");
        if (invalido(crudos, values, "Linea"))
            add(errors, error(sheet, row, "Linea", "LINEA_ACTA_INVALIDA", "La linea debe ser un entero de 32 bits."));
        if (invalido(crudos, values, "Cantidad"))
            add(errors, error(sheet, row, "Cantidad", "CANTIDAD_ACTA_INVALIDA", "La cantidad debe ser un numero finito."));
        if (invalido(crudos, values, "ValorComercial"))
            add(errors, error(sheet, row, "ValorComercial", "VALOR_COMERCIAL_ACTA_INVALIDO", "El valor comercial debe caber en NUMERIC(18,10)."));
        if (invalido(crudos, values, "Fecha"))
            add(errors, error(sheet, row, "Fecha", "FECHA_ACTA_INVALIDA", "La fecha debe ser ISO y estar entre 1753-01-01 y 9999-12-31."));
    }

    private boolean invalido(Map<String, String> crudos, Map<String, String> values, String column) {
        return !crudos.getOrDefault(column, "").isBlank() && values.getOrDefault(column, "").isBlank();
    }

    private String rawValue(CatalogImportType type, String column, Cell cell, DataFormatter formatter) {
        if (cell == null) return "";
        boolean fechaActa = type == CatalogImportType.ACTA && "Fecha".equals(column);
        boolean fechaConstancia = type == CatalogImportType.CONSTANCIA
                && ("FECHACREACION".equals(column) || "PERIODO".equals(column));
        if ((fechaActa || fechaConstancia) && cell.getCellType() == CellType.NUMERIC
                && DateUtil.isCellDateFormatted(cell))
            return cell.getLocalDateTimeCellValue().format(ACTA_ISO);
        return formatter.formatCellValue(cell).trim();
    }

    private String canonical(CatalogImportType type, String column, String value) {
        String trimmed = value == null ? "" : value.trim();
        if (trimmed.isBlank()) return "";
        if (type == CatalogImportType.ACTA) return canonicalActa(column, trimmed);
        if (type == CatalogImportType.CONSTANCIA) return canonicalConstancia(column, trimmed);
        if (MATERIAL_DECIMALS.contains(column)) {
            try {
                if (!trimmed.matches("[+]?(?:0|[1-9]\\d*)(?:\\.\\d+)?")) return "";
                BigDecimal decimal = new BigDecimal(trimmed);
                return decimal.signum() > 0 ? decimal.toPlainString() : "";
            } catch (NumberFormatException ignored) { return ""; }
        }
        return safe(trimmed, column.equals("DescripcionComercial") || column.equals("NOMBRE") ? 250 : 150);
    }

    /**
     * Canonicaliza una celda ACTA. Devuelve "" cuando el valor informado es invalido;
     * una celda vacia ya retorno "" antes, por lo que validateActa distingue vacio
     * (permitido) de invalido.
     */
    private String canonicalActa(String column, String trimmed) {
        return switch (column) {
            case "Fecha" -> canonicalActaFecha(trimmed);
            case "Linea" -> canonicalActaLinea(trimmed);
            case "Cantidad" -> canonicalActaCantidad(trimmed);
            case "ValorComercial" -> canonicalActaValorComercial(trimmed);
            default -> safe(trimmed, 150);
        };
    }

    private String canonicalActaFecha(String value) {
        try {
            LocalDateTime momento;
            try {
                momento = LocalDateTime.parse(value);
            } catch (DateTimeParseException sinHora) {
                momento = LocalDate.parse(value).atStartOfDay();
            }
            LocalDate dia = momento.toLocalDate();
            if (dia.isBefore(ACTA_FECHA_MIN) || dia.isAfter(ACTA_FECHA_MAX)) return "";
            return momento.format(ACTA_ISO);
        } catch (DateTimeParseException invalida) {
            return "";
        }
    }

    private String canonicalActaLinea(String value) {
        try {
            BigInteger entero = new BigDecimal(value).toBigIntegerExact();
            if (entero.compareTo(BigInteger.valueOf(Integer.MIN_VALUE)) < 0
                    || entero.compareTo(BigInteger.valueOf(Integer.MAX_VALUE)) > 0) return "";
            return entero.toString();
        } catch (NumberFormatException | ArithmeticException invalida) {
            return "";
        }
    }

    private String canonicalActaCantidad(String value) {
        try {
            BigDecimal numero = new BigDecimal(value);
            if (!Double.isFinite(numero.doubleValue())) return "";
            return numero.toPlainString();
        } catch (NumberFormatException invalida) {
            return "";
        }
    }

    private String canonicalActaValorComercial(String value) {
        try {
            BigDecimal numero = new BigDecimal(value).stripTrailingZeros();
            int escala = Math.max(numero.scale(), 0);
            int enteros = numero.precision() - numero.scale();
            if (escala > ACTA_VALOR_ESCALA_MAX || enteros > ACTA_VALOR_ENTEROS_MAX) return "";
            return numero.toPlainString();
        } catch (NumberFormatException invalida) {
            return "";
        }
    }

    private void validateConstancia(Map<String, String> values, Map<String, String> crudos, String sheet,
                                    int row, List<CatalogImportError> errors) {
        validateMaxLength(values, "NUMERODEFOLIO", 50, sheet, row, errors, "NUMERODEFOLIO_CONSTANCIA_LARGO");
        validateMaxLength(values, "SEC", 5, sheet, row, errors, "SEC_CONSTANCIA_LARGA");
        validateMaxLength(values, "PROV", 50, sheet, row, errors, "PROV_CONSTANCIA_LARGO");
        validateMaxLength(values, "NOPARTE", 50, sheet, row, errors, "NOPARTE_CONSTANCIA_LARGO");
        validateMaxLength(values, "DESCRIPCION", 100, sheet, row, errors, "DESCRIPCION_CONSTANCIA_LARGA");
        validateMaxLength(values, "PEDIMENTO", 50, sheet, row, errors, "PEDIMENTO_CONSTANCIA_LARGO");
        validateMaxLength(values, "ADUANA", 10, sheet, row, errors, "ADUANA_CONSTANCIA_LARGA");
        validateMaxLength(values, "MER", 10, sheet, row, errors, "MER_CONSTANCIA_LARGO");
        if (invalido(crudos, values, "FECHACREACION"))
            add(errors, error(sheet, row, "FECHACREACION", "FECHA_CONSTANCIA_INVALIDA", "La fecha de creacion debe ser ISO y estar entre 1753-01-01 y 9999-12-31."));
        if (invalido(crudos, values, "PERIODO"))
            add(errors, error(sheet, row, "PERIODO", "PERIODO_CONSTANCIA_INVALIDO", "El periodo debe ser ISO y estar entre 1753-01-01 y 9999-12-31."));
        if (invalido(crudos, values, "LIN"))
            add(errors, error(sheet, row, "LIN", "LIN_CONSTANCIA_INVALIDO", "La linea debe ser un entero representable en NUMERIC(18,0)."));
        if (invalido(crudos, values, "CANTIDAD"))
            add(errors, error(sheet, row, "CANTIDAD", "CANTIDAD_CONSTANCIA_INVALIDA", "La cantidad debe ser un entero representable en NUMERIC(18,0)."));
        if (invalido(crudos, values, "Val_dolares"))
            add(errors, error(sheet, row, "Val_dolares", "VAL_DOLARES_CONSTANCIA_INVALIDO", "El valor en dolares debe caber en NUMERIC(18,10)."));
        if (invalido(crudos, values, "Val_Comercial"))
            add(errors, error(sheet, row, "Val_Comercial", "VAL_COMERCIAL_CONSTANCIA_INVALIDO", "El valor comercial debe caber en NUMERIC(18,10)."));
    }

    private String canonicalConstancia(String column, String trimmed) {
        return switch (column) {
            case "FECHACREACION", "PERIODO" -> canonicalActaFecha(trimmed);
            case "LIN", "CANTIDAD" -> canonicalConstanciaEntero(trimmed);
            case "Val_dolares", "Val_Comercial" -> canonicalActaValorComercial(trimmed);
            default -> safe(trimmed, 150);
        };
    }

    private String canonicalConstanciaEntero(String value) {
        try {
            BigInteger entero = new BigDecimal(value).toBigIntegerExact();
            if (entero.abs().compareTo(BigInteger.TEN.pow(18)) >= 0) return "";
            return entero.toString();
        } catch (NumberFormatException | ArithmeticException invalida) {
            return "";
        }
    }

    private CatalogImportArchivo result(CatalogImportType type, String filename, String hash, List<String> columns,
                                        List<CatalogImportFila> rows, List<CatalogImportError> errors, boolean failed) {
        return new CatalogImportArchivo(type, safe(filename == null ? "archivo" : filename.replaceAll("[\\r\\n\\\\/]", "_"), 255),
                hash, versionContrato(type), List.copyOf(columns), List.copyOf(rows), List.copyOf(errors), failed);
    }

    private List<String> columns(CatalogImportType type) {
            return switch (type) {
                case MATERIAL -> MATERIAL_COLUMNS;
                case PRODUCTO -> PRODUCT_COLUMNS;
                case CLIENTE -> CLIENT_COLUMNS;
                case PROVEEDOR -> PROVIDER_COLUMNS;
            case AGENTE -> AGENT_COLUMNS;
            case ACTA -> ACTA_COLUMNS;
            case CONSTANCIA -> CONSTANCIA_COLUMNS;
            };
        }

    /** Encabezados que deben existir para que el layout sea determinista. */
    private List<String> requiredHeaders(CatalogImportType type) {
        return switch (type) {
            case ACTA -> ACTA_COLUMNS;
            case CONSTANCIA -> CONSTANCIA_COLUMNS;
            default -> requiredValues(type);
        };
    }

    /**
     * Valores que no pueden ir vacios. Para los catalogos legacy refleja lo auditado;
     * para ACTA queda vacio a proposito: dbo.ACTA admite NULL y dbo.CARGAACTAS no valida
     * obligatoriedad, por lo que el parser no impone reglas de negocio inventadas.
     */
    private List<String> requiredValues(CatalogImportType type) {
        return switch (type) {
            case MATERIAL -> List.of("ClaveMaterial", "UnidadComercial", "Fraccion");
            case PRODUCTO -> List.of("CVE_PRODUCTO", "NOMBRE", "UNIDAD", "fraccion");
            case CLIENTE -> List.of("Clave", "Nombre", "IdFiscal", "TipoNE");
            case PROVEEDOR -> List.of("Clave", "Nombre", "IdFiscal", "TipoNE");
            case AGENTE -> List.of("Clave", "Nombre", "Patente");
            case ACTA -> List.of();
            case CONSTANCIA -> List.of();
        };
    }

    private String keyColumn(CatalogImportType type) {
        return switch (type) {
            case MATERIAL -> "ClaveMaterial";
            case PRODUCTO -> "CVE_PRODUCTO";
            case CLIENTE -> "Clave";
            case PROVEEDOR -> "Clave";
            case AGENTE -> "Clave";
            case ACTA -> "Folio";
            case CONSTANCIA -> "NUMERODEFOLIO";
        };
    }

    private String versionContrato(CatalogImportType type) {
        return switch (type) {
            case ACTA -> VERSION_CONTRATO_ACTA;
            case CONSTANCIA -> VERSION_CONTRATO_CONSTANCIA;
            default -> VERSION_CONTRATO;
        };
    }

    /** Clave de duplicado del archivo. ACTA usa FOLIO + LINEA (nunca FECHA). */
    private String duplicateKey(CatalogImportType type, Map<String, String> values) {
        // CONSTANCIA_DUPLICATE_STRATEGY = NONE: CARGACONSTANCIAS no deduplica; inventar una
        // clave divergiria del contrato legacy (inserta por NUMERODEFOLIO/SEC sin chequeo).
        if (type == CatalogImportType.CONSTANCIA) return null;
        if (type == CatalogImportType.ACTA) {
            String folio = values.getOrDefault("Folio", "");
            String linea = values.getOrDefault("Linea", "");
            if (folio.isBlank() || linea.isBlank()) return null;
            return normalize(folio) + "␟" + normalize(linea);
        }
        String key = values.getOrDefault(keyColumn(type), "");
        return key.isBlank() ? null : normalize(key);
    }

    private String duplicateColumn(CatalogImportType type) {
        return type == CatalogImportType.ACTA ? "Folio,Linea" : keyColumn(type);
    }

    private String mensajeDuplicado(CatalogImportType type) {
        return type == CatalogImportType.ACTA
                ? "La combinacion Folio+Linea se repite en el archivo."
                : "La clave se repite en el archivo.";
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
