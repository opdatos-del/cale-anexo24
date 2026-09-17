package com.jovycandy.anexo24.administration.users.infrastructure.persistence;

import com.jovycandy.anexo24.administration.users.domain.model.UsuarioApp;
import com.jovycandy.anexo24.administration.users.domain.port.UsuarioRepository;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Repository;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;
import java.util.Optional;

/**
 * Implementación del puerto de usuarios contra el esquema {@code app24}.
 *
 * <p>Consulta parametrizada; nunca concatena SQL (contrato Módulo C,
 * principio 2 de {@code docs/03-diseno/contrato-integracion-modulo-c.md}).</p>
 */
@Repository
public class UsuarioJdbcAdapter implements UsuarioRepository {

    private final JdbcTemplate appJdbcTemplate;

    /** Mapea una fila de UsuarioApp a un objeto de dominio. */
    private static final RowMapper<UsuarioApp> MAPPER = (rs, rowNum) -> new UsuarioApp(
            rs.getLong("id"),
            rs.getString("clave"),
            rs.getString("nombre"),
            rs.getString("correo"),
            rs.getString("password_hash"),
            rs.getString("estado"),
            rs.getDate("vigencia") == null ? null : rs.getDate("vigencia").toLocalDate(),
            rs.getLong("perfil_id"));

    /**
     * Constructor con la plantilla del esquema complementario.
     *
     * @param appJdbcTemplate plantilla JDBC de ANEXO24_DEV
     */
    public UsuarioJdbcAdapter(@Qualifier("appJdbcTemplate") JdbcTemplate appJdbcTemplate) {
        this.appJdbcTemplate = appJdbcTemplate;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Optional<UsuarioApp> findByClave(String clave) {
        List<UsuarioApp> resultados = appJdbcTemplate.query(
                "SELECT id, clave, nombre, correo, password_hash, estado, vigencia, perfil_id "
                        + "FROM app24.UsuarioApp WHERE clave = ?",
                MAPPER, clave);
        return resultados.stream().findFirst();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public List<String> findPermisosByUsuario(Long usuarioId) {
        return appJdbcTemplate.query(
                "SELECT a.clave FROM app24.UsuarioApp u "
                        + "JOIN app24.PerfilActividad pa ON pa.perfil_id = u.perfil_id "
                        + "JOIN app24.Actividad a ON a.id = pa.actividad_id "
                        + "WHERE u.id = ? AND u.estado = 'ACTIVO'",
                (rs, rowNum) -> rs.getString("clave"), usuarioId);
    }
}