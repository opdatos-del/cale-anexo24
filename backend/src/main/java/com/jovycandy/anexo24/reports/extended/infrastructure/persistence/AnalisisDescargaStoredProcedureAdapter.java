package com.jovycandy.anexo24.reports.extended.infrastructure.persistence;

import com.jovycandy.anexo24.reports.extended.domain.model.AnalisisDescarga;
import com.jovycandy.anexo24.reports.extended.domain.port.AnalisisDescargaRepository;
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

/** Consume el SP read-only del análisis histórico de descargas. */
@Repository
public class AnalisisDescargaStoredProcedureAdapter implements AnalisisDescargaRepository {
    static final String PROCEDURE = "dbo.APP24_Q_ANALISIS_DESCARGAS_LISTAR";
    private static final String ITEMS = "items";
    private static final String TOTAL = "Total";
    private static final RowMapper<AnalisisDescarga> MAPPER = (rs, rowNum) -> new AnalisisDescarga(
            nullableLong(rs, "DESCARGA_ID"), rs.getString("PEDIMENTO_ENTRADA"), rs.getString("PARTIDA_ENTRADA"),
            rs.getString("PEDIMENTO_SALIDA"), rs.getString("PARTIDA_SALIDA"), rs.getString("MATERIAL"),
            rs.getString("PRODUCTO"), localDateTime(rs, "FECHA_IMPORTACION"), localDateTime(rs, "FECHA_SALIDA"),
            localDateTime(rs, "FECHA_VENCIMIENTO"), rs.getBigDecimal("CANTIDAD_IMPORTADA"),
            rs.getBigDecimal("CANTIDAD_EXPORTADA"), rs.getBigDecimal("CANTIDAD_INCORPORADA"),
            rs.getBigDecimal("CANTIDAD_MERMA"), rs.getBigDecimal("CANTIDAD_DESPERDICIO"),
            rs.getString("UNIDAD"));
    private static final List<SqlParameter> PARAMETERS = List.of(
            new SqlParameter("Filtro", Types.VARCHAR),
            new SqlParameter("Pagina", Types.INTEGER),
            new SqlParameter("Tamano", Types.INTEGER),
            new SqlOutParameter(TOTAL, Types.BIGINT),
            new SqlReturnResultSet(ITEMS, MAPPER));

    private final JdbcTemplate jdbcTemplate;

    public AnalisisDescargaStoredProcedureAdapter(@Qualifier("jdbcTemplate") JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    @Override
    public Pagina<AnalisisDescarga> findPage(String filtro, int pagina, int tamano) {
        Map<String, Object> result = jdbcTemplate.call(connection -> {
            CallableStatement statement = connection.prepareCall("{call " + PROCEDURE + "(?, ?, ?, ?)}");
            statement.setString(1, filtro);
            statement.setInt(2, pagina);
            statement.setInt(3, tamano);
            statement.registerOutParameter(4, Types.BIGINT);
            return statement;
        }, PARAMETERS);
        @SuppressWarnings("unchecked")
        List<AnalisisDescarga> items = (List<AnalisisDescarga>) result.getOrDefault(ITEMS, List.of());
        Number total = (Number) result.get(TOTAL);
        return new Pagina<>(items, total == null ? 0 : total.longValue(), pagina, tamano);
    }

    private static Long nullableLong(ResultSet resultSet, String column) throws SQLException {
        long value = resultSet.getLong(column);
        return resultSet.wasNull() ? null : value;
    }

    private static LocalDateTime localDateTime(ResultSet resultSet, String column) throws SQLException {
        Timestamp timestamp = resultSet.getTimestamp(column);
        return timestamp == null ? null : timestamp.toLocalDateTime();
    }
}
