package com.jovycandy.anexo24.reports.extended.infrastructure.persistence;

import com.jovycandy.anexo24.reports.extended.domain.model.Saldo;
import com.jovycandy.anexo24.reports.extended.domain.port.SaldoRepository;
import com.jovycandy.anexo24.shared.api.Pagina;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.core.SqlOutParameter;
import org.springframework.jdbc.core.SqlParameter;
import org.springframework.jdbc.core.SqlReturnResultSet;
import org.springframework.stereotype.Repository;

import java.sql.CallableStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.sql.Types;
import java.time.Instant;
import java.util.List;
import java.util.Map;

/**
 * Consume el SP wrapper read-only de saldos legacy (LEGACY-038). El wrapper
 * materializa el resultset de dbo.PR_INFORME_SALDOS en una tabla variable local
 * sin side effects persistentes; este adapter preserva el orden de columnas y
 * los nombres del contrato legacy.
 */
@Repository
public class SaldoStoredProcedureAdapter implements SaldoRepository {
    static final String PROCEDURE = "dbo.APP24_Q_SALDOS_LISTAR";

    private static final String ITEMS = "items";
    private static final String TOTAL = "Total";

    private static final RowMapper<Saldo> MAPPER = (rs, rowNum) -> new Saldo(
            rs.getString("Documento"),
            instantOrNull(rs, "Fecha de Pago"),
            rs.getString("Clave Pedimento"),
            rs.getString("Tipo de Operacion"),
            bigDecimalOrNull(rs, "tc"),
            rs.getString("Clave"),
            rs.getString("Descripcion"),
            rs.getString("Fraccion"),
            bigDecimalOrNull(rs, "Cant. Importado"),
            rs.getString("Unidad"),
            bigDecimalOrNull(rs, "Saldo"),
            bigDecimalOrNull(rs, "Valor Aduanal de Saldo"),
            bigDecimalOrNull(rs, "Valor dolares del saldo"),
            rs.getString("Pais origen"),
            integerOrNull(rs, "Temporalidad(Meses)"),
            rs.getString("Categoria"),
            instantOrNull(rs, "Fecha de Vencimiento"),
            rs.getString("PedimentoOriginal"),
            rs.getString("Descarga"),
            rs.getString("lote"),
            rs.getString("Complemento 1"),
            rs.getString("Complemento 2"),
            rs.getString("Complemento 3"),
            bigDecimalOrNull(rs, "Desperdiciado"),
            bigDecimalOrNull(rs, "Saldodesperdicio"),
            rs.getString("COVE"),
            rs.getString("Factura"),
            rs.getString("Tipo Material"),
            bigDecimalOrNull(rs, "pu_vad"),
            bigDecimalOrNull(rs, "pu_vdo"),
            bigDecimalOrNull(rs, "val_aduanal"),
            bigDecimalOrNull(rs, "val_dolares"),
            doubleOrNull(rs, "saldo en UMT"),
            rs.getString("unidadt"),
            doubleOrNull(rs, "valor en pesos"),
            doubleOrNull(rs, "Saldo en valor pesos"),
            rs.getString("NICO"));

    private static final List<SqlParameter> PARAMETERS = List.of(
            new SqlParameter("DESDE", Types.TIMESTAMP),
            new SqlParameter("HASTA", Types.TIMESTAMP),
            new SqlParameter("documento", Types.VARCHAR),
            new SqlParameter("Pagina", Types.INTEGER),
            new SqlParameter("Tamano", Types.INTEGER),
            new SqlOutParameter(TOTAL, Types.BIGINT),
            new SqlReturnResultSet(ITEMS, MAPPER));

    private final JdbcTemplate jdbcTemplate;

    public SaldoStoredProcedureAdapter(@Qualifier("jdbcTemplate") JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    @Override
    public Pagina<Saldo> findPage(Instant desde, Instant hasta, String documento,
                                  int pagina, int tamano) {
        Map<String, Object> result = jdbcTemplate.call(connection -> {
            CallableStatement statement = connection.prepareCall("{call " + PROCEDURE + "(?, ?, ?, ?, ?, ?)}");
            if (desde == null) statement.setNull(1, Types.TIMESTAMP);
            else statement.setTimestamp(1, Timestamp.from(desde));
            if (hasta == null) statement.setNull(2, Types.TIMESTAMP);
            else statement.setTimestamp(2, Timestamp.from(hasta));
            statement.setString(3, documento);
            statement.setInt(4, pagina);
            statement.setInt(5, tamano);
            statement.registerOutParameter(6, Types.BIGINT);
            return statement;
        }, PARAMETERS);
        @SuppressWarnings("unchecked")
        List<Saldo> items = (List<Saldo>) result.getOrDefault(ITEMS, List.of());
        Number total = (Number) result.get(TOTAL);
        return new Pagina<>(items, total == null ? 0 : total.longValue(), pagina, tamano);
    }

    private static Instant instantOrNull(ResultSet rs, String column) throws SQLException {
        Timestamp ts = rs.getTimestamp(column);
        return ts == null ? null : ts.toInstant();
    }

    private static java.math.BigDecimal bigDecimalOrNull(ResultSet rs, String column) throws SQLException {
        try {
            java.math.BigDecimal value = rs.getBigDecimal(column);
            if (value == null) return null;
            if (rs.wasNull()) return null;
            return value;
        } catch (SQLException ex) {
            // Para columnas FLOAT, rs.getBigDecimal puede fallar: leer como double y convertir.
            double d = rs.getDouble(column);
            if (rs.wasNull()) return null;
            return java.math.BigDecimal.valueOf(d);
        }
    }

    private static java.math.BigDecimal doubleOrNull(ResultSet rs, String column) throws SQLException {
        double d = rs.getDouble(column);
        if (rs.wasNull()) return null;
        return java.math.BigDecimal.valueOf(d);
    }

    private static Integer integerOrNull(ResultSet rs, String column) throws SQLException {
        int i = rs.getInt(column);
        if (rs.wasNull()) return null;
        return i;
    }
}
