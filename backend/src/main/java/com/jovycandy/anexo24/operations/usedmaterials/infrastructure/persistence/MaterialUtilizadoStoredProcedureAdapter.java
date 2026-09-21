package com.jovycandy.anexo24.operations.usedmaterials.infrastructure.persistence;

import com.jovycandy.anexo24.operations.usedmaterials.domain.model.MaterialUtilizado;
import com.jovycandy.anexo24.operations.usedmaterials.domain.port.MaterialUtilizadoRepository;
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

/** Adapter JDBC que consume el procedimiento APP24 de materiales utilizados. */
@Repository
public class MaterialUtilizadoStoredProcedureAdapter implements MaterialUtilizadoRepository {

    static final String PROCEDURE_NAME = "dbo.APP24_Q_MATERIALES_UTILIZADOS_LISTAR";
    static final String RESULT_SET_NAME = "items";
    static final String TOTAL_PARAMETER = "Total";

    private static final RowMapper<MaterialUtilizado> MATERIAL_UTILIZADO_MAPPER = (rs, rowNum) ->
            new MaterialUtilizado(
                    rs.getLong("DESCARGA_ID"),
                    rs.getBigDecimal("ENTRADA_ID"),
                    rs.getBigDecimal("PARTIDA_ENTRADA_ID"),
                    rs.getBigDecimal("SALIDA_ID"),
                    rs.getBigDecimal("PARTIDA_SALIDA_ID"),
                    rs.getString("PEDIMENTO_ENTRADA"),
                    rs.getString("PEDIMENTO_SALIDA"),
                    rs.getString("MATERIAL_CODE"),
                    rs.getString("MATERIAL_DESCRIPTION"),
                    rs.getString("PRODUCT_CODE"),
                    rs.getString("PRODUCT_DESCRIPTION"),
                    rs.getBigDecimal("CANTIDAD_INCORPORADA"),
                    rs.getBigDecimal("CANTIDAD_MERMA"),
                    rs.getBigDecimal("CANTIDAD_DESPERDICIO"),
                    rs.getBigDecimal("CANTIDAD_TOTAL_DESCARGADA"),
                    rs.getString("UNIDAD"),
                    localDateTime(rs, "FECHA"));

    private static final List<SqlParameter> DECLARED_PARAMETERS = List.of(
            new SqlParameter("Desde", Types.DATE),
            new SqlParameter("Hasta", Types.DATE),
            new SqlParameter("Material", Types.VARCHAR),
            new SqlParameter("Producto", Types.VARCHAR),
            new SqlParameter("PedimentoSalida", Types.VARCHAR),
            new SqlParameter("ClavePedimentoSalida", Types.VARCHAR),
            new SqlParameter("Pagina", Types.INTEGER),
            new SqlParameter("Tamano", Types.INTEGER),
            new SqlOutParameter(TOTAL_PARAMETER, Types.BIGINT),
            new SqlReturnResultSet(RESULT_SET_NAME, MATERIAL_UTILIZADO_MAPPER));

    private final JdbcTemplate jdbcTemplate;

    /**
     * Constructor con la plantilla JDBC de CALE_IMMEX.
     *
     * @param jdbcTemplate plantilla JDBC del Módulo C
     */
    public MaterialUtilizadoStoredProcedureAdapter(@Qualifier("jdbcTemplate") JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    /** {@inheritDoc} */
    @Override
    public Pagina<MaterialUtilizado> findPage(LocalDate desde, LocalDate hasta, String material,
                                              String producto, String pedimentoSalida,
                                              String clavePedimentoSalida, int pagina, int tamano) {
        Map<String, Object> resultado = jdbcTemplate.call(connection -> {
            CallableStatement statement = connection.prepareCall(
                    "{call " + PROCEDURE_NAME + "(?, ?, ?, ?, ?, ?, ?, ?, ?)}");
            statement.setDate(1, java.sql.Date.valueOf(desde));
            statement.setDate(2, java.sql.Date.valueOf(hasta));
            statement.setString(3, material);
            statement.setString(4, producto);
            statement.setString(5, pedimentoSalida);
            statement.setString(6, clavePedimentoSalida);
            statement.setInt(7, pagina);
            statement.setInt(8, tamano);
            statement.registerOutParameter(9, Types.BIGINT);
            return statement;
        }, DECLARED_PARAMETERS);

        @SuppressWarnings("unchecked")
        List<MaterialUtilizado> items = (List<MaterialUtilizado>) resultado
                .getOrDefault(RESULT_SET_NAME, List.of());
        Number total = (Number) resultado.get(TOTAL_PARAMETER);
        return new Pagina<>(items, total == null ? 0L : total.longValue(), pagina, tamano);
    }

    private static LocalDateTime localDateTime(ResultSet resultSet, String column)
            throws SQLException {
        Timestamp timestamp = resultSet.getTimestamp(column);
        return timestamp == null ? null : timestamp.toLocalDateTime();
    }
}
