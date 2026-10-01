package com.jovycandy.anexo24.reports.extended.infrastructure.persistence;

import com.jovycandy.anexo24.reports.extended.domain.model.OperacionBloqueada;
import com.jovycandy.anexo24.reports.extended.domain.port.OperacionBloqueadaRepository;
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

/** Consume el SP read-only del snapshot de operaciones bloqueadas. */
@Repository
public class OperacionBloqueadaStoredProcedureAdapter implements OperacionBloqueadaRepository {
    static final String PROCEDURE = "dbo.APP24_Q_OPERACIONES_BLOQUEADAS_LISTAR";
    private static final String ITEMS = "items";
    private static final String TOTAL = "Total";
    private static final RowMapper<OperacionBloqueada> MAPPER = (rs, rowNum) -> new OperacionBloqueada(
            nullableLong(rs, "BLOQUEO_ID"), rs.getString("PEDIMENTO_EXPORTACION"),
            rs.getString("CLAVE_EXPORTACION"), localDateTime(rs, "FECHA_EXPORTACION"),
            rs.getString("PRODUCTO"), rs.getBigDecimal("CANTIDAD_PRODUCTO"),
            rs.getString("FRACCION_PRODUCTO"), nullableLong(rs, "PSALIDA_KEY"),
            rs.getString("PEDIMENTO_IMPORTACION"), rs.getString("CLAVE_IMPORTACION"),
            localDateTime(rs, "FECHA_IMPORTACION"), rs.getString("MATERIAL"),
            rs.getBigDecimal("CANTIDAD_MATERIAL"), rs.getString("FRACCION_MATERIAL"),
            nullableLong(rs, "PARTIDA_KEY"), rs.getBigDecimal("INCORPORADO"),
            rs.getBigDecimal("DESPERDICIO"), rs.getBigDecimal("MERMA"),
            localDateTime(rs, "FECHA_BLOQUEO"), nullableInt(rs, "FOLIO"));
    private static final List<SqlParameter> PARAMETERS = List.of(
            new SqlParameter("Filtro", Types.VARCHAR),
            new SqlParameter("Pagina", Types.INTEGER),
            new SqlParameter("Tamano", Types.INTEGER),
            new SqlOutParameter(TOTAL, Types.BIGINT),
            new SqlReturnResultSet(ITEMS, MAPPER));

    private final JdbcTemplate jdbcTemplate;

    public OperacionBloqueadaStoredProcedureAdapter(@Qualifier("jdbcTemplate") JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    @Override
    public Pagina<OperacionBloqueada> findPage(String filtro, int pagina, int tamano) {
        Map<String, Object> result = jdbcTemplate.call(connection -> {
            CallableStatement statement = connection.prepareCall("{call " + PROCEDURE + "(?, ?, ?, ?)}");
            statement.setString(1, filtro);
            statement.setInt(2, pagina);
            statement.setInt(3, tamano);
            statement.registerOutParameter(4, Types.BIGINT);
            return statement;
        }, PARAMETERS);
        @SuppressWarnings("unchecked")
        List<OperacionBloqueada> items = (List<OperacionBloqueada>) result.getOrDefault(ITEMS, List.of());
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
