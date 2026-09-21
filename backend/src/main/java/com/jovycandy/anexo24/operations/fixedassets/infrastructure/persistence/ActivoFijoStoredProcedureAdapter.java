package com.jovycandy.anexo24.operations.fixedassets.infrastructure.persistence;

import com.jovycandy.anexo24.operations.fixedassets.domain.model.ActivoFijo;
import com.jovycandy.anexo24.operations.fixedassets.domain.port.ActivoFijoRepository;
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
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

/** Adapter que consume el procedimiento APP24 de consulta de Activos Fijos. */
@Repository
public class ActivoFijoStoredProcedureAdapter implements ActivoFijoRepository {

    static final String PROCEDURE_NAME = "dbo.APP24_Q_ACTIVOS_FIJOS_LISTAR";
    static final String RESULT_SET_NAME = "items";
    static final String TOTAL_PARAMETER = "Total";

    private static final RowMapper<ActivoFijo> ACTIVO_FIJO_MAPPER = (rs, rowNum) -> new ActivoFijo(
            rs.getBigDecimal("PARTIDA_ENTRADA_ID"),
            rs.getBigDecimal("IMPORTACION_ID"),
            rs.getString("PEDIMENTO"),
            rs.getString("CLAVE_PEDIMENTO"),
            localDateTime(rs, "FECHA_IMPORTACION"),
            rs.getString("NUMERO_PARTE"),
            rs.getString("DESCRIPCION"),
            rs.getString("FRACCION"),
            rs.getBigDecimal("CANTIDAD"),
            rs.getString("UNIDAD"),
            rs.getString("NUMERO_SERIE"),
            rs.getString("MARCA"),
            rs.getString("MODELO"));

    private static final List<SqlParameter> DECLARED_PARAMETERS = List.of(
            new SqlParameter("Desde", Types.DATE),
            new SqlParameter("Hasta", Types.DATE),
            new SqlParameter("Pedimento", Types.VARCHAR),
            new SqlParameter("ClavePedimento", Types.VARCHAR),
            new SqlParameter("NumeroParte", Types.VARCHAR),
            new SqlParameter("Descripcion", Types.VARCHAR),
            new SqlParameter("Serie", Types.VARCHAR),
            new SqlParameter("Marca", Types.VARCHAR),
            new SqlParameter("Modelo", Types.VARCHAR),
            new SqlParameter("Pagina", Types.INTEGER),
            new SqlParameter("Tamano", Types.INTEGER),
            new SqlOutParameter(TOTAL_PARAMETER, Types.BIGINT),
            new SqlReturnResultSet(RESULT_SET_NAME, ACTIVO_FIJO_MAPPER));

    private final JdbcTemplate jdbcTemplate;

    /**
     * Constructor con la plantilla JDBC de CALE_IMMEX.
     *
     * @param jdbcTemplate plantilla JDBC del Módulo C
     */
    public ActivoFijoStoredProcedureAdapter(@Qualifier("jdbcTemplate") JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Pagina<ActivoFijo> findPage(LocalDate desde, LocalDate hasta, String pedimento,
                                       String clavePedimento, String numeroParte, String descripcion,
                                       String serie, String marca, String modelo, int pagina, int tamano) {
        Map<String, Object> resultado = jdbcTemplate.call(connection -> {
            CallableStatement statement = connection.prepareCall(
                    "{call " + PROCEDURE_NAME + "(?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)}");
            setDateOrNull(statement, 1, desde);
            setDateOrNull(statement, 2, hasta);
            statement.setString(3, pedimento);
            statement.setString(4, clavePedimento);
            statement.setString(5, numeroParte);
            statement.setString(6, descripcion);
            statement.setString(7, serie);
            statement.setString(8, marca);
            statement.setString(9, modelo);
            statement.setInt(10, pagina);
            statement.setInt(11, tamano);
            statement.registerOutParameter(12, Types.BIGINT);
            return statement;
        }, DECLARED_PARAMETERS);

        @SuppressWarnings("unchecked")
        List<ActivoFijo> items = (List<ActivoFijo>) resultado
                .getOrDefault(RESULT_SET_NAME, List.of());
        Number total = (Number) resultado.get(TOTAL_PARAMETER);
        return new Pagina<>(items, total == null ? 0L : total.longValue(), pagina, tamano);
    }

    private static void setDateOrNull(CallableStatement statement, int index, LocalDate value)
            throws SQLException {
        if (value == null) {
            statement.setNull(index, Types.DATE);
        } else {
            statement.setDate(index, java.sql.Date.valueOf(value));
        }
    }

    private static LocalDateTime localDateTime(ResultSet resultSet, String column)
            throws SQLException {
        Timestamp timestamp = resultSet.getTimestamp(column);
        return timestamp == null ? null : timestamp.toLocalDateTime();
    }
}
