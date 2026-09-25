package com.jovycandy.anexo24.billing.infrastructure.persistence;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.jovycandy.anexo24.billing.domain.model.PlantillaFacturacion;
import com.jovycandy.anexo24.billing.domain.port.PlantillaFacturacionRepository;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.SqlReturnResultSet;

import java.sql.CallableStatement;
import java.util.ArrayList;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Map;
import java.util.Optional;

@Repository
public class PlantillaFacturacionJdbcAdapter implements PlantillaFacturacionRepository {
    static final String ACTIVA = "app24.APP24_Q_FACTURACION_PLANTILLA_ACTIVA";
    private final JdbcTemplate jdbc;
    private final ObjectMapper mapper = new ObjectMapper();

    public PlantillaFacturacionJdbcAdapter(@Qualifier("appJdbcTemplate") JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    @Override
    public Optional<PlantillaFacturacion> findActive() {
        Map<String, Object> result = jdbc.call(connection -> {
            CallableStatement statement = connection.prepareCall("{call " + ACTIVA + "()}");
            return statement;
        }, List.of(new SqlReturnResultSet("items", (rs, n) -> Map.of(
                "nombre", rs.getString("nombre"), "version", rs.getString("version"),
                "extension", rs.getString("extension"), "columnas_json", rs.getString("columnas_json")))));
        @SuppressWarnings("unchecked") List<Map<String, Object>> rows = (List<Map<String, Object>>) result.getOrDefault("items", new ArrayList<>());
        if (rows.isEmpty()) return Optional.empty();
        Map<String, Object> row = rows.getFirst();
        try {
            List<PlantillaFacturacion.Columna> columns = mapper.readValue(
                    String.valueOf(row.get("columnas_json")), new TypeReference<>() {});
            return Optional.of(new PlantillaFacturacion(String.valueOf(row.get("nombre")),
                    String.valueOf(row.get("version")), String.valueOf(row.get("extension")), "FACTURAS", columns));
        } catch (Exception exception) {
            throw new IllegalStateException("La configuración activa de Facturación no es válida", exception);
        }
    }
}
