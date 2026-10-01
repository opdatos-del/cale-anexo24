package com.jovycandy.anexo24.reports.extended.infrastructure.persistence;

import com.jovycandy.anexo24.reports.extended.domain.model.Rectificacion;
import com.jovycandy.anexo24.reports.extended.domain.port.RectificacionRepository;
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

/** Consume el SP versionado read-only del resumen de rectificaciones. */
@Repository
public class RectificacionStoredProcedureAdapter implements RectificacionRepository {
    static final String PROCEDURE = "dbo.APP24_Q_RECTIFICACIONES_LISTAR";
    private static final String ITEMS = "items";
    private static final String TOTAL = "Total";
    private static final RowMapper<Rectificacion> MAPPER = (rs, rowNum) -> new Rectificacion(
            rs.getString("PEDIMENTO"), rs.getInt("TOTAL_RECTIFICACIONES"));
    private static final List<SqlParameter> PARAMETERS = List.of(
            new SqlParameter("Filtro", Types.VARCHAR),
            new SqlParameter("Pagina", Types.INTEGER),
            new SqlParameter("Tamano", Types.INTEGER),
            new SqlOutParameter(TOTAL, Types.BIGINT),
            new SqlReturnResultSet(ITEMS, MAPPER));

    private final JdbcTemplate jdbcTemplate;

    public RectificacionStoredProcedureAdapter(@Qualifier("jdbcTemplate") JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    @Override
    public Pagina<Rectificacion> findPage(String filtro, int pagina, int tamano) {
        Map<String, Object> result = jdbcTemplate.call(connection -> {
            CallableStatement statement = connection.prepareCall("{call " + PROCEDURE + "(?, ?, ?, ?)}");
            statement.setString(1, filtro);
            statement.setInt(2, pagina);
            statement.setInt(3, tamano);
            statement.registerOutParameter(4, Types.BIGINT);
            return statement;
        }, PARAMETERS);
        @SuppressWarnings("unchecked")
        List<Rectificacion> items = (List<Rectificacion>) result.getOrDefault(ITEMS, List.of());
        Number total = (Number) result.get(TOTAL);
        return new Pagina<>(items, total == null ? 0 : total.longValue(), pagina, tamano);
    }
}
