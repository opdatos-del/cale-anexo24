package com.jovycandy.anexo24.auditlog.infrastructure.persistence;

import com.jovycandy.anexo24.auditlog.domain.model.BitacoraAccion;
import com.jovycandy.anexo24.auditlog.domain.model.BitacoraModulo;
import com.jovycandy.anexo24.auditlog.domain.model.BitacoraRegistro;
import com.jovycandy.anexo24.auditlog.domain.model.BitacoraResultado;
import com.jovycandy.anexo24.auditlog.domain.port.BitacoraConsultaRepository;
import com.jovycandy.anexo24.shared.api.Pagina;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.core.SqlOutParameter;
import org.springframework.jdbc.core.SqlParameter;
import org.springframework.jdbc.core.SqlReturnResultSet;
import org.springframework.stereotype.Repository;

import java.sql.CallableStatement;
import java.sql.Types;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Map;

/** Adaptador read-only de Bitácora mediante consulta almacenada app24. */
@Repository
public class BitacoraConsultaJdbcAdapter implements BitacoraConsultaRepository {

    static final String PROCEDURE_NAME = "app24.APP24_Q_BITACORA_LISTAR";
    private static final String RESULTADO = "items";
    private static final String TOTAL = "Total";

    private static final RowMapper<BitacoraRegistro> MAPPER = (rs, rowNum) -> {
        LocalDateTime fecha = rs.getObject("fecha", LocalDateTime.class);
        return new BitacoraRegistro(
                rs.getLong("id"),
                fecha.toInstant(ZoneOffset.UTC),
                rs.getObject("usuario_id", Long.class),
                rs.getString("usuario"),
                BitacoraModulo.valueOf(rs.getString("modulo")),
                BitacoraAccion.valueOf(rs.getString("accion")),
                rs.getString("detalle"),
                BitacoraResultado.valueOf(rs.getString("resultado")),
                rs.getString("correlacion_id"));
    };

    private final JdbcTemplate appJdbcTemplate;

    public BitacoraConsultaJdbcAdapter(@Qualifier("appJdbcTemplate") JdbcTemplate appJdbcTemplate) {
        this.appJdbcTemplate = appJdbcTemplate;
    }

    @Override
    @SuppressWarnings("unchecked")
    public Pagina<BitacoraRegistro> findPage(
            Instant desde, Instant hasta, Long usuarioId, BitacoraModulo modulo,
            BitacoraResultado resultado, String correlationId, int pagina, int tamano) {
        Map<String, Object> salida = appJdbcTemplate.call(connection -> {
            CallableStatement statement = connection.prepareCall(
                    "{call " + PROCEDURE_NAME + "(?, ?, ?, ?, ?, ?, ?, ?, ?)}");
            statement.setObject(1, LocalDateTime.ofInstant(desde, ZoneOffset.UTC));
            statement.setObject(2, LocalDateTime.ofInstant(hasta, ZoneOffset.UTC));
            if (usuarioId == null) {
                statement.setNull(3, Types.BIGINT);
            } else {
                statement.setLong(3, usuarioId);
            }
            statement.setString(4, modulo == null ? null : modulo.name());
            statement.setString(5, resultado == null ? null : resultado.name());
            statement.setString(6, correlationId);
            statement.setInt(7, pagina);
            statement.setInt(8, tamano);
            statement.registerOutParameter(9, Types.BIGINT);
            return statement;
        }, List.of(
                new SqlParameter("Desde", Types.TIMESTAMP),
                new SqlParameter("Hasta", Types.TIMESTAMP),
                new SqlParameter("UsuarioId", Types.BIGINT),
                new SqlParameter("Modulo", Types.VARCHAR),
                new SqlParameter("Resultado", Types.VARCHAR),
                new SqlParameter("CorrelacionId", Types.VARCHAR),
                new SqlParameter("Pagina", Types.INTEGER),
                new SqlParameter("Tamano", Types.INTEGER),
                new SqlOutParameter(TOTAL, Types.BIGINT),
                new SqlReturnResultSet(RESULTADO, MAPPER)));
        List<BitacoraRegistro> items = (List<BitacoraRegistro>) salida.getOrDefault(RESULTADO, List.of());
        Number total = (Number) salida.get(TOTAL);
        return new Pagina<>(items, total == null ? 0L : total.longValue(), pagina, tamano);
    }
}
