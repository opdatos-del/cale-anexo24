package com.jovycandy.anexo24.administration.activities.infrastructure.persistence;

import com.jovycandy.anexo24.administration.activities.domain.model.ActividadAdministracion;
import com.jovycandy.anexo24.administration.activities.domain.port.ActividadConsultaRepository;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.core.SqlReturnResultSet;
import org.springframework.stereotype.Repository;

import java.sql.CallableStatement;
import java.util.List;
import java.util.Map;

/** Implementación read-only del catálogo de actividades mediante consulta almacenada app24. */
@Repository
public class ActividadConsultaJdbcAdapter implements ActividadConsultaRepository {

    static final String ACTIVIDADES_LISTAR = "app24.APP24_Q_ACTIVIDADES_LISTAR";
    private static final String RESULTADO = "items";

    private static final RowMapper<ActividadAdministracion> MAPPER = (rs, rowNum) ->
            new ActividadAdministracion(
                    rs.getLong("id"),
                    rs.getString("clave"),
                    rs.getString("nombre"),
                    rs.getString("recurso"),
                    rs.getString("accion"));

    private final JdbcTemplate appJdbcTemplate;

    public ActividadConsultaJdbcAdapter(@Qualifier("appJdbcTemplate") JdbcTemplate appJdbcTemplate) {
        this.appJdbcTemplate = appJdbcTemplate;
    }

    @Override
    @SuppressWarnings("unchecked")
    public List<ActividadAdministracion> findAll() {
        Map<String, Object> resultado = appJdbcTemplate.call(connection -> {
            CallableStatement statement = connection.prepareCall("{call " + ACTIVIDADES_LISTAR + "}");
            return statement;
        }, List.of(new SqlReturnResultSet(RESULTADO, MAPPER)));
        return (List<ActividadAdministracion>) resultado.getOrDefault(RESULTADO, List.of());
    }
}
