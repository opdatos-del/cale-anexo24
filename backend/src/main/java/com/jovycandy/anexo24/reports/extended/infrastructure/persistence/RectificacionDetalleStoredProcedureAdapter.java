package com.jovycandy.anexo24.reports.extended.infrastructure.persistence;

import com.jovycandy.anexo24.reports.extended.domain.model.RectificacionDetalle;
import com.jovycandy.anexo24.reports.extended.domain.port.RectificacionDetalleRepository;
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

/** Consume el SP versionado read-only del detalle de rectificaciones (fuente dbo.v_rectificaciones). */
@Repository
public class RectificacionDetalleStoredProcedureAdapter implements RectificacionDetalleRepository {
    static final String PROCEDURE = "dbo.APP24_Q_RECTIFICACIONES_DETALLE_LISTAR";
    private static final String ITEMS = "items";
    private static final String TOTAL = "Total";
    private static final RowMapper<RectificacionDetalle> MAPPER = (rs, rowNum) -> new RectificacionDetalle(
            rs.getString("PEDIMENTO"), rs.getString("CLAVE_PEDIMENTO"), rs.getString("DESCARGA"),
            rs.getString("PEDIMENTO_ORIGINAL"), rs.getString("EXISTE_PEDIMENTO"),
            rs.getString("CLAVE_PEDIMENTO_ORIGINAL"), rs.getString("DESCARGA_ORIGINAL"), rs.getString("STATUS"));
    private static final List<SqlParameter> PARAMETERS = List.of(
            new SqlParameter("Filtro", Types.VARCHAR),
            new SqlParameter("Pagina", Types.INTEGER),
            new SqlParameter("Tamano", Types.INTEGER),
            new SqlOutParameter(TOTAL, Types.BIGINT),
            new SqlReturnResultSet(ITEMS, MAPPER));

    private final JdbcTemplate jdbcTemplate;

    public RectificacionDetalleStoredProcedureAdapter(@Qualifier("jdbcTemplate") JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    @Override
    public Pagina<RectificacionDetalle> findPage(String filtro, int pagina, int tamano) {
        Map<String, Object> result = jdbcTemplate.call(connection -> {
            CallableStatement statement = connection.prepareCall("{call " + PROCEDURE + "(?, ?, ?, ?)}");
            statement.setString(1, filtro);
            statement.setInt(2, pagina);
            statement.setInt(3, tamano);
            statement.registerOutParameter(4, Types.BIGINT);
            return statement;
        }, PARAMETERS);
        @SuppressWarnings("unchecked")
        List<RectificacionDetalle> items = (List<RectificacionDetalle>) result.getOrDefault(ITEMS, List.of());
        Number total = (Number) result.get(TOTAL);
        return new Pagina<>(items, total == null ? 0 : total.longValue(), pagina, tamano);
    }
}
