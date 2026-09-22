package com.jovycandy.anexo24.administration.users.infrastructure.persistence;

import com.jovycandy.anexo24.administration.users.domain.port.UsuarioComandoRepository;
import com.jovycandy.anexo24.shared.exception.RecursoDuplicadoException;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;

/** Adaptador JDBC de comandos administrativos contra {@code ANEXO24_DEV.app24}. */
@Repository
public class UsuarioComandoJdbcAdapter implements UsuarioComandoRepository {
    private static final String INSERT_USUARIO = """
            INSERT INTO app24.UsuarioApp
                (clave, nombre, correo, password_hash, estado, vigencia, perfil_id)
            OUTPUT INSERTED.id
            VALUES (?, ?, ?, ?, 'ACTIVO', ?, ?)
            """;
    private static final String UPDATE_DATOS = """
            UPDATE app24.UsuarioApp
            SET nombre = ?, correo = ?
            WHERE id = ?
            """;
    private static final String UPDATE_ESTADO = """
            UPDATE app24.UsuarioApp
            SET estado = ?
            WHERE id = ?
            """;
    private static final String UPDATE_PERFIL = """
            UPDATE app24.UsuarioApp
            SET perfil_id = ?
            WHERE id = ?
            """;
    private static final String UPDATE_VIGENCIA = """
            UPDATE app24.UsuarioApp
            SET vigencia = ?
            WHERE id = ?
            """;
    private static final String EXISTS_CAPACIDAD_ADMINISTRATIVA = """
            SELECT CASE WHEN EXISTS (
                SELECT 1
                FROM app24.UsuarioApp u
                JOIN app24.PerfilApp p ON p.id = u.perfil_id
                WHERE u.estado = 'ACTIVO'
                  AND (u.vigencia IS NULL OR u.vigencia >= ?)
                  AND p.estado = 'ACTIVO'
                  AND EXISTS (
                      SELECT 1
                      FROM app24.PerfilActividad pa
                      JOIN app24.Actividad a ON a.id = pa.actividad_id
                      WHERE pa.perfil_id = p.id
                        AND a.clave = 'USUARIOS_ADMINISTRAR'
                  )
                  AND EXISTS (
                      SELECT 1
                      FROM app24.PerfilActividad pa
                      JOIN app24.Actividad a ON a.id = pa.actividad_id
                      WHERE pa.perfil_id = p.id
                        AND a.clave = 'PERFILES_ADMINISTRAR'
                  )
            ) THEN 1 ELSE 0 END
            """;
    private final JdbcTemplate appJdbcTemplate;

    public UsuarioComandoJdbcAdapter(@Qualifier("appJdbcTemplate") JdbcTemplate appJdbcTemplate) {
        this.appJdbcTemplate = appJdbcTemplate;
    }

    @Override
    public boolean existsByClave(String clave) {
        return contar("SELECT COUNT(*) FROM app24.UsuarioApp WHERE clave = ?", clave) > 0;
    }

    @Override
    public boolean existsByCorreo(String correo) {
        return contar("SELECT COUNT(*) FROM app24.UsuarioApp WHERE correo = ?", correo) > 0;
    }

    @Override
    public boolean existsByCorreoExceptoUsuario(String correo, Long usuarioId) {
        return contar("SELECT COUNT(*) FROM app24.UsuarioApp WHERE correo = ? AND id <> ?", correo, usuarioId) > 0;
    }

    @Override
    public Long crear(String clave, String nombre, String correo, String passwordHash, LocalDate vigencia, Long perfilId) {
        try {
            return appJdbcTemplate.queryForObject(INSERT_USUARIO, Long.class,
                    clave, nombre, correo, passwordHash, vigencia, perfilId);
        } catch (DuplicateKeyException exception) {
            throw new RecursoDuplicadoException();
        }
    }

    @Override
    public int actualizarDatos(Long usuarioId, String nombre, String correo) {
        try {
            return appJdbcTemplate.update(UPDATE_DATOS, nombre, correo, usuarioId);
        } catch (DuplicateKeyException exception) {
            throw new RecursoDuplicadoException();
        }
    }

    @Override
    public int actualizarEstado(Long usuarioId, String estado) {
        return appJdbcTemplate.update(UPDATE_ESTADO, estado, usuarioId);
    }

    @Override
    public int actualizarPerfil(Long usuarioId, Long perfilId) {
        return appJdbcTemplate.update(UPDATE_PERFIL, perfilId, usuarioId);
    }

    @Override
    public int actualizarVigencia(Long usuarioId, LocalDate vigencia) {
        return appJdbcTemplate.update(UPDATE_VIGENCIA, vigencia, usuarioId);
    }

    @Override
    public boolean existsConCapacidadAdministrativa(LocalDate fechaActual) {
        Integer existe = appJdbcTemplate.queryForObject(EXISTS_CAPACIDAD_ADMINISTRATIVA, Integer.class, fechaActual);
        return existe != null && existe == 1;
    }

    private long contar(String sql, Object... params) {
        Long total = appJdbcTemplate.queryForObject(sql, Long.class, params);
        return total == null ? 0L : total;
    }
}
