package com.jovycandy.anexo24.savedqueries.application;

import tools.jackson.databind.JsonNode;
import com.jovycandy.anexo24.savedqueries.domain.model.ConsultaGuardadaAlcance;
import com.jovycandy.anexo24.shared.exception.SolicitudInvalidaException;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.util.Set;

/** Valida whitelist estricta de filtros. Nunca interpreta ni ejecuta texto como SQL. */
@Component
public class ConsultaGuardadaCriteriosValidator {
    private static final int MAX_JSON = 4000;
    private static final Set<String> OPERATION_KEYS = Set.of("from", "to", "customsDocument", "customsCode", "tariffFraction", "partNumber");
    private static final Set<String> USED_MATERIAL_KEYS = Set.of("from", "to", "material", "product", "exitCustomsDocument", "exitCustomsCode");
    private static final Set<String> FIXED_ASSET_KEYS = Set.of("from", "to", "customsDocument", "customsCode", "partNumber", "description", "serialNumber", "brand", "model");
    private static final Set<String> REPORT_KEYS = Set.of("type", "from", "to", "customsDocument", "customsCode", "tariffFraction", "partNumber", "material", "product", "userId", "module", "result", "correlationId", "filter");
    private static final Set<String> REPORT_TYPES = Set.of("entradas", "salidas", "materiales-utilizados", "bitacora", "compulsa", "rectificaciones", "vencimientos", "dirigidos", "analisis-descargas", "operaciones-bloqueadas", "f4", "anexo30-revision-entradas", "anexo30-revision-fracciones", "anexo30-revision-descargas", "anexo30-revision-comparativa", "rectificaciones-detalle", "compulsa-detalle", "saldos");

    /** Serializa JSON canónico sólo después de validar root, llaves, tipo y profundidad. */
    public String validarYSerializar(ConsultaGuardadaAlcance alcance, JsonNode criterios) {
        if (alcance == null || criterios == null || !criterios.isObject()) invalido();
        Set<String> allowed = allowed(alcance);
        criterios.properties().forEach(entry -> { if (!allowed.contains(entry.getKey())) invalido(); });
        for (String key : allowed) {
            JsonNode value = criterios.get(key);
            if (value == null) continue;
            if ("userId".equals(key)) validarUserId(value);
            else if ("type".equals(key)) validarReportType(value);
            else validarTexto(key, value);
        }
        validarFechas(alcance, criterios);
        if (alcance == ConsultaGuardadaAlcance.REPORTES && (criterios.get("type") == null || criterios.get("type").isNull())) invalido();
        String json = criterios.toString();
        if (json.length() > MAX_JSON) invalido();
        return json;
    }

    private Set<String> allowed(ConsultaGuardadaAlcance alcance) {
        return switch (alcance) {
            case ENTRADAS, SALIDAS -> OPERATION_KEYS;
            case MATERIALES_UTILIZADOS -> USED_MATERIAL_KEYS;
            case ACTIVOS_FIJOS -> FIXED_ASSET_KEYS;
            case REPORTES -> REPORT_KEYS;
        };
    }

    private void validarTexto(String key, JsonNode value) {
        if (value.isNull()) return;
        if (!value.isTextual() || value.textValue().length() > limite(key)) invalido();
        if ("from".equals(key) || "to".equals(key)) {
            try { LocalDate.parse(value.textValue()); } catch (DateTimeParseException exception) { invalido(); }
        }
    }

    private void validarReportType(JsonNode value) {
        if (!value.isTextual() || !REPORT_TYPES.contains(value.textValue())) invalido();
    }

    private void validarUserId(JsonNode value) {
        if (!value.isNull() && (!value.isIntegralNumber() || !value.canConvertToLong() || value.longValue() <= 0)) invalido();
    }

    private void validarFechas(ConsultaGuardadaAlcance alcance, JsonNode criterios) {
        JsonNode from = criterios.get("from");
        JsonNode to = criterios.get("to");
        boolean hasFrom = from != null && !from.isNull();
        boolean hasTo = to != null && !to.isNull();
        if (hasFrom != hasTo) invalido();
        if (hasFrom && from.textValue().compareTo(to.textValue()) > 0) invalido();
        if ((alcance == ConsultaGuardadaAlcance.ENTRADAS || alcance == ConsultaGuardadaAlcance.SALIDAS
                || alcance == ConsultaGuardadaAlcance.MATERIALES_UTILIZADOS) && !hasFrom) invalido();
    }

    private int limite(String key) {
        return switch (key) {
            case "from", "to" -> 10;
            case "customsDocument", "exitCustomsDocument" -> 60;
            case "customsCode", "exitCustomsCode" -> 20;
            case "tariffFraction" -> 30;
            case "partNumber", "material", "product", "serialNumber", "brand", "model" -> 100;
            case "description" -> 250;
            case "module" -> 80;
            case "result" -> 20;
            case "correlationId" -> 40;
            case "filter" -> 100;
            default -> 250;
        };
    }

    private static void invalido() { throw new SolicitudInvalidaException("Los criterios de la consulta guardada no son válidos."); }
}
