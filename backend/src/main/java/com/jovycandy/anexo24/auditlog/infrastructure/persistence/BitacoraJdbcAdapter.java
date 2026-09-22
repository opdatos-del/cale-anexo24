package com.jovycandy.anexo24.auditlog.infrastructure.persistence;

import com.jovycandy.anexo24.auditlog.domain.model.BitacoraEvento;
import com.jovycandy.anexo24.auditlog.domain.port.BitacoraEventoRepository;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

/** Adaptador append-only de Bitácora contra {@code ANEXO24_DEV.app24}. */
@Repository
public class BitacoraJdbcAdapter implements BitacoraEventoRepository {

    private static final String INSERT_EVENTO = """
            INSERT INTO app24.BitacoraEvento
                (usuario_id, modulo, accion, detalle, correlacion_id, resultado)
            VALUES (?, ?, ?, ?, ?, ?)
            """;

    private final JdbcTemplate appJdbcTemplate;

    /**
     * Construye el adaptador con la conexión exclusiva del esquema de aplicación.
     *
     * @param appJdbcTemplate plantilla JDBC de ANEXO24_DEV
     */
    public BitacoraJdbcAdapter(@Qualifier("appJdbcTemplate") JdbcTemplate appJdbcTemplate) {
        this.appJdbcTemplate = appJdbcTemplate;
    }

    /**
     * {@inheritDoc}
     *
     * <p>No envía {@code id} ni {@code fecha}: SQL Server asigna IDENTITY y
     * {@code SYSUTCDATETIME()} respectivamente.</p>
     */
    @Override
    public void registrar(BitacoraEvento evento) {
        int filasAfectadas = appJdbcTemplate.update(
                INSERT_EVENTO,
                evento.usuarioId(),
                evento.modulo().name(),
                evento.accion().name(),
                evento.detalle(),
                evento.correlationId(),
                evento.resultado().name());
        if (filasAfectadas != 1) {
            throw new IllegalStateException("La inserción de Bitácora no afectó exactamente una fila");
        }
    }
}
