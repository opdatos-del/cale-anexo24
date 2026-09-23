package com.jovycandy.anexo24.administration.users.infrastructure.persistence;

import com.jovycandy.anexo24.administration.users.domain.port.UsuarioComandoRepository;
import com.jovycandy.anexo24.shared.exception.EstadoIncompatibleException;
import com.jovycandy.anexo24.shared.exception.RecursoDuplicadoException;
import com.jovycandy.anexo24.shared.exception.RecursoNoEncontradoException;
import com.jovycandy.anexo24.shared.exception.SolicitudInvalidaException;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.dao.DataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.SqlOutParameter;
import org.springframework.jdbc.core.SqlParameter;
import org.springframework.stereotype.Repository;

import java.sql.CallableStatement;
import java.sql.SQLException;
import java.sql.Types;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;

/** Adaptador de commands administrativos mediante Stored Procedures app24. */
@Repository
public class UsuarioComandoJdbcAdapter implements UsuarioComandoRepository {
    static final String CREAR = "app24.APP24_C_USUARIO_CREAR";
    static final String ACTUALIZAR_DATOS = "app24.APP24_C_USUARIO_ACTUALIZAR_DATOS";
    static final String CAMBIAR_ESTADO = "app24.APP24_C_USUARIO_CAMBIAR_ESTADO";
    static final String CAMBIAR_PERFIL = "app24.APP24_C_USUARIO_CAMBIAR_PERFIL";
    static final String CAMBIAR_VIGENCIA = "app24.APP24_C_USUARIO_CAMBIAR_VIGENCIA";
    static final String RESTABLECER_PASSWORD = "app24.APP24_C_USUARIO_RESTABLECER_PASSWORD";
    private static final String NUEVO_USUARIO_ID = "NuevoUsuarioId";

    private final JdbcTemplate appJdbcTemplate;

    public UsuarioComandoJdbcAdapter(@Qualifier("appJdbcTemplate") JdbcTemplate appJdbcTemplate) {
        this.appJdbcTemplate = appJdbcTemplate;
    }

    @Override
    public Long crear(String clave, String nombre, String correo, String passwordHash,
                      LocalDate vigencia, Long perfilId) {
        try {
            Map<String, Object> resultado = appJdbcTemplate.call(connection -> {
                CallableStatement statement = connection.prepareCall(
                        "{call " + CREAR + "(?, ?, ?, ?, ?, ?, ?)}");
                statement.setString(1, clave);
                statement.setString(2, nombre);
                statement.setString(3, correo);
                statement.setString(4, passwordHash);
                if (vigencia == null) statement.setNull(5, Types.DATE);
                else statement.setDate(5, java.sql.Date.valueOf(vigencia));
                statement.setLong(6, perfilId);
                statement.registerOutParameter(7, Types.BIGINT);
                return statement;
            }, List.of(
                    new SqlParameter("Clave", Types.VARCHAR),
                    new SqlParameter("Nombre", Types.VARCHAR),
                    new SqlParameter("Correo", Types.VARCHAR),
                    new SqlParameter("PasswordHash", Types.VARCHAR),
                    new SqlParameter("Vigencia", Types.DATE),
                    new SqlParameter("PerfilId", Types.BIGINT),
                    new SqlOutParameter(NUEVO_USUARIO_ID, Types.BIGINT)));
            Number id = (Number) resultado.get(NUEVO_USUARIO_ID);
            if (id == null || id.longValue() <= 0) {
                throw new IllegalStateException("El command de creación no devolvió un ID válido");
            }
            return id.longValue();
        } catch (DataAccessException exception) {
            throw traducir(exception);
        }
    }

    @Override
    public void actualizarDatos(Long usuarioId, String nombre, String correo) {
        ejecutar(ACTUALIZAR_DATOS,
                connection -> {
                    CallableStatement statement = connection.prepareCall(
                            "{call " + ACTUALIZAR_DATOS + "(?, ?, ?)}");
                    statement.setLong(1, usuarioId);
                    statement.setString(2, nombre);
                    statement.setString(3, correo);
                    return statement;
                }, List.of(
                        new SqlParameter("UsuarioId", Types.BIGINT),
                        new SqlParameter("Nombre", Types.VARCHAR),
                        new SqlParameter("Correo", Types.VARCHAR)));
    }

    @Override
    public void actualizarEstado(Long usuarioId, String estado, Long actorId, LocalDate fechaActual) {
        ejecutar(CAMBIAR_ESTADO,
                connection -> {
                    CallableStatement statement = connection.prepareCall(
                            "{call " + CAMBIAR_ESTADO + "(?, ?, ?, ?)}");
                    statement.setLong(1, usuarioId);
                    statement.setString(2, estado);
                    statement.setLong(3, actorId);
                    statement.setDate(4, java.sql.Date.valueOf(fechaActual));
                    return statement;
                }, List.of(
                        new SqlParameter("UsuarioId", Types.BIGINT),
                        new SqlParameter("NuevoEstado", Types.VARCHAR),
                        new SqlParameter("ActorId", Types.BIGINT),
                        new SqlParameter("FechaActual", Types.DATE)));
    }

    @Override
    public void actualizarPerfil(Long usuarioId, Long perfilId, Long actorId, LocalDate fechaActual) {
        ejecutar(CAMBIAR_PERFIL,
                connection -> {
                    CallableStatement statement = connection.prepareCall(
                            "{call " + CAMBIAR_PERFIL + "(?, ?, ?, ?)}");
                    statement.setLong(1, usuarioId);
                    statement.setLong(2, perfilId);
                    statement.setLong(3, actorId);
                    statement.setDate(4, java.sql.Date.valueOf(fechaActual));
                    return statement;
                }, List.of(
                        new SqlParameter("UsuarioId", Types.BIGINT),
                        new SqlParameter("PerfilId", Types.BIGINT),
                        new SqlParameter("ActorId", Types.BIGINT),
                        new SqlParameter("FechaActual", Types.DATE)));
    }

    @Override
    public void actualizarVigencia(Long usuarioId, LocalDate vigencia, Long actorId, LocalDate fechaActual) {
        ejecutar(CAMBIAR_VIGENCIA,
                connection -> {
                    CallableStatement statement = connection.prepareCall(
                            "{call " + CAMBIAR_VIGENCIA + "(?, ?, ?, ?)}");
                    statement.setLong(1, usuarioId);
                    if (vigencia == null) statement.setNull(2, Types.DATE);
                    else statement.setDate(2, java.sql.Date.valueOf(vigencia));
                    statement.setLong(3, actorId);
                    statement.setDate(4, java.sql.Date.valueOf(fechaActual));
                    return statement;
                }, List.of(
                        new SqlParameter("UsuarioId", Types.BIGINT),
                        new SqlParameter("Vigencia", Types.DATE),
                        new SqlParameter("ActorId", Types.BIGINT),
                        new SqlParameter("FechaActual", Types.DATE)));
    }

    @Override
    public void restablecerPassword(Long usuarioId, String passwordHash) {
        ejecutar(RESTABLECER_PASSWORD,
                connection -> {
                    CallableStatement statement = connection.prepareCall(
                            "{call " + RESTABLECER_PASSWORD + "(?, ?)}");
                    statement.setLong(1, usuarioId);
                    statement.setString(2, passwordHash);
                    return statement;
                }, List.of(
                        new SqlParameter("UsuarioId", Types.BIGINT),
                        new SqlParameter("PasswordHash", Types.VARCHAR)));
    }

    private void ejecutar(String procedure, org.springframework.jdbc.core.CallableStatementCreator creator,
                          List<SqlParameter> parameters) {
        try {
            appJdbcTemplate.call(creator, parameters);
        } catch (DataAccessException exception) {
            throw traducir(exception);
        }
    }

    private RuntimeException traducir(DataAccessException exception) {
        Integer codigo = codigoSql(exception);
        if (codigo == null) return exception;
        return switch (codigo) {
            case 51101, 51102 -> new RecursoNoEncontradoException();
            case 51103, 51105, 51106, 51107 -> new EstadoIncompatibleException();
            case 51104, 2601, 2627 -> new RecursoDuplicadoException();
            case 51108 -> new SolicitudInvalidaException("Los parámetros del command son inválidos.");
            case 51150 -> new IllegalStateException("El command afectó un número inconsistente de filas", exception);
            default -> exception;
        };
    }

    private Integer codigoSql(Throwable throwable) {
        Throwable actual = throwable;
        while (actual != null) {
            if (actual instanceof SQLException sqlException) return sqlException.getErrorCode();
            actual = actual.getCause();
        }
        return null;
    }
}
