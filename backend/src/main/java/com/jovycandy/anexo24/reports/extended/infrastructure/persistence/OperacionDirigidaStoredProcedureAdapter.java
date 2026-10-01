package com.jovycandy.anexo24.reports.extended.infrastructure.persistence;

import com.jovycandy.anexo24.reports.extended.domain.model.OperacionDirigida;
import com.jovycandy.anexo24.reports.extended.domain.port.OperacionDirigidaRepository;
import com.jovycandy.anexo24.shared.api.Pagina;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.core.SqlOutParameter;
import org.springframework.jdbc.core.SqlParameter;
import org.springframework.jdbc.core.SqlReturnResultSet;
import org.springframework.stereotype.Repository;

import java.sql.CallableStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.sql.Types;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

/** Consume el SP read-only de líneas dirigidas derivado de V_STATUS_DESCARGAS. */
@Repository
public class OperacionDirigidaStoredProcedureAdapter implements OperacionDirigidaRepository {
    static final String PROCEDURE = "dbo.APP24_Q_DIRIGIDOS_LISTAR";
    private static final String ITEMS = "items";
    private static final String TOTAL = "Total";
    private static final RowMapper<OperacionDirigida> MAPPER = (rs, rowNum) -> new OperacionDirigida(
            nullableLong(rs, "SALIDA_KEY"), nullableLong(rs, "PSALIDA_KEY"), rs.getString("DOCUMENTO"),
            localDateTime(rs, "FECHA_SALIDA"), rs.getString("CLAVE_PEDIMENTO"), nullableInt(rs, "SECUENCIA"),
            rs.getString("PRODUCTO"), rs.getBigDecimal("CANTIDAD"), rs.getString("FACTURA"),
            rs.getString("DESCARGO"), rs.getString("DIRIGIDO"), rs.getBigDecimal("VALOR_DESCARGA_DOLARES"),
            rs.getBigDecimal("VALOR_DESCARGA_PESOS"), rs.getString("TIENE_ESTRUCTURA"),
            nullableInt(rs, "NUMERO_MATERIALES"));
    private static final List<SqlParameter> PARAMETERS = List.of(
            new SqlParameter("Filtro", Types.VARCHAR),
            new SqlParameter("Pagina", Types.INTEGER),
            new SqlParameter("Tamano", Types.INTEGER),
            new SqlOutParameter(TOTAL, Types.BIGINT),
            new SqlReturnResultSet(ITEMS, MAPPER));

    private final JdbcTemplate jdbcTemplate;

    public OperacionDirigidaStoredProcedureAdapter(@Qualifier("jdbcTemplate") JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    @Override
    public Pagina<OperacionDirigida> findPage(String filtro, int pagina, int tamano) {
        Map<String, Object> result = jdbcTemplate.call(connection -> {
            CallableStatement statement = connection.prepareCall("{call " + PROCEDURE + "(?, ?, ?, ?)}");
            statement.setString(1, filtro);
            statement.setInt(2, pagina);
            statement.setInt(3, tamano);
            statement.registerOutParameter(4, Types.BIGINT);
            return statement;
        }, PARAMETERS);
        @SuppressWarnings("unchecked")
        List<OperacionDirigida> items = (List<OperacionDirigida>) result.getOrDefault(ITEMS, List.of());
        Number total = (Number) result.get(TOTAL);
        return new Pagina<>(items, total == null ? 0 : total.longValue(), pagina, tamano);
    }

    private static Long nullableLong(ResultSet resultSet, String column) throws SQLException {
        long value = resultSet.getLong(column);
        return resultSet.wasNull() ? null : value;
    }

    private static Integer nullableInt(ResultSet resultSet, String column) throws SQLException {
        int value = resultSet.getInt(column);
        return resultSet.wasNull() ? null : value;
    }

    private static LocalDateTime localDateTime(ResultSet resultSet, String column) throws SQLException {
        Timestamp timestamp = resultSet.getTimestamp(column);
        return timestamp == null ? null : timestamp.toLocalDateTime();
    }
}
