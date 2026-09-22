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
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.List;

/** Adaptador read-only de Bitácora contra {@code ANEXO24_DEV.app24}. */
@Repository
public class BitacoraConsultaJdbcAdapter implements BitacoraConsultaRepository {

    private static final String SELECT_REGISTROS = """
            SELECT
                b.id,
                b.fecha,
                b.usuario_id,
                u.clave AS usuario,
                b.modulo,
                b.accion,
                b.detalle,
                b.resultado,
                b.correlacion_id
            FROM app24.BitacoraEvento b
            LEFT JOIN app24.UsuarioApp u ON u.id = b.usuario_id
            """;
    private static final String SELECT_TOTAL = "SELECT COUNT(*) FROM app24.BitacoraEvento b";
    private static final String ORDEN_ESTABLE = " ORDER BY b.fecha DESC, b.id DESC";
    private static final String PAGINACION = " OFFSET ? ROWS FETCH NEXT ? ROWS ONLY";

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

    /**
     * Construye el adaptador con la conexión exclusiva del esquema de aplicación.
     *
     * @param appJdbcTemplate plantilla JDBC de ANEXO24_DEV
     */
    public BitacoraConsultaJdbcAdapter(@Qualifier("appJdbcTemplate") JdbcTemplate appJdbcTemplate) {
        this.appJdbcTemplate = appJdbcTemplate;
    }

    /** {@inheritDoc} */
    @Override
    public Pagina<BitacoraRegistro> findPage(
            Instant desde,
            Instant hasta,
            Long usuarioId,
            BitacoraModulo modulo,
            BitacoraResultado resultado,
            String correlationId,
            int pagina,
            int tamano) {
        List<Object> filtros = new ArrayList<>();
        String where = construirWhere(desde, hasta, usuarioId, modulo, resultado, correlationId, filtros);
        Object[] parametrosFiltro = filtros.toArray();

        Long total = appJdbcTemplate.queryForObject(
                SELECT_TOTAL + where,
                Long.class,
                parametrosFiltro);

        long offset = ((long) pagina - 1L) * tamano;
        List<Object> parametrosConsulta = new ArrayList<>(filtros);
        parametrosConsulta.add(offset);
        parametrosConsulta.add(tamano);
        List<BitacoraRegistro> items = appJdbcTemplate.query(
                SELECT_REGISTROS + where + ORDEN_ESTABLE + PAGINACION,
                MAPPER,
                parametrosConsulta.toArray());

        return new Pagina<>(items, total, pagina, tamano);
    }

    private String construirWhere(
            Instant desde,
            Instant hasta,
            Long usuarioId,
            BitacoraModulo modulo,
            BitacoraResultado resultado,
            String correlationId,
            List<Object> filtros) {
        StringBuilder where = new StringBuilder(" WHERE b.fecha >= ? AND b.fecha <= ?");
        filtros.add(LocalDateTime.ofInstant(desde, ZoneOffset.UTC));
        filtros.add(LocalDateTime.ofInstant(hasta, ZoneOffset.UTC));
        if (usuarioId != null) {
            where.append(" AND b.usuario_id = ?");
            filtros.add(usuarioId);
        }
        if (modulo != null) {
            where.append(" AND b.modulo = ?");
            filtros.add(modulo.name());
        }
        if (resultado != null) {
            where.append(" AND b.resultado = ?");
            filtros.add(resultado.name());
        }
        if (correlationId != null) {
            where.append(" AND b.correlacion_id = ?");
            filtros.add(correlationId);
        }
        return where.toString();
    }
}
