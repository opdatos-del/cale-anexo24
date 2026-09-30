package com.jovycandy.anexo24.catalogs.businessparties.customsagents.infrastructure.persistence;

import com.jovycandy.anexo24.catalogs.businessparties.customsagents.domain.model.AgenteAduanal;
import com.jovycandy.anexo24.catalogs.businessparties.customsagents.domain.port.AgenteAduanalRepository;
import com.jovycandy.anexo24.shared.api.Pagina;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.jdbc.core.*;
import org.springframework.stereotype.Repository;
import java.sql.*;
import java.util.*;

/** Adapter SP-first read-only de agente aduanal. */
@Repository
public class AgenteAduanalJdbcAdapter implements AgenteAduanalRepository {
    static final String PROCEDURE = "dbo.APP24_Q_AGENTES_ADUANALES_LISTAR";
    private static final String RESULTADO = "items", TOTAL = "Total";
    private static final RowMapper<AgenteAduanal> MAPPER = (rs, rowNum) -> new AgenteAduanal(rs.getString("clave"), rs.getString("nombre"), rs.getString("rfc"), rs.getString("patente"), rs.getString("agenciaAduanal"));
    private static final List<SqlParameter> PARAMETROS = List.of(new SqlParameter("Filtro", Types.VARCHAR), new SqlParameter("Pagina", Types.INTEGER),
            new SqlParameter("Tamano", Types.INTEGER), new SqlOutParameter(TOTAL, Types.BIGINT),
            new SqlReturnResultSet(RESULTADO, MAPPER));
    private final JdbcTemplate jdbcTemplate;
    public AgenteAduanalJdbcAdapter(@Qualifier("jdbcTemplate") JdbcTemplate jdbcTemplate) { this.jdbcTemplate = jdbcTemplate; }
    @Override @SuppressWarnings("unchecked")
    public Pagina<AgenteAduanal> findPage(String filtro, int pagina, int tamano) {
        Map<String,Object> out=jdbcTemplate.call(connection -> {
            CallableStatement statement=connection.prepareCall("{call " + PROCEDURE + "(?, ?, ?, ?)}");
            statement.setString(1,filtro); statement.setInt(2,pagina); statement.setInt(3,tamano); statement.registerOutParameter(4,Types.BIGINT); return statement;
        }, PARAMETROS);
        List<AgenteAduanal> items=(List<AgenteAduanal>)out.getOrDefault(RESULTADO,List.of()); Number total=(Number)out.get(TOTAL);
        return new Pagina<>(items,total==null?0L:total.longValue(),pagina,tamano);
    }
}
