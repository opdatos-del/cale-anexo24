package com.jovycandy.anexo24.savedqueries.infrastructure.persistence;

import com.jovycandy.anexo24.savedqueries.domain.model.ConsultaGuardada;
import com.jovycandy.anexo24.savedqueries.domain.model.ConsultaGuardadaAlcance;
import com.jovycandy.anexo24.savedqueries.domain.port.ConsultaGuardadaRepository;
import com.jovycandy.anexo24.shared.exception.EstadoIncompatibleException;
import com.jovycandy.anexo24.shared.exception.RecursoDuplicadoException;
import com.jovycandy.anexo24.shared.exception.RecursoNoEncontradoException;
import com.jovycandy.anexo24.shared.exception.SolicitudInvalidaException;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.dao.DataAccessException;
import org.springframework.jdbc.core.CallableStatementCreator;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.core.SqlParameter;
import org.springframework.jdbc.core.SqlReturnResultSet;
import org.springframework.stereotype.Repository;

import java.sql.CallableStatement;
import java.sql.SQLException;
import java.sql.Types;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

/** Adaptador app24 que invoca sólo SP owner-scoped de consultas guardadas. */
@Repository
public class ConsultaGuardadaJdbcAdapter implements ConsultaGuardadaRepository {
    static final String LISTAR = "app24.APP24_Q_CONSULTAS_GUARDADAS_LISTAR";
    static final String CREAR = "app24.APP24_C_CONSULTA_GUARDADA_CREAR";
    static final String ACTUALIZAR = "app24.APP24_C_CONSULTA_GUARDADA_ACTUALIZAR";
    static final String ELIMINAR = "app24.APP24_C_CONSULTA_GUARDADA_ELIMINAR";
    private static final String ITEMS = "items";
    private static final RowMapper<ConsultaGuardada> MAPPER = (rs, row) -> new ConsultaGuardada(
            rs.getLong("id"), rs.getString("nombre"), rs.getString("descripcion"),
            ConsultaGuardadaAlcance.valueOf(rs.getString("alcance")), rs.getString("criterios_json"),
            rs.getTimestamp("fecha_creacion").toLocalDateTime(), rs.getTimestamp("fecha_actualizacion").toLocalDateTime());
    private final JdbcTemplate jdbc;

    public ConsultaGuardadaJdbcAdapter(@Qualifier("appJdbcTemplate") JdbcTemplate jdbc) { this.jdbc = jdbc; }

    @Override
    @SuppressWarnings("unchecked")
    public List<ConsultaGuardada> listar(Long usuarioId, ConsultaGuardadaAlcance alcance) {
        try {
            Map<String, Object> result = jdbc.call(connection -> {
                CallableStatement statement = connection.prepareCall("{call " + LISTAR + "(?, ?)}");
                statement.setLong(1, usuarioId);
                if (alcance == null) statement.setNull(2, Types.VARCHAR); else statement.setString(2, alcance.name());
                return statement;
            }, List.of(new SqlParameter("UsuarioId", Types.BIGINT), new SqlParameter("Alcance", Types.VARCHAR), new SqlReturnResultSet(ITEMS, MAPPER)));
            return (List<ConsultaGuardada>) result.getOrDefault(ITEMS, List.of());
        } catch (DataAccessException exception) { throw traducir(exception); }
    }

    @Override
    public ConsultaGuardada crear(Long usuarioId, String nombre, String descripcion, ConsultaGuardadaAlcance alcance, String criteriosJson) {
        return ejecutarEntidad(CREAR, Arrays.asList(usuarioId, nombre, descripcion, alcance.name(), criteriosJson));
    }

    @Override
    public ConsultaGuardada actualizar(Long id, Long usuarioId, String nombre, String descripcion, ConsultaGuardadaAlcance alcance, String criteriosJson) {
        return ejecutarEntidad(ACTUALIZAR, Arrays.asList(id, usuarioId, nombre, descripcion, alcance.name(), criteriosJson));
    }

    @Override
    public void eliminar(Long id, Long usuarioId) {
        ejecutar(ELIMINAR, List.of(id, usuarioId), false);
    }

    @SuppressWarnings("unchecked")
    private ConsultaGuardada ejecutarEntidad(String procedure, List<Object> values) {
        Map<String, Object> result = ejecutar(procedure, values, true);
        List<ConsultaGuardada> items = (List<ConsultaGuardada>) result.getOrDefault(ITEMS, List.of());
        if (items.size() != 1) throw new RecursoNoEncontradoException();
        return items.getFirst();
    }

    private Map<String, Object> ejecutar(String procedure, List<Object> values, boolean resultSet) {
        try {
            CallableStatementCreator creator = connection -> {
                CallableStatement statement = connection.prepareCall("{call " + procedure + "(" + "?,".repeat(values.size() - 1) + "?)}");
                for (int i = 0; i < values.size(); i++) {
                    Object value = values.get(i);
                    if (value == null) statement.setNull(i + 1, Types.NVARCHAR); else statement.setObject(i + 1, value);
                }
                return statement;
            };
            List<SqlParameter> parameters = new ArrayList<>();
            for (int i = 0; i < values.size(); i++) parameters.add(new SqlParameter("p" + i, Types.NVARCHAR));
            if (resultSet) parameters.add(new SqlReturnResultSet(ITEMS, MAPPER));
            return jdbc.call(creator, parameters);
        } catch (DataAccessException exception) { throw traducir(exception); }
    }

    private RuntimeException traducir(DataAccessException exception) {
        Integer code = codigoSql(exception);
        if (code == null) return exception;
        return switch (code) {
            case 51201 -> new SolicitudInvalidaException("Parámetros inválidos.");
            case 51202 -> new RecursoNoEncontradoException();
            case 51203, 2601, 2627 -> new RecursoDuplicadoException();
            case 51204 -> new EstadoIncompatibleException();
            default -> exception;
        };
    }

    private Integer codigoSql(Throwable error) {
        for (Throwable actual = error; actual != null; actual = actual.getCause()) {
            if (actual instanceof SQLException sql) return sql.getErrorCode();
        }
        return null;
    }
}
