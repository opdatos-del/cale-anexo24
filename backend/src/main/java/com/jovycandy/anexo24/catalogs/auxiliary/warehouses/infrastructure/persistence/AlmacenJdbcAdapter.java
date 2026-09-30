package com.jovycandy.anexo24.catalogs.auxiliary.warehouses.infrastructure.persistence;

import com.jovycandy.anexo24.catalogs.auxiliary.warehouses.domain.model.Almacen;
import com.jovycandy.anexo24.catalogs.auxiliary.warehouses.domain.port.AlmacenRepository;
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

/** Adapter del procedimiento read-only de almacenes. */
@Repository
public class AlmacenJdbcAdapter implements AlmacenRepository {
    static final String PROCEDURE = "dbo.APP24_Q_ALMACENES_LISTAR";
    private static final String RESULTADO = "items";
    private static final String TOTAL = "Total";
    private static final RowMapper<Almacen> MAPPER = (rs, rowNum) -> new Almacen(
            rs.getLong("almacen_key"), rs.getString("clave"), rs.getString("descripcion"));
    private static final List<SqlParameter> PARAMETROS = List.of(
            new SqlParameter("Filtro", Types.VARCHAR),
            new SqlParameter("Pagina", Types.INTEGER),
            new SqlParameter("Tamano", Types.INTEGER),
            new SqlOutParameter(TOTAL, Types.BIGINT),
            new SqlReturnResultSet(RESULTADO, MAPPER));
    private final JdbcTemplate jdbcTemplate;

    public AlmacenJdbcAdapter(@Qualifier("jdbcTemplate") JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    @Override
    @SuppressWarnings("unchecked")
    public Pagina<Almacen> findPage(String filtro, int pagina, int tamano) {
        Map<String, Object> resultado = jdbcTemplate.call(connection -> {
            CallableStatement statement = connection.prepareCall("{call " + PROCEDURE + "(?, ?, ?, ?)}");
            statement.setString(1, filtro);
            statement.setInt(2, pagina);
            statement.setInt(3, tamano);
            statement.registerOutParameter(4, Types.BIGINT);
            return statement;
        }, PARAMETROS);
        List<Almacen> items = (List<Almacen>) resultado.getOrDefault(RESULTADO, List.of());
        Number total = (Number) resultado.get(TOTAL);
        return new Pagina<>(items, total == null ? 0L : total.longValue(), pagina, tamano);
    }
}
