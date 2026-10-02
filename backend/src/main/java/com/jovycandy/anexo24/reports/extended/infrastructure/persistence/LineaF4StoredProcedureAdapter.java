package com.jovycandy.anexo24.reports.extended.infrastructure.persistence;

import com.jovycandy.anexo24.reports.extended.domain.model.LineaF4;
import com.jovycandy.anexo24.reports.extended.domain.port.LineaF4Repository;
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

/** Consume el SP read-only de líneas dirigidas F4 derivado de V_F4CTMA y V_F4DESP. */
@Repository
public class LineaF4StoredProcedureAdapter implements LineaF4Repository {
    static final String PROCEDURE = "dbo.APP24_Q_F4_LISTAR";
    private static final String ITEMS = "items";
    private static final String TOTAL = "Total";
    private static final RowMapper<LineaF4> MAPPER = (rs, rowNum) -> new LineaF4(
            rs.getString("TIPO_DESCARGA"), rs.getString("F4"), localDateTime(rs, "FECHA"),
            rs.getString("IMPORTACION"), rs.getString("CLAVE"),
            rs.getBigDecimal("INCORPORADO"), rs.getBigDecimal("SALDO"));
    private static final List<SqlParameter> PARAMETERS = List.of(
            new SqlParameter("Filtro", Types.VARCHAR),
            new SqlParameter("Pagina", Types.INTEGER),
            new SqlParameter("Tamano", Types.INTEGER),
            new SqlOutParameter(TOTAL, Types.BIGINT),
            new SqlReturnResultSet(ITEMS, MAPPER));

    private final JdbcTemplate jdbcTemplate;

    public LineaF4StoredProcedureAdapter(@Qualifier("jdbcTemplate") JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    @Override
    public Pagina<LineaF4> findPage(String filtro, int pagina, int tamano) {
        Map<String, Object> result = jdbcTemplate.call(connection -> {
            CallableStatement statement = connection.prepareCall("{call " + PROCEDURE + "(?, ?, ?, ?)}");
            statement.setString(1, filtro);
            statement.setInt(2, pagina);
            statement.setInt(3, tamano);
            statement.registerOutParameter(4, Types.BIGINT);
            return statement;
        }, PARAMETERS);
        @SuppressWarnings("unchecked")
        List<LineaF4> items = (List<LineaF4>) result.getOrDefault(ITEMS, List.of());
        Number total = (Number) result.get(TOTAL);
        return new Pagina<>(items, total == null ? 0 : total.longValue(), pagina, tamano);
    }

    private static LocalDateTime localDateTime(ResultSet resultSet, String column) throws SQLException {
        Timestamp timestamp = resultSet.getTimestamp(column);
        return timestamp == null ? null : timestamp.toLocalDateTime();
    }
}
