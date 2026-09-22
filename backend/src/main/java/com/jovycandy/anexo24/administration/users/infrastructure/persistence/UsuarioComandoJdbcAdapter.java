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

    private long contar(String sql, Object... params) {
        Long total = appJdbcTemplate.queryForObject(sql, Long.class, params);
        return total == null ? 0L : total;
    }
}
