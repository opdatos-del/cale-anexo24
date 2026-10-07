package com.jovycandy.anexo24.reports.extended.infrastructure.persistence;

import com.jovycandy.anexo24.reports.extended.domain.model.Anexo30Entrada;
import com.jovycandy.anexo24.reports.extended.domain.port.Anexo30EntradaRepository;
import com.jovycandy.anexo24.shared.api.Pagina;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.core.SqlOutParameter;
import org.springframework.jdbc.core.SqlParameter;
import org.springframework.jdbc.core.SqlReturnResultSet;
import org.springframework.stereotype.Repository;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.sql.Types;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

/** Consume el SP versionado read-only de entradas A31 (Revision Anexo 30). */
@Repository
public class Anexo30EntradaStoredProcedureAdapter implements Anexo30EntradaRepository {
    static final String PROCEDURE = "dbo.APP24_Q_ANEXO30_REVISION_ENTRADAS_LISTAR";

    private static final String ITEMS = "items";
    private static final String TOTAL = "Total";

    private static final RowMapper<Anexo30Entrada> MAPPER = (rs, rowNum) -> new Anexo30Entrada(
            rs.getLong("ENTRADA_KEY"),
            rs.getString("DESCARGA"),
            rs.getString("TIPO_OPERACION"),
            rs.getString("PEDIMENTO"),
            rs.getString("PEDIMENTO_ORIGINAL"),
            localDateTime(rs, "FECHA"),
            localDateTime(rs, "FECHA_ORIGINAL"),
            rs.getString("CLAVE_PEDIMENTO"),
            rs.getString("FRACCION"),
            rs.getBigDecimal("VALOR_COMERCIAL"),
            rs.getBigDecimal("IVA_FP21"),
            rs.getBigDecimal("IVA_FP22"),
            rs.getBigDecimal("SALDO"),
            longOrNull(rs, "OPERACION"),
            rs.getString("PARTIDA"),
            rs.getString("ESAF"));

    private static final List<SqlParameter> PARAMETERS = List.of(
            new SqlParameter("Filtro", Types.VARCHAR),
            new SqlParameter("Pagina", Types.INTEGER),
            new SqlParameter("Tamano", Types.INTEGER),
            new SqlOutParameter(TOTAL, Types.BIGINT),
            new SqlReturnResultSet(ITEMS, MAPPER));

    private final JdbcTemplate jdbcTemplate;

    public Anexo30EntradaStoredProcedureAdapter(@Qualifier("jdbcTemplate") JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    @Override
    public Pagina<Anexo30Entrada> findPage(String filtro, int pagina, int tamano) {
        Map<String, Object> result = jdbcTemplate.call(connection -> {
            java.sql.CallableStatement statement = connection.prepareCall("{call " + PROCEDURE + "(?, ?, ?, ?)}");
            statement.setString(1, filtro);
            statement.setInt(2, pagina);
            statement.setInt(3, tamano);
            statement.registerOutParameter(4, Types.BIGINT);
            return statement;
        }, PARAMETERS);
        @SuppressWarnings("unchecked")
        List<Anexo30Entrada> items = (List<Anexo30Entrada>) result.getOrDefault(ITEMS, List.of());
        Number total = (Number) result.get(TOTAL);
        return new Pagina<>(items, total == null ? 0 : total.longValue(), pagina, tamano);
    }

    private static LocalDateTime localDateTime(ResultSet rs, String column) throws SQLException {
        Timestamp timestamp = rs.getTimestamp(column);
        return timestamp == null ? null : timestamp.toLocalDateTime();
    }

    private static Long longOrNull(ResultSet rs, String column) throws SQLException {
        long value = rs.getLong(column);
        return rs.wasNull() ? null : value;
    }
}
