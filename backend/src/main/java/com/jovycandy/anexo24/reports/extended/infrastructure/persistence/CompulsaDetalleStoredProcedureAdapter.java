package com.jovycandy.anexo24.reports.extended.infrastructure.persistence;

import com.jovycandy.anexo24.reports.extended.domain.model.CompulsaDetalle;
import com.jovycandy.anexo24.reports.extended.domain.port.CompulsaDetalleRepository;
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

/** Consume el SP versionado read-only de detalle de compulsa sobre dbo.v_compulsa. */
@Repository
public class CompulsaDetalleStoredProcedureAdapter implements CompulsaDetalleRepository {
    static final String PROCEDURE = "dbo.APP24_Q_COMPULSA_DETALLE_LISTAR";
    private static final String ITEMS = "items";
    private static final String TOTAL = "Total";
    private static final RowMapper<CompulsaDetalle> MAPPER = (rs, rowNum) -> new CompulsaDetalle(
            rs.getString("PEDIMENTO_GLOSA"), nullableInteger(rs, "SEC_GLOSA"),
            rs.getString("PEDIMENTO_A24"), nullableDouble(rs, "SEC_A24"),
            rs.getString("CLAVE_PEDIMENTO_GLOSA"), rs.getString("CLAVE_PEDIMENTO_A24"),
            rs.getString("STATUS_CLAVE_PEDIMENTO"), localDateTime(rs, "FECHA_GLOSA"),
            localDateTime(rs, "FECHA_A24"), rs.getString("STATUS_FECHAS"),
            rs.getString("FRACCION_GLOSA"), rs.getString("FRACCION_A24"),
            rs.getString("STATUS_FRACCION"), rs.getString("PAIS_OD_GLOSA"),
            rs.getString("PAIS_OD_A24"), rs.getString("STATUS_PAIS_OD"),
            rs.getString("PAIS_CV_GLOSA"), rs.getString("PAIS_CV_A24"),
            rs.getString("STATUS_PAIS_CV"), nullableDouble(rs, "VALOR_ADUANA_GLOSA"),
            rs.getBigDecimal("VALOR_ADUANA_A24"), rs.getString("STATUS_VALOR_ADUANA"),
            nullableDouble(rs, "VALOR_COMERCIAL_GLOSA"), nullableDouble(rs, "VALOR_COMERCIAL_A24"),
            rs.getString("STATUS_VALOR_COMERCIAL"), nullableDouble(rs, "CANTIDAD_UMC_GLOSA"),
            rs.getBigDecimal("CANTIDAD_UMC_A24"), rs.getString("STATUS_CANTIDAD_COMERCIAL"),
            nullableDouble(rs, "CANTIDAD_UMT_GLOSA"), nullableDouble(rs, "CANTIDAD_UMT_A24"),
            rs.getString("STATUS_CANTIDAD_TARIFA"), nullableDouble(rs, "TIPO_OPERACION_GLOSA"),
            nullableDouble(rs, "TIPO_PEDIMENTO_GLOSA"));
    private static final List<SqlParameter> PARAMETERS = List.of(
            new SqlParameter("Filtro", Types.VARCHAR),
            new SqlParameter("Pagina", Types.INTEGER),
            new SqlParameter("Tamano", Types.INTEGER),
            new SqlOutParameter(TOTAL, Types.BIGINT),
            new SqlReturnResultSet(ITEMS, MAPPER));

    private final JdbcTemplate jdbcTemplate;

    public CompulsaDetalleStoredProcedureAdapter(@Qualifier("jdbcTemplate") JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    @Override
    public Pagina<CompulsaDetalle> findPage(String filtro, int pagina, int tamano) {
        Map<String, Object> result = jdbcTemplate.call(connection -> {
            CallableStatement statement = connection.prepareCall("{call " + PROCEDURE + "(?, ?, ?, ?)}");
            statement.setString(1, filtro);
            statement.setInt(2, pagina);
            statement.setInt(3, tamano);
            statement.registerOutParameter(4, Types.BIGINT);
            return statement;
        }, PARAMETERS);
        @SuppressWarnings("unchecked")
        List<CompulsaDetalle> items = (List<CompulsaDetalle>) result.getOrDefault(ITEMS, List.of());
        Number total = (Number) result.get(TOTAL);
        return new Pagina<>(items, total == null ? 0 : total.longValue(), pagina, tamano);
    }

    private static Integer nullableInteger(ResultSet resultSet, String column) throws SQLException {
        int value = resultSet.getInt(column);
        return resultSet.wasNull() ? null : value;
    }

    private static Double nullableDouble(ResultSet resultSet, String column) throws SQLException {
        double value = resultSet.getDouble(column);
        return resultSet.wasNull() ? null : value;
    }

    private static LocalDateTime localDateTime(ResultSet resultSet, String column) throws SQLException {
        Timestamp timestamp = resultSet.getTimestamp(column);
        return timestamp == null ? null : timestamp.toLocalDateTime();
    }
}
