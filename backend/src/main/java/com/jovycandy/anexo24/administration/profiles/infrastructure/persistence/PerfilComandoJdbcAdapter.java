package com.jovycandy.anexo24.administration.profiles.infrastructure.persistence;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.jovycandy.anexo24.administration.profiles.domain.port.PerfilComandoRepository;
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

/** Adaptador de commands de perfiles mediante los SP F5C de app24. */
@Repository
public class PerfilComandoJdbcAdapter implements PerfilComandoRepository {
    static final String CREAR = "app24.APP24_C_PERFIL_CREAR";
    static final String ACTUALIZAR_NOMBRE = "app24.APP24_C_PERFIL_ACTUALIZAR_NOMBRE";
    static final String CAMBIAR_ESTADO = "app24.APP24_C_PERFIL_CAMBIAR_ESTADO";
    static final String REEMPLAZAR_PERMISOS = "app24.APP24_C_PERFIL_REEMPLAZAR_PERMISOS";
    private static final String NUEVO_PERFIL_ID = "NuevoPerfilId";

    private final JdbcTemplate appJdbcTemplate;
    private final ObjectMapper objectMapper = new ObjectMapper();

    public PerfilComandoJdbcAdapter(@Qualifier("appJdbcTemplate") JdbcTemplate appJdbcTemplate) {
        this.appJdbcTemplate = appJdbcTemplate;
    }

    @Override
    public Long crear(String nombre) {
        try {
            Map<String, Object> resultado = appJdbcTemplate.call(connection -> {
                CallableStatement statement = connection.prepareCall("{call " + CREAR + "(?, ?)}");
                statement.setString(1, nombre);
                statement.registerOutParameter(2, Types.BIGINT);
                return statement;
            }, List.of(new SqlParameter("Nombre", Types.VARCHAR), new SqlOutParameter(NUEVO_PERFIL_ID, Types.BIGINT)));
            Number id = (Number) resultado.get(NUEVO_PERFIL_ID);
            if (id == null || id.longValue() <= 0) throw new IllegalStateException("El command no devolvió un ID válido");
            return id.longValue();
        } catch (DataAccessException exception) {
            throw traducir(exception);
        }
    }

    @Override
    public void actualizarNombre(Long perfilId, String nombre) {
        ejecutar(connection -> {
            CallableStatement statement = connection.prepareCall("{call " + ACTUALIZAR_NOMBRE + "(?, ?)}");
            statement.setLong(1, perfilId);
            statement.setString(2, nombre);
            return statement;
        }, List.of(new SqlParameter("PerfilId", Types.BIGINT), new SqlParameter("Nombre", Types.VARCHAR)));
    }

    @Override
    public void cambiarEstado(Long perfilId, String estado, LocalDate fechaActual) {
        ejecutar(connection -> {
            CallableStatement statement = connection.prepareCall("{call " + CAMBIAR_ESTADO + "(?, ?, ?)}");
            statement.setLong(1, perfilId);
            statement.setString(2, estado);
            statement.setDate(3, java.sql.Date.valueOf(fechaActual));
            return statement;
        }, List.of(new SqlParameter("PerfilId", Types.BIGINT), new SqlParameter("NuevoEstado", Types.VARCHAR),
                new SqlParameter("FechaActual", Types.DATE)));
    }

    @Override
    public void reemplazarPermisos(Long perfilId, List<Long> actividadIds, LocalDate fechaActual) {
        String actividadesJson = serializarActividades(actividadIds);
        ejecutar(connection -> {
            CallableStatement statement = connection.prepareCall("{call " + REEMPLAZAR_PERMISOS + "(?, ?, ?)}");
            statement.setLong(1, perfilId);
            statement.setNString(2, actividadesJson);
            statement.setDate(3, java.sql.Date.valueOf(fechaActual));
            return statement;
        }, List.of(new SqlParameter("PerfilId", Types.BIGINT), new SqlParameter("ActividadIdsJson", Types.NVARCHAR),
                new SqlParameter("FechaActual", Types.DATE)));
    }

    private String serializarActividades(List<Long> actividadIds) {
        try {
            return objectMapper.writeValueAsString(actividadIds);
        } catch (JsonProcessingException exception) {
            throw new SolicitudInvalidaException("No fue posible serializar las actividades.");
        }
    }

    private void ejecutar(org.springframework.jdbc.core.CallableStatementCreator creator, List<SqlParameter> parametros) {
        try { appJdbcTemplate.call(creator, parametros); }
        catch (DataAccessException exception) { throw traducir(exception); }
    }

    private RuntimeException traducir(DataAccessException exception) {
        Integer codigo = codigoSql(exception);
        if (codigo == null) return exception;
        return switch (codigo) {
            case 51102 -> new RecursoNoEncontradoException();
            case 51104, 2601, 2627 -> new RecursoDuplicadoException();
            case 51107 -> new EstadoIncompatibleException();
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
