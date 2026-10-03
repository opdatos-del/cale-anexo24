package com.jovycandy.anexo24.catalogs.imports.infrastructure.persistence;

import com.jovycandy.anexo24.catalogs.imports.domain.model.ConfirmacionCargaCliente;
import com.jovycandy.anexo24.catalogs.imports.domain.port.ConfirmacionCargaClienteRepository;
import com.jovycandy.anexo24.shared.exception.ConfirmacionNoProcesableException;
import com.jovycandy.anexo24.shared.exception.EstadoIncompatibleException;
import com.jovycandy.anexo24.shared.exception.RecursoNoEncontradoException;
import com.jovycandy.anexo24.shared.exception.SolicitudInvalidaException;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.dao.DataAccessException;
import org.springframework.dao.QueryTimeoutException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.SqlParameter;
import org.springframework.jdbc.core.SqlReturnResultSet;
import org.springframework.stereotype.Repository;

import java.sql.CallableStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.SQLTimeoutException;
import java.sql.Timestamp;
import java.sql.Types;
import java.util.List;
import java.util.Map;

/** Adaptador del command técnico de confirmación de clientes en {@code CALE_IMMEX}. */
@Repository
public class ConfirmacionCargaClienteJdbcAdapter implements ConfirmacionCargaClienteRepository {

    static final String PROCEDIMIENTO = "dbo.APP24_C_CLIENTE_CARGA_CONFIRMAR";
    private static final String RESULTADO = "confirmacion";
    private static final int TIMEOUT_CONFIRMACION_SEGUNDOS = 30;

    private final JdbcTemplate jdbcTemplate;

    public ConfirmacionCargaClienteJdbcAdapter(@Qualifier("jdbcTemplate") JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    @Override
    @SuppressWarnings("unchecked")
    public ConfirmacionCargaCliente confirmar(long cargaId) {
        Map<String, Object> resultado;
        try {
            resultado = jdbcTemplate.call(connection -> {
                CallableStatement statement = connection.prepareCall("{call " + PROCEDIMIENTO + "(?)}");
                // Evita solicitudes colgadas si WebForms retiene la etapa legacy global.
                statement.setQueryTimeout(TIMEOUT_CONFIRMACION_SEGUNDOS);
                statement.setLong(1, cargaId);
                return statement;
            }, List.of(
                    new SqlParameter("CargaId", Types.BIGINT),
                    new SqlReturnResultSet(RESULTADO, (rs, row) -> mapear(rs))));
        } catch (DataAccessException exception) {
            throw traducir(exception);
        }
        List<ConfirmacionCargaCliente> filas =
                (List<ConfirmacionCargaCliente>) resultado.getOrDefault(RESULTADO, List.of());
        if (filas.isEmpty()) throw new IllegalStateException("El command de confirmación no devolvió resultado.");
        return filas.getFirst();
    }

    private ConfirmacionCargaCliente mapear(ResultSet rs) throws SQLException {
        Timestamp confirmadaEn = rs.getTimestamp("ConfirmadaEn");
        return new ConfirmacionCargaCliente(rs.getLong("CargaId"), rs.getString("Estado"),
                rs.getInt("TotalFilas"), rs.getInt("FilasValidas"), rs.getInt("FilasConError"),
                confirmadaEn == null ? null : confirmadaEn.toLocalDateTime(), rs.getString("Resultado"));
    }

    /** Traduce los errores controlados del SP a la convención HTTP del proyecto. */
    static RuntimeException traducir(DataAccessException exception) {
        if (esTiempoAgotado(exception)) return new EstadoIncompatibleException();
        Integer codigo = codigoSql(exception);
        if (codigo == null) return exception;
        return switch (codigo) {
            case 51501 -> new SolicitudInvalidaException("La carga indicada no es válida.");
            case 51502, 51504 -> new EstadoIncompatibleException();
            case 51503 -> new RecursoNoEncontradoException();
            case 51505, 51506, 51507 -> new ConfirmacionNoProcesableException();
            default -> exception;
        };
    }

    private static boolean esTiempoAgotado(Throwable error) {
        for (Throwable actual = error; actual != null; actual = actual.getCause()) {
            if (actual instanceof QueryTimeoutException || actual instanceof SQLTimeoutException) return true;
            if (actual instanceof SQLException sql && "HYT00".equalsIgnoreCase(sql.getSQLState())) return true;
        }
        return false;
    }

    private static Integer codigoSql(Throwable error) {
        for (Throwable actual = error; actual != null; actual = actual.getCause()) {
            if (actual instanceof SQLException sql) return sql.getErrorCode();
        }
        return null;
    }
}