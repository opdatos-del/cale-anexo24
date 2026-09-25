package com.jovycandy.anexo24.billing.infrastructure.persistence;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.jovycandy.anexo24.billing.domain.model.ArchivoFacturacion;
import com.jovycandy.anexo24.billing.domain.model.CargaFacturacionDetalle;
import com.jovycandy.anexo24.billing.domain.port.CargaFacturacionRepository;
import com.jovycandy.anexo24.shared.exception.RecursoDuplicadoException;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.dao.DataAccessException;
import org.springframework.jdbc.core.*;
import org.springframework.stereotype.Repository;

import java.sql.CallableStatement;
import java.sql.SQLException;
import java.sql.Types;
import java.util.*;

@Repository
public class CargaFacturacionJdbcAdapter implements CargaFacturacionRepository {
    static final String EXISTE = "app24.APP24_Q_FACTURACION_CARGA_POR_HASH";
    static final String CREAR = "app24.APP24_C_FACTURACION_CARGA_CREAR";
    static final String OBTENER = "app24.APP24_Q_FACTURACION_CARGA_OBTENER";
    private final JdbcTemplate appJdbcTemplate;
    private final ObjectMapper objectMapper = new ObjectMapper();

    public CargaFacturacionJdbcAdapter(@Qualifier("appJdbcTemplate") JdbcTemplate appJdbcTemplate) {
        this.appJdbcTemplate = appJdbcTemplate;
    }

    @Override
    public boolean existsByHash(String hash) {
        Map<String, Object> result = appJdbcTemplate.call(connection -> {
            CallableStatement statement = connection.prepareCall("{call " + EXISTE + "(?, ?)}");
            statement.setString(1, hash); statement.registerOutParameter(2, Types.BIT); return statement;
        }, List.of(new SqlParameter("Hash", Types.VARCHAR), new SqlOutParameter("Existe", Types.BIT)));
        return Boolean.TRUE.equals(result.get("Existe"));
    }

    @Override
    public long save(ArchivoFacturacion archivo, long usuarioId, String correlationId) {
        return save(archivo, usuarioId, correlationId, "[]");
    }

    @Override
    public long save(ArchivoFacturacion archivo, long usuarioId, String correlationId, String filasJson) {
        try {
            String errors = objectMapper.writeValueAsString(archivo.errores());
            Map<String, Object> result = appJdbcTemplate.call(connection -> {
                CallableStatement statement = connection.prepareCall("{call " + CREAR + "(?, ?, ?, ?, ?, ?, ?, ?, ?, ?)}");
                statement.setString(1, archivo.nombre()); statement.setString(2, archivo.hash());
                statement.setLong(3, usuarioId); statement.setString(4, archivo.errores().isEmpty() ? "PREVISUALIZADA" : "INVALIDA");
                statement.setInt(5, archivo.filas()); statement.setInt(6, Math.max(0, archivo.filas() - invalidRows(archivo)));
                statement.setNString(7, errors); statement.setNString(8, filasJson); statement.setString(9, correlationId);
                statement.registerOutParameter(10, Types.BIGINT); return statement;
            }, List.of(new SqlParameter("Archivo", Types.VARCHAR), new SqlParameter("Hash", Types.VARCHAR),
                    new SqlParameter("UsuarioId", Types.BIGINT), new SqlParameter("Estado", Types.VARCHAR),
                    new SqlParameter("TotalRegistros", Types.INTEGER), new SqlParameter("RegistrosValidos", Types.INTEGER),
                    new SqlParameter("ErroresJson", Types.NVARCHAR), new SqlParameter("FilasJson", Types.NVARCHAR),
                    new SqlParameter("CorrelationId", Types.VARCHAR), new SqlOutParameter("CargaId", Types.BIGINT)));
            Number id = (Number) result.get("CargaId");
            if (id == null || id.longValue() < 1) throw new IllegalStateException("El command no devolvió un ID válido");
            return id.longValue();
        } catch (JsonProcessingException e) {
            throw new IllegalStateException("No se pudieron serializar los datos de staging", e);
        } catch (DataAccessException e) {
            if (sqlCode(e) == 2601 || sqlCode(e) == 2627 || sqlCode(e) == 51104) throw new RecursoDuplicadoException();
            throw e;
        }
    }

    @Override
    @SuppressWarnings("unchecked")
    public Optional<CargaFacturacionDetalle> findById(long id, int pagina, int tamano) {
        Map<String, Object> result = appJdbcTemplate.call(connection -> {
            CallableStatement statement = connection.prepareCall("{call " + OBTENER + "(?, ?, ?)}");
            statement.setLong(1, id); statement.setInt(2, pagina); statement.setInt(3, tamano); return statement;
        }, List.of(new SqlParameter("CargaId", Types.BIGINT), new SqlParameter("Pagina", Types.INTEGER),
                new SqlParameter("Tamano", Types.INTEGER),
                new SqlReturnResultSet("carga", (rs, n) -> new Object[]{rs.getLong("id"), rs.getString("archivo"), rs.getString("hash"),
                        rs.getString("estado"), rs.getInt("total_registros"), rs.getInt("registros_validos"), rs.getInt("registros_invalidos")} ),
                new SqlReturnResultSet("filas", (rs, n) -> new Object[]{rs.getString("hoja"), rs.getInt("fila"), rs.getString("datos_json")} ),
                new SqlReturnResultSet("total", (rs, n) -> rs.getLong("total_filas")),
                new SqlReturnResultSet("errores", (rs, n) -> new ArchivoFacturacion.Error(rs.getString("hoja"),
                        (Integer) rs.getObject("fila"), rs.getString("columna"), rs.getString("valor"), rs.getString("regla"), rs.getString("mensaje")))));
        List<Object[]> carga = (List<Object[]>) result.getOrDefault("carga", List.of());
        if (carga.isEmpty()) return Optional.empty();
        Object[] meta = carga.getFirst();
        List<CargaFacturacionDetalle.Fila> filas = new ArrayList<>();
        for (Object[] row : (List<Object[]>) result.getOrDefault("filas", List.of())) {
            try { filas.add(new CargaFacturacionDetalle.Fila((String) row[0], (Integer) row[1], objectMapper.readValue((String) row[2], LinkedHashMap.class))); }
            catch (Exception exception) { throw new IllegalStateException("La fila persistida no es JSON válido", exception); }
        }
        return Optional.of(new CargaFacturacionDetalle((Long) meta[0], (String) meta[1], (String) meta[2], (String) meta[3],
                (Integer) meta[4], (Integer) meta[5], (Integer) meta[6], filas,
                (List<ArchivoFacturacion.Error>) result.getOrDefault("errores", List.of())));
    }

    private int invalidRows(ArchivoFacturacion archivo) {
        if (archivo.errores().stream().anyMatch(error -> error.fila() == null)) return archivo.filas();
        return (int) archivo.errores().stream().map(error -> error.hoja() + "\u0000" + error.fila()).distinct().count();
    }
    private Integer sqlCode(Throwable error) {
        for (Throwable current = error; current != null; current = current.getCause()) if (current instanceof SQLException sql) return sql.getErrorCode();
        return null;
    }
}
