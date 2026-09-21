package com.jovycandy.anexo24.operations.exits.infrastructure.persistence;

import com.jovycandy.anexo24.operations.exits.domain.model.SalidaLinea;
import com.jovycandy.anexo24.operations.exits.domain.port.SalidaRepository;
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

/** Adapter que consume el procedimiento APP24 de consulta de Salidas. */
@Repository
public class SalidaStoredProcedureAdapter implements SalidaRepository {

    static final String PROCEDURE_NAME = "dbo.APP24_Q_SALIDAS_LISTAR";
    static final String RESULT_SET_NAME = "items";
    static final String TOTAL_PARAMETER = "Total";

    private static final RowMapper<SalidaLinea> SALIDA_MAPPER = (rs, rowNum) -> new SalidaLinea(
            rs.getBigDecimal("SALIDA_ID"),
            rs.getBigDecimal("PARTIDA_ID"),
            rs.getString("PEDIMENTO"),
            rs.getString("CLAVE_PEDIMENTO"),
            rs.getString("FRACCION"),
            rs.getString("UNIDAD_COMERCIAL"),
            rs.getBigDecimal("CANTIDAD"),
            rs.getString("NUMERO_PARTE"),
            localDateTime(rs, "FECHA_PAGO"));

    private static final List<SqlParameter> DECLARED_PARAMETERS = List.of(
            new SqlParameter("Desde", Types.DATE),
            new SqlParameter("Hasta", Types.DATE),
            new SqlParameter("Pedimento", Types.VARCHAR),
            new SqlParameter("ClavePedimento", Types.VARCHAR),
            new SqlParameter("Fraccion", Types.VARCHAR),
            new SqlParameter("NumeroParte", Types.VARCHAR),
            new SqlParameter("Pagina", Types.INTEGER),
            new SqlParameter("Tamano", Types.INTEGER),
            new SqlOutParameter(TOTAL_PARAMETER, Types.BIGINT),
            new SqlReturnResultSet(RESULT_SET_NAME, SALIDA_MAPPER));

    private final JdbcTemplate jdbcTemplate;

    /**
     * Constructor con la plantilla JDBC de CALE_IMMEX.
     *
     * @param jdbcTemplate plantilla JDBC de CALE_IMMEX
     */
    public SalidaStoredProcedureAdapter(@Qualifier("jdbcTemplate") JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Pagina<SalidaLinea> findPage(LocalDate desde, LocalDate hasta, String pedimento,
                                        String clavePedimento, String fraccion, String numeroParte,
                                        int pagina, int tamano) {
        Map<String, Object> resultado = jdbcTemplate.call(connection -> {
            CallableStatement statement = connection.prepareCall(
                    "{call " + PROCEDURE_NAME + "(?, ?, ?, ?, ?, ?, ?, ?, ?)}");
            statement.setDate(1, java.sql.Date.valueOf(desde));
            statement.setDate(2, java.sql.Date.valueOf(hasta));
            statement.setString(3, pedimento);
            statement.setString(4, clavePedimento);
            statement.setString(5, fraccion);
            statement.setString(6, numeroParte);
            statement.setInt(7, pagina);
            statement.setInt(8, tamano);
            statement.registerOutParameter(9, Types.BIGINT);
            return statement;
        }, DECLARED_PARAMETERS);

        List<SalidaLinea> items = extraerItems(resultado);
        Number total = (Number) resultado.get(TOTAL_PARAMETER);
        return new Pagina<>(items, total == null ? 0L : total.longValue(), pagina, tamano);
    }

    @SuppressWarnings("unchecked")
    private static List<SalidaLinea> extraerItems(Map<String, Object> resultado) {
        return (List<SalidaLinea>) resultado.getOrDefault(RESULT_SET_NAME, List.of());
    }

    private static LocalDateTime localDateTime(ResultSet resultSet, String column)
            throws SQLException {
        Timestamp timestamp = resultSet.getTimestamp(column);
        return timestamp == null ? null : timestamp.toLocalDateTime();
    }
}
