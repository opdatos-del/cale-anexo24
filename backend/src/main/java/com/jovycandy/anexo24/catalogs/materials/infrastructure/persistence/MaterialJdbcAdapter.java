package com.jovycandy.anexo24.catalogs.materials.infrastructure.persistence;

import com.jovycandy.anexo24.catalogs.materials.domain.model.Material;
import com.jovycandy.anexo24.catalogs.materials.domain.port.MaterialRepository;
import com.jovycandy.anexo24.shared.api.Pagina;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.core.SqlOutParameter;
import org.springframework.jdbc.core.SqlParameter;
import org.springframework.jdbc.core.SqlReturnResultSet;
import org.springframework.stereotype.Repository;

import java.sql.CallableStatement;
import java.sql.Types;
import java.util.List;
import java.util.Map;

/** Adapter que consume el procedimiento APP24 de consulta de Materiales. */
@Repository
public class MaterialJdbcAdapter implements MaterialRepository {

    static final String PROCEDURE_NAME = "dbo.APP24_Q_MATERIALES_LISTAR";
    static final String RESULT_SET_NAME = "items";
    static final String TOTAL_PARAMETER = "Total";

    private static final RowMapper<Material> MATERIAL_MAPPER = (rs, rowNum) -> new Material(
            rs.getBigDecimal("materialkey"),
            rs.getString("clave"),
            rs.getString("descripcion"),
            rs.getString("fraccion"),
            rs.getString("unidad"),
            rs.getString("unidadt"),
            rs.getString("tipomaterial"),
            rs.getString("tipo"),
            rs.getBigDecimal("FactorUM"),
            rs.getBigDecimal("IGIE"));

    private static final List<SqlParameter> DECLARED_PARAMETERS = List.of(
            new SqlParameter("Filtro", Types.VARCHAR),
            new SqlParameter("Pagina", Types.INTEGER),
            new SqlParameter("Tamano", Types.INTEGER),
            new SqlOutParameter(TOTAL_PARAMETER, Types.BIGINT),
            new SqlReturnResultSet(RESULT_SET_NAME, MATERIAL_MAPPER));

    private final JdbcTemplate jdbcTemplate;

    /**
     * Constructor con la plantilla JDBC de CALE_IMMEX.
     *
     * @param jdbcTemplate plantilla JDBC del Módulo C
     */
    public MaterialJdbcAdapter(@Qualifier("jdbcTemplate") JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    @SuppressWarnings("unchecked")
    public Pagina<Material> findPage(String filtro, int pagina, int tamano) {
        Map<String, Object> resultado = jdbcTemplate.call(connection -> {
            CallableStatement statement = connection.prepareCall(
                    "{call " + PROCEDURE_NAME + "(?, ?, ?, ?)}");
            statement.setString(1, filtro);
            statement.setInt(2, pagina);
            statement.setInt(3, tamano);
            statement.registerOutParameter(4, Types.BIGINT);
            return statement;
        }, DECLARED_PARAMETERS);

        List<Material> items = (List<Material>) resultado.getOrDefault(RESULT_SET_NAME, List.of());
        Number total = (Number) resultado.get(TOTAL_PARAMETER);
        return new Pagina<>(items, total == null ? 0L : total.longValue(), pagina, tamano);
    }
}
