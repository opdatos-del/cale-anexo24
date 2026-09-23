package com.jovycandy.anexo24.auditlog.infrastructure.persistence;

import com.jovycandy.anexo24.auditlog.domain.model.BitacoraEvento;
import com.jovycandy.anexo24.auditlog.domain.port.BitacoraEventoRepository;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.dao.DataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.SqlOutParameter;
import org.springframework.jdbc.core.SqlParameter;
import org.springframework.stereotype.Repository;

import java.sql.CallableStatement;
import java.sql.Types;
import java.util.List;
import java.util.Map;

/** Adaptador append-only de Bitácora mediante command almacenado app24. */
@Repository
public class BitacoraJdbcAdapter implements BitacoraEventoRepository {
    static final String PROCEDURE_NAME = "app24.APP24_C_BITACORA_REGISTRAR";
    private static final String EVENTO_ID = "EventoId";

    private final JdbcTemplate appJdbcTemplate;

    public BitacoraJdbcAdapter(@Qualifier("appJdbcTemplate") JdbcTemplate appJdbcTemplate) {
        this.appJdbcTemplate = appJdbcTemplate;
    }

    @Override
    public void registrar(BitacoraEvento evento) {
        try {
            Map<String, Object> resultado = appJdbcTemplate.call(connection -> {
                CallableStatement statement = connection.prepareCall(
                        "{call " + PROCEDURE_NAME + "(?, ?, ?, ?, ?, ?, ?)}");
                if (evento.usuarioId() == null) statement.setNull(1, Types.BIGINT);
                else statement.setLong(1, evento.usuarioId());
                statement.setString(2, evento.modulo().name());
                statement.setString(3, evento.accion().name());
                statement.setString(4, evento.detalle());
                statement.setString(5, evento.correlationId());
                statement.setString(6, evento.resultado().name());
                statement.registerOutParameter(7, Types.BIGINT);
                return statement;
            }, List.of(
                    new SqlParameter("UsuarioId", Types.BIGINT),
                    new SqlParameter("Modulo", Types.VARCHAR),
                    new SqlParameter("Accion", Types.VARCHAR),
                    new SqlParameter("Detalle", Types.VARCHAR),
                    new SqlParameter("CorrelacionId", Types.VARCHAR),
                    new SqlParameter("Resultado", Types.VARCHAR),
                    new SqlOutParameter(EVENTO_ID, Types.BIGINT)));
            Number id = (Number) resultado.get(EVENTO_ID);
            if (id == null || id.longValue() <= 0) {
                throw new IllegalStateException("El command de Bitácora no devolvió un ID válido");
            }
        } catch (DataAccessException exception) {
            throw exception;
        }
    }
}
