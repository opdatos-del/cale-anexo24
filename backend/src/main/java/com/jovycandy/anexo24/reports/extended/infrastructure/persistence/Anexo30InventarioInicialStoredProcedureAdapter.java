package com.jovycandy.anexo24.reports.extended.infrastructure.persistence;

import com.jovycandy.anexo24.reports.extended.domain.model.Anexo30InventarioInicial;
import com.jovycandy.anexo24.reports.extended.domain.port.Anexo30InventarioInicialRepository;
import com.jovycandy.anexo24.shared.api.Pagina;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.core.SqlOutParameter;
import org.springframework.jdbc.core.SqlParameter;
import org.springframework.jdbc.core.SqlReturnResultSet;
import org.springframework.stereotype.Repository;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.sql.Types;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

/** Consume el SP read-only del inventario inicial del Anexo 30. */
@Repository
public class Anexo30InventarioInicialStoredProcedureAdapter implements Anexo30InventarioInicialRepository {
    static final String PROCEDURE = "dbo.APP24_Q_ANEXO30_INVENTARIO_INICIAL_LISTAR";
    private static final String ITEMS = "items";
    private static final String TOTAL = "Total";
    private static final RowMapper<Anexo30InventarioInicial> MAPPER = (rs, rowNum) -> new Anexo30InventarioInicial(
            rs.getString("PATENTE"), rs.getString("NUMERO_PEDIMENTO"), rs.getString("CLAVE_SECCION_ADUANERA"),
            localDateTime(rs, "FECHA_SELECCION"), rs.getString("FRACCION"),
            rs.getBigDecimal("VALOR_COMERCIAL_HISTORICO"), rs.getString("IDENTIFICADOR_ACTIVO_FIJO"));
    private static final List<SqlParameter> PARAMETERS = List.of(
            new SqlParameter("Filtro", Types.VARCHAR), new SqlParameter("Pagina", Types.INTEGER),
            new SqlParameter("Tamano", Types.INTEGER), new SqlOutParameter(TOTAL, Types.BIGINT),
            new SqlReturnResultSet(ITEMS, MAPPER));
    private final JdbcTemplate jdbcTemplate;

    public Anexo30InventarioInicialStoredProcedureAdapter(@Qualifier("jdbcTemplate") JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    @Override
    public Pagina<Anexo30InventarioInicial> findPage(String filtro, int pagina, int tamano) {
        Map<String, Object> result = jdbcTemplate.call(connection -> {
            var statement = connection.prepareCall("{call " + PROCEDURE + "(?, ?, ?, ?)}");
            statement.setString(1, filtro); statement.setInt(2, pagina); statement.setInt(3, tamano);
            statement.registerOutParameter(4, Types.BIGINT); return statement;
        }, PARAMETERS);
        @SuppressWarnings("unchecked")
        List<Anexo30InventarioInicial> items = (List<Anexo30InventarioInicial>) result.getOrDefault(ITEMS, List.of());
        Number total = (Number) result.get(TOTAL);
        return new Pagina<>(items, total == null ? 0 : total.longValue(), pagina, tamano);
    }

    private static LocalDateTime localDateTime(ResultSet rs, String column) throws SQLException {
        Timestamp value = rs.getTimestamp(column);
        return value == null ? null : value.toLocalDateTime();
    }
}
