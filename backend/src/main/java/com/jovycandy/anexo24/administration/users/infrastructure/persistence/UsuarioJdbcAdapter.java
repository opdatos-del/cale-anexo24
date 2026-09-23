package com.jovycandy.anexo24.administration.users.infrastructure.persistence;

import com.jovycandy.anexo24.administration.users.domain.model.UsuarioAcceso;
import com.jovycandy.anexo24.administration.users.domain.model.UsuarioApp;
import com.jovycandy.anexo24.administration.users.domain.port.UsuarioRepository;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.core.SqlParameter;

import org.springframework.jdbc.core.SqlReturnResultSet;
import org.springframework.stereotype.Repository;

import java.sql.CallableStatement;
import java.sql.Types;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/** Implementación del puerto de usuarios mediante consultas almacenadas app24. */
@Repository
public class UsuarioJdbcAdapter implements UsuarioRepository {

    static final String USUARIO_POR_CLAVE = "app24.APP24_Q_USUARIO_POR_CLAVE";
    static final String USUARIO_ACCESO = "app24.APP24_Q_USUARIO_ACCESO";
    private static final String USUARIO_RESULTADO = "usuario";
    private static final String ACCESO_RESULTADO = "acceso";

    private static final RowMapper<UsuarioApp> USUARIO_MAPPER = (rs, rowNum) -> new UsuarioApp(
            rs.getLong("id"),
            rs.getString("clave"),
            rs.getString("nombre"),
            rs.getString("correo"),
            rs.getString("password_hash"),
            rs.getString("estado"),
            rs.getDate("vigencia") == null ? null : rs.getDate("vigencia").toLocalDate(),
            rs.getLong("perfil_id"));

    private final JdbcTemplate appJdbcTemplate;

    public UsuarioJdbcAdapter(@Qualifier("appJdbcTemplate") JdbcTemplate appJdbcTemplate) {
        this.appJdbcTemplate = appJdbcTemplate;
    }

    @Override
    @SuppressWarnings("unchecked")
    public Optional<UsuarioApp> findByClave(String clave) {
        Map<String, Object> resultado = appJdbcTemplate.call(connection -> {
            CallableStatement statement = connection.prepareCall(
                    "{call " + USUARIO_POR_CLAVE + "(?)}");
            statement.setString(1, clave);
            return statement;
        }, List.of(new SqlParameter("Clave", Types.VARCHAR),
                new SqlReturnResultSet(USUARIO_RESULTADO, USUARIO_MAPPER)));
        List<UsuarioApp> usuarios = (List<UsuarioApp>) resultado.getOrDefault(USUARIO_RESULTADO, List.of());
        return usuarios.stream().findFirst();
    }

    @Override
    @SuppressWarnings("unchecked")
    public Optional<UsuarioAcceso> findAccesoByUsuario(Long usuarioId) {
        RowMapper<Object[]> mapper = (rs, rowNum) -> new Object[]{
                rs.getObject("perfil_id", Long.class),
                rs.getString("perfil_estado"),
                rs.getString("permiso")};
        Map<String, Object> resultado = appJdbcTemplate.call(connection -> {
            CallableStatement statement = connection.prepareCall(
                    "{call " + USUARIO_ACCESO + "(?)}");
            statement.setLong(1, usuarioId);
            return statement;
        }, List.of(new SqlParameter("UsuarioId", Types.BIGINT),
                new SqlReturnResultSet(ACCESO_RESULTADO, mapper)));

        List<Object[]> filas = (List<Object[]>) resultado.getOrDefault(ACCESO_RESULTADO, List.of());
        if (filas.isEmpty()) {
            return Optional.empty();
        }
        Long perfilId = (Long) filas.get(0)[0];
        String perfilEstado = (String) filas.get(0)[1];
        List<String> permisos = new ArrayList<>();
        for (Object[] fila : filas) {
            if (fila[2] != null) {
                permisos.add((String) fila[2]);
            }
        }
        return Optional.of(new UsuarioAcceso(perfilId, perfilEstado, permisos));
    }
}
