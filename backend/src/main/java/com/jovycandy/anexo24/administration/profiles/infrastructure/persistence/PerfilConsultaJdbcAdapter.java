package com.jovycandy.anexo24.administration.profiles.infrastructure.persistence;

import com.jovycandy.anexo24.administration.profiles.domain.model.PerfilAdministracion;
import com.jovycandy.anexo24.administration.profiles.domain.port.PerfilConsultaRepository;
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

/** Implementación read-only de perfiles mediante consulta almacenada app24. */
@Repository
public class PerfilConsultaJdbcAdapter implements PerfilConsultaRepository {

    static final String PERFILES_LISTAR = "app24.APP24_Q_PERFILES_LISTAR";
    private static final String RESULTADO = "items";
    private static final String TOTAL = "Total";

    private static final RowMapper<PerfilAdministracion> MAPPER = (rs, rowNum) ->
            new PerfilAdministracion(
                    rs.getLong("id"),
                    rs.getString("nombre"),
                    rs.getString("estado"),
                    rs.getLong("cantidad_permisos"));

    private final JdbcTemplate appJdbcTemplate;

    public PerfilConsultaJdbcAdapter(@Qualifier("appJdbcTemplate") JdbcTemplate appJdbcTemplate) {
        this.appJdbcTemplate = appJdbcTemplate;
    }

    @Override
    @SuppressWarnings("unchecked")
    public Pagina<PerfilAdministracion> findPage(String nombre, String estado, int pagina, int tamano) {
        Map<String, Object> resultado = appJdbcTemplate.call(connection -> {
            CallableStatement statement = connection.prepareCall(
                    "{call " + PERFILES_LISTAR + "(?, ?, ?, ?, ?)}");
            statement.setString(1, nombre);
            statement.setString(2, estado);
            statement.setInt(3, pagina);
            statement.setInt(4, tamano);
            statement.registerOutParameter(5, Types.BIGINT);
            return statement;
        }, List.of(
                new SqlParameter("Nombre", Types.VARCHAR),
                new SqlParameter("Estado", Types.VARCHAR),
                new SqlParameter("Pagina", Types.INTEGER),
                new SqlParameter("Tamano", Types.INTEGER),
                new SqlOutParameter(TOTAL, Types.BIGINT),
                new SqlReturnResultSet(RESULTADO, MAPPER)));
        List<PerfilAdministracion> items = (List<PerfilAdministracion>) resultado.getOrDefault(RESULTADO, List.of());
        Number total = (Number) resultado.get(TOTAL);
        return new Pagina<>(items, total == null ? 0L : total.longValue(), pagina, tamano);
    }
}
