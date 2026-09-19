package com.jovycandy.anexo24.catalogs.structures.infrastructure.persistence;

import com.jovycandy.anexo24.catalogs.structures.domain.model.EstructuraDetalle;
import com.jovycandy.anexo24.catalogs.structures.domain.port.EstructuraRepository;
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
import java.util.List;
import java.util.Map;

/** Adapter que consume el procedimiento APP24 de consulta de estructuras. */
@Repository
public class EstructuraStoredProcedureAdapter implements EstructuraRepository {

    static final String PROCEDURE_NAME = "dbo.APP24_Q_ESTRUCTURAS_LISTAR";
    static final String RESULT_SET_NAME = "items";
    static final String TOTAL_PARAMETER = "Total";

    private static final RowMapper<EstructuraDetalle> ESTRUCTURA_DETALLE_MAPPER =
            (rs, rowNum) -> new EstructuraDetalle(
                    rs.getLong("ESTRUCTURAKEY"),
                    rs.getBigDecimal("PRODUCTOKEY"),
                    rs.getString("CVE_PRODUCTO"),
                    rs.getString("PRODUCTO_DESCRIPCION"),
                    rs.getString("PRODUCTO_UNIDAD"),
                    localDateTime(rs, "FECHA_INICIO"),
                    localDateTime(rs, "FECHA_FIN"),
                    rs.getBigDecimal("PRODMATKEY"),
                    rs.getString("CVE_MATERIAL"),
                    rs.getString("MATERIAL_DESCRIPCION"),
                    rs.getString("MATERIAL_UNIDAD"),
                    rs.getString("MATERIAL_FRACCION"),
                    rs.getBigDecimal("CANT_UTILIZADA"),
                    rs.getBigDecimal("CANT_MERMADA"),
                    rs.getBigDecimal("CANT_DESPERDICIADA"));

    private static final List<SqlParameter> DECLARED_PARAMETERS = List.of(
            new SqlParameter("Producto", Types.VARCHAR),
            new SqlParameter("Material", Types.VARCHAR),
            new SqlParameter("Pagina", Types.INTEGER),
            new SqlParameter("Tamano", Types.INTEGER),
            new SqlOutParameter(TOTAL_PARAMETER, Types.BIGINT),
            new SqlReturnResultSet(RESULT_SET_NAME, ESTRUCTURA_DETALLE_MAPPER));

    private final JdbcTemplate jdbcTemplate;

    /**
     * Constructor con la plantilla JDBC de CALE_IMMEX.
     *
     * @param jdbcTemplate plantilla JDBC del Módulo C
     */
    public EstructuraStoredProcedureAdapter(@Qualifier("jdbcTemplate") JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    @SuppressWarnings("unchecked")
    public Pagina<EstructuraDetalle> findPage(String producto, String material,
                                               int pagina, int tamano) {
        Map<String, Object> resultado = jdbcTemplate.call(connection -> {
            CallableStatement statement = connection.prepareCall(
                    "{call " + PROCEDURE_NAME + "(?, ?, ?, ?, ?)}");
            statement.setString(1, producto);
            statement.setString(2, material);
            statement.setInt(3, pagina);
            statement.setInt(4, tamano);
            statement.registerOutParameter(5, Types.BIGINT);
            return statement;
        }, DECLARED_PARAMETERS);

        List<EstructuraDetalle> items = (List<EstructuraDetalle>) resultado
                .getOrDefault(RESULT_SET_NAME, List.of());
        Number total = (Number) resultado.get(TOTAL_PARAMETER);
        return new Pagina<>(items, total == null ? 0L : total.longValue(), pagina, tamano);
    }

    private static java.time.LocalDateTime localDateTime(ResultSet resultSet, String column)
            throws SQLException {
        Timestamp timestamp = resultSet.getTimestamp(column);
        return timestamp == null ? null : timestamp.toLocalDateTime();
    }
}
