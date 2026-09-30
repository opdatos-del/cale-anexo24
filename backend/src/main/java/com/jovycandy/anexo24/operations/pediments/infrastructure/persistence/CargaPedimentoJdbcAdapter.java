package com.jovycandy.anexo24.operations.pediments.infrastructure.persistence;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.jovycandy.anexo24.operations.pediments.domain.model.CargaPedimentoArchivo;
import com.jovycandy.anexo24.operations.pediments.domain.model.CargaPedimentoDetalle;
import com.jovycandy.anexo24.operations.pediments.domain.model.PedimentoError;
import com.jovycandy.anexo24.operations.pediments.domain.port.CargaPedimentoRepository;
import com.jovycandy.anexo24.shared.exception.RecursoDuplicadoException;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.dao.DataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.SqlOutParameter;
import org.springframework.jdbc.core.SqlParameter;
import org.springframework.jdbc.core.SqlReturnResultSet;
import org.springframework.stereotype.Repository;

import java.sql.CallableStatement;
import java.sql.SQLException;
import java.sql.Types;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/** Adapter que sólo usa SPs versionados de ANEXO24_DEV. */
@Repository
public class CargaPedimentoJdbcAdapter implements CargaPedimentoRepository {
    static final String EXISTE = "app24.APP24_Q_PEDIMENTO_CARGA_POR_HASH";
    static final String CREAR = "app24.APP24_C_PEDIMENTO_CARGA_CREAR";
    static final String OBTENER = "app24.APP24_Q_PEDIMENTO_CARGA_OBTENER";
    static final String ERRORES = "app24.APP24_Q_PEDIMENTO_CARGA_ERRORES";

    private final JdbcTemplate appJdbcTemplate;
    private final ObjectMapper objectMapper = new ObjectMapper();

    public CargaPedimentoJdbcAdapter(@Qualifier("appJdbcTemplate") JdbcTemplate appJdbcTemplate) {
        this.appJdbcTemplate = appJdbcTemplate;
    }

    @Override
    public boolean existsByHash(String hash) {
        Map<String, Object> result = appJdbcTemplate.call(connection -> {
            CallableStatement statement = connection.prepareCall("{call " + EXISTE + "(?, ?)}");
            statement.setString(1, hash);
            statement.registerOutParameter(2, Types.BIT);
            return statement;
        }, List.of(new SqlParameter("Hash", Types.VARCHAR), new SqlOutParameter("Existe", Types.BIT)));
        return Boolean.TRUE.equals(result.get("Existe"));
    }

    @Override
    public long save(CargaPedimentoArchivo archivo, long usuarioId, String correlationId) {
        final String filasJson;
        final String erroresJson;
        try {
            filasJson = filasJson(archivo);
            erroresJson = objectMapper.writeValueAsString(archivo.errores());
        } catch (JsonProcessingException exception) {
            throw new IllegalStateException("No se pudieron serializar las filas de staging", exception);
        }
        try {
            Map<String, Object> result = appJdbcTemplate.call(connection -> {
                CallableStatement statement = connection.prepareCall("{call " + CREAR + "(?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)}");
                statement.setString(1, archivo.nombre());
                statement.setString(2, archivo.hash());
                statement.setLong(3, usuarioId);
                statement.setString(4, archivo.errores().isEmpty() ? "PREVISUALIZADA" : "CON_ERRORES");
                statement.setInt(5, archivo.totalFilas());
                statement.setInt(6, archivo.filasValidas());
                statement.setString(7, archivo.versionPlantilla());
                statement.setString(8, correlationId);
                statement.setNString(9, filasJson);
                statement.setNString(10, erroresJson);
                statement.registerOutParameter(11, Types.BIGINT);
                return statement;
            }, List.of(
                    new SqlParameter("Archivo", Types.VARCHAR),
                    new SqlParameter("Hash", Types.VARCHAR),
                    new SqlParameter("UsuarioId", Types.BIGINT),
                    new SqlParameter("Estado", Types.VARCHAR),
                    new SqlParameter("TotalFilas", Types.INTEGER),
                    new SqlParameter("FilasValidas", Types.INTEGER),
                    new SqlParameter("VersionPlantilla", Types.VARCHAR),
                    new SqlParameter("CorrelationId", Types.VARCHAR),
                    new SqlParameter("FilasJson", Types.NVARCHAR),
                    new SqlParameter("ErroresJson", Types.NVARCHAR),
                    new SqlOutParameter("CargaId", Types.BIGINT)));
            Number id = (Number) result.get("CargaId");
            if (id == null || id.longValue() < 1) throw new IllegalStateException("El command no devolvió un ID válido");
            return id.longValue();
        } catch (DataAccessException exception) {
            if (sqlCode(exception) == 2601 || sqlCode(exception) == 2627) throw new RecursoDuplicadoException();
            throw exception;
        }
    }

    @Override
    @SuppressWarnings("unchecked")
    public Optional<CargaPedimentoDetalle> findById(long id, int pagina, int tamano) {
        Map<String, Object> result = appJdbcTemplate.call(connection -> {
            CallableStatement statement = connection.prepareCall("{call " + OBTENER + "(?, ?, ?)}");
            statement.setLong(1, id);
            statement.setInt(2, pagina);
            statement.setInt(3, tamano);
            return statement;
        }, List.of(
                new SqlParameter("CargaId", Types.BIGINT),
                new SqlParameter("Pagina", Types.INTEGER),
                new SqlParameter("Tamano", Types.INTEGER),
                new SqlReturnResultSet("carga", (rs, row) -> new Object[]{
                        rs.getLong("id"), rs.getString("archivo"), rs.getString("hash"), rs.getString("estado"),
                        rs.getInt("total_filas"), rs.getInt("filas_validas"), rs.getInt("filas_invalidas"),
                        rs.getString("version_plantilla"), rs.getString("correlation_id")} ),
                new SqlReturnResultSet("filas", (rs, row) -> new Object[]{
                        rs.getString("hoja"), rs.getInt("fila"), rs.getString("datos_json")} ),
                new SqlReturnResultSet("total", (rs, row) -> rs.getLong("total_filas")),
                errorResultSet("errores")));

        List<Object[]> carga = (List<Object[]>) result.getOrDefault("carga", List.of());
        if (carga.isEmpty()) return Optional.empty();
        Object[] meta = carga.getFirst();
        List<CargaPedimentoDetalle.Fila> filas = new ArrayList<>();
        for (Object[] row : (List<Object[]>) result.getOrDefault("filas", List.of()))
            filas.add(new CargaPedimentoDetalle.Fila((String) row[0], ((Number) row[1]).intValue(), readMap((String) row[2])));
        List<Long> totals = (List<Long>) result.getOrDefault("total", List.of());
        List<PedimentoError> errores = (List<PedimentoError>) result.getOrDefault("errores", List.of());
        return Optional.of(new CargaPedimentoDetalle(
                ((Number) meta[0]).longValue(), (String) meta[1], (String) meta[2], (String) meta[3],
                ((Number) meta[4]).intValue(), ((Number) meta[5]).intValue(), ((Number) meta[6]).intValue(),
                (String) meta[7], (String) meta[8], filas, totals.isEmpty() ? 0 : totals.getFirst(), errores));
    }

    @Override
    @SuppressWarnings("unchecked")
    public List<PedimentoError> findErrors(long id, int pagina, int tamano) {
        Map<String, Object> result = appJdbcTemplate.call(connection -> {
            CallableStatement statement = connection.prepareCall("{call " + ERRORES + "(?, ?, ?)}");
            statement.setLong(1, id);
            statement.setInt(2, pagina);
            statement.setInt(3, tamano);
            return statement;
        }, List.of(new SqlParameter("CargaId", Types.BIGINT), new SqlParameter("Pagina", Types.INTEGER),
                new SqlParameter("Tamano", Types.INTEGER), errorResultSet("errores")));
        return (List<PedimentoError>) result.getOrDefault("errores", List.of());
    }

    private SqlReturnResultSet errorResultSet(String name) {
        return new SqlReturnResultSet(name, (rs, row) -> new PedimentoError(
                rs.getString("hoja"), (Integer) rs.getObject("fila"), rs.getString("columna"),
                rs.getString("valor_enmascarado"), rs.getString("codigo"), rs.getString("mensaje")));
    }

    private String filasJson(CargaPedimentoArchivo archivo) throws JsonProcessingException {
        List<Map<String, Object>> rows = archivo.filas().stream().map(row -> {
            Map<String, Object> value = new LinkedHashMap<>();
            value.put("hoja", row.hoja());
            value.put("fila", row.numero());
            value.put("datos", row.datos());
            return value;
        }).toList();
        return objectMapper.writeValueAsString(rows);
    }

    @SuppressWarnings("unchecked")
    private Map<String, String> readMap(String json) {
        try {
            return objectMapper.readValue(json, LinkedHashMap.class);
        } catch (JsonProcessingException exception) {
            throw new IllegalStateException("La fila persistida no es JSON válido", exception);
        }
    }

    private Integer sqlCode(Throwable error) {
        for (Throwable current = error; current != null; current = current.getCause())
            if (current instanceof SQLException sql) return sql.getErrorCode();
        return null;
    }
}
