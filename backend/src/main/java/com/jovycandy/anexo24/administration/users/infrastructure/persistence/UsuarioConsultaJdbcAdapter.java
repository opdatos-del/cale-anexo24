package com.jovycandy.anexo24.administration.users.infrastructure.persistence;

import com.jovycandy.anexo24.administration.users.domain.model.UsuarioAdministracion;
import com.jovycandy.anexo24.administration.users.domain.port.UsuarioConsultaRepository;
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
import java.util.Optional;

/** Implementación read-only de usuarios mediante consultas almacenadas app24. */
@Repository
public class UsuarioConsultaJdbcAdapter implements UsuarioConsultaRepository {

    static final String USUARIOS_LISTAR = "app24.APP24_Q_USUARIOS_LISTAR";
    static final String USUARIO_OBTENER = "app24.APP24_Q_USUARIO_OBTENER";
    private static final String RESULTADO = "items";
    private static final String TOTAL = "Total";

    private static final RowMapper<UsuarioAdministracion> MAPPER = (rs, rowNum) ->
            new UsuarioAdministracion(
                    rs.getLong("id"),
                    rs.getString("clave"),
                    rs.getString("nombre"),
                    rs.getString("correo"),
                    rs.getString("estado"),
                    rs.getDate("vigencia") == null ? null : rs.getDate("vigencia").toLocalDate(),
                    rs.getLong("perfil_id"),
                    rs.getString("perfil_nombre"));

    private final JdbcTemplate appJdbcTemplate;

    public UsuarioConsultaJdbcAdapter(@Qualifier("appJdbcTemplate") JdbcTemplate appJdbcTemplate) {
        this.appJdbcTemplate = appJdbcTemplate;
    }

    @Override
    @SuppressWarnings("unchecked")
    public Pagina<UsuarioAdministracion> findPage(
            String clave, String nombre, String correo, String estado, Long perfilId,
            int pagina, int tamano) {
        Map<String, Object> resultado = appJdbcTemplate.call(connection -> {
            CallableStatement statement = connection.prepareCall(
                    "{call " + USUARIOS_LISTAR + "(?, ?, ?, ?, ?, ?, ?, ?)}");
            statement.setString(1, clave);
            statement.setString(2, nombre);
            statement.setString(3, correo);
            statement.setString(4, estado);
            if (perfilId == null) {
                statement.setNull(5, Types.BIGINT);
            } else {
                statement.setLong(5, perfilId);
            }
            statement.setInt(6, pagina);
            statement.setInt(7, tamano);
            statement.registerOutParameter(8, Types.BIGINT);
            return statement;
        }, List.of(
                new SqlParameter("Clave", Types.VARCHAR),
                new SqlParameter("Nombre", Types.VARCHAR),
                new SqlParameter("Correo", Types.VARCHAR),
                new SqlParameter("Estado", Types.VARCHAR),
                new SqlParameter("PerfilId", Types.BIGINT),
                new SqlParameter("Pagina", Types.INTEGER),
                new SqlParameter("Tamano", Types.INTEGER),
                new SqlOutParameter(TOTAL, Types.BIGINT),
                new SqlReturnResultSet(RESULTADO, MAPPER)));
        List<UsuarioAdministracion> items = (List<UsuarioAdministracion>) resultado.getOrDefault(RESULTADO, List.of());
        Number total = (Number) resultado.get(TOTAL);
        return new Pagina<>(items, total == null ? 0L : total.longValue(), pagina, tamano);
    }

    @Override
    @SuppressWarnings("unchecked")
    public Optional<UsuarioAdministracion> findById(Long id) {
        Map<String, Object> resultado = appJdbcTemplate.call(connection -> {
            CallableStatement statement = connection.prepareCall(
                    "{call " + USUARIO_OBTENER + "(?)}");
            statement.setLong(1, id);
            return statement;
        }, List.of(new SqlParameter("UsuarioId", Types.BIGINT),
                new SqlReturnResultSet(RESULTADO, MAPPER)));
        List<UsuarioAdministracion> usuarios = (List<UsuarioAdministracion>) resultado.getOrDefault(RESULTADO, List.of());
        return usuarios.stream().findFirst();
    }
}
