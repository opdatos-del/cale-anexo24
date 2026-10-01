package com.jovycandy.anexo24.operations.pediments.infrastructure.persistence;

import com.jovycandy.anexo24.operations.pediments.domain.model.ConfirmacionPedimento;
import com.jovycandy.anexo24.operations.pediments.domain.port.ConfirmacionPedimentoRepository;
import com.jovycandy.anexo24.shared.exception.ConfirmacionNoProcesableException;
import com.jovycandy.anexo24.shared.exception.EstadoIncompatibleException;
import com.jovycandy.anexo24.shared.exception.RecursoNoEncontradoException;
import com.jovycandy.anexo24.shared.exception.SolicitudInvalidaException;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.dao.DataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.SqlParameter;
import org.springframework.jdbc.core.SqlReturnResultSet;
import org.springframework.stereotype.Repository;

import java.sql.CallableStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.sql.Types;
import java.util.List;
import java.util.Map;

/**
 * Adaptador del command autoritativo en {@code CALE_IMMEX}.
 *
 * <p>Único acceso a datos: {@code dbo.APP24_C_PEDIMENTO_CONFIRMAR}. El SP coordina
 * internamente el estado de {@code ANEXO24_DEV}; Java no abre el otro datasource.</p>
 */
@Repository
public class ConfirmacionPedimentoJdbcAdapter implements ConfirmacionPedimentoRepository {

    static final String PROCEDIMIENTO = "dbo.APP24_C_PEDIMENTO_CONFIRMAR";
    private static final String RESULTADO = "confirmacion";

    private final JdbcTemplate jdbcTemplate;

    public ConfirmacionPedimentoJdbcAdapter(@Qualifier("jdbcTemplate") JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    @Override
    @SuppressWarnings("unchecked")
    public ConfirmacionPedimento confirmar(long cargaId, long usuarioId, String correlationId) {
        Map<String, Object> result;
        try {
            result = jdbcTemplate.call(connection -> {
                CallableStatement statement = connection.prepareCall("{call " + PROCEDIMIENTO + "(?, ?, ?)}");
                statement.setLong(1, cargaId);
                statement.setLong(2, usuarioId);
                statement.setString(3, correlationId);
                return statement;
            }, List.of(
                    new SqlParameter("CargaId", Types.BIGINT),
                    new SqlParameter("UsuarioId", Types.BIGINT),
                    new SqlParameter("CorrelacionId", Types.VARCHAR),
                    new SqlReturnResultSet(RESULTADO, (rs, row) -> mapear(rs))));
        } catch (DataAccessException exception) {
            throw traducir(exception);
        }
        List<ConfirmacionPedimento> filas = (List<ConfirmacionPedimento>) result.getOrDefault(RESULTADO, List.of());
        if (filas.isEmpty()) {
            throw new IllegalStateException("El command de confirmación no devolvió resultado.");
        }
        return filas.getFirst();
    }

    private ConfirmacionPedimento mapear(ResultSet rs) throws SQLException {
        Timestamp fecha = rs.getTimestamp("FechaConfirmacion");
        Integer tipo = rs.getObject("TipoOperacion", Integer.class);
        return new ConfirmacionPedimento(
                rs.getLong("CargaId"),
                rs.getString("Estado"),
                rs.getString("Resultado"),
                tipo,
                rs.getInt("OperacionesProcesadas"),
                rs.getInt("PartidasProcesadas"),
                fecha == null ? null : fecha.toLocalDateTime());
    }

    /** Traduce los errores controlados del SP a excepciones de la capa API. */
    static RuntimeException traducir(DataAccessException exception) {
        Integer codigo = codigoSql(exception);
        if (codigo == null) return exception;
        return switch (codigo) {
            case 51401 -> new SolicitudInvalidaException("La solicitud de confirmación no es válida.");
            case 51402, 51411, 51413 -> new EstadoIncompatibleException();
            case 51403 -> new RecursoNoEncontradoException();
            case 51404, 51405, 51406, 51407, 51408, 51410, 51412 -> new ConfirmacionNoProcesableException();
            default -> exception;
        };
    }

    private static Integer codigoSql(Throwable error) {
        for (Throwable actual = error; actual != null; actual = actual.getCause()) {
            if (actual instanceof SQLException sql) return sql.getErrorCode();
        }
        return null;
    }
}
