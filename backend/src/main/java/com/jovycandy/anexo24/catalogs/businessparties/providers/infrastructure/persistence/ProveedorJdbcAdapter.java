package com.jovycandy.anexo24.catalogs.businessparties.providers.infrastructure.persistence;

import com.jovycandy.anexo24.catalogs.businessparties.providers.domain.model.Proveedor;
import com.jovycandy.anexo24.catalogs.businessparties.providers.domain.port.ProveedorRepository;
import com.jovycandy.anexo24.shared.api.Pagina;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.jdbc.core.*;
import org.springframework.stereotype.Repository;
import java.sql.*;
import java.util.*;

/** Adapter SP-first read-only de proveedor. */
@Repository
public class ProveedorJdbcAdapter implements ProveedorRepository {
    static final String PROCEDURE = "dbo.APP24_Q_PROVEEDORES_LISTAR";
    private static final String RESULTADO = "items", TOTAL = "Total";
    private static final RowMapper<Proveedor> MAPPER = (rs, rowNum) -> new Proveedor(rs.getLong("proveedorkey"), rs.getString("clave"), rs.getString("nombre"), rs.getString("idfiscal"), rs.getString("pais"), rs.getString("correo"));
    private static final List<SqlParameter> PARAMETROS = List.of(new SqlParameter("Filtro", Types.VARCHAR), new SqlParameter("Pagina", Types.INTEGER),
            new SqlParameter("Tamano", Types.INTEGER), new SqlOutParameter(TOTAL, Types.BIGINT),
            new SqlReturnResultSet(RESULTADO, MAPPER));
    private final JdbcTemplate jdbcTemplate;
    public ProveedorJdbcAdapter(@Qualifier("jdbcTemplate") JdbcTemplate jdbcTemplate) { this.jdbcTemplate = jdbcTemplate; }
    @Override @SuppressWarnings("unchecked")
    public Pagina<Proveedor> findPage(String filtro, int pagina, int tamano) {
        Map<String,Object> out=jdbcTemplate.call(connection -> {
            CallableStatement statement=connection.prepareCall("{call " + PROCEDURE + "(?, ?, ?, ?)}");
            statement.setString(1,filtro); statement.setInt(2,pagina); statement.setInt(3,tamano); statement.registerOutParameter(4,Types.BIGINT); return statement;
        }, PARAMETROS);
        List<Proveedor> items=(List<Proveedor>)out.getOrDefault(RESULTADO,List.of()); Number total=(Number)out.get(TOTAL);
        return new Pagina<>(items,total==null?0L:total.longValue(),pagina,tamano);
    }
}
