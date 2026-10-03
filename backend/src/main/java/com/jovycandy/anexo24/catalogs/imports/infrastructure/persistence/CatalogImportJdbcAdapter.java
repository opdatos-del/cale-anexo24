package com.jovycandy.anexo24.catalogs.imports.infrastructure.persistence;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.jovycandy.anexo24.catalogs.imports.domain.model.CatalogImportArchivo;
import com.jovycandy.anexo24.catalogs.imports.domain.model.CatalogImportDetalle;
import com.jovycandy.anexo24.catalogs.imports.domain.model.CatalogImportError;
import com.jovycandy.anexo24.catalogs.imports.domain.model.CatalogImportFila;
import com.jovycandy.anexo24.catalogs.imports.domain.model.CatalogImportType;
import com.jovycandy.anexo24.catalogs.imports.domain.port.CatalogImportRepository;
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

/** Adapter JDBC que usa sólo SPs versionados de staging app24. */
@Repository
public class CatalogImportJdbcAdapter implements CatalogImportRepository {
    private final JdbcTemplate appJdbcTemplate;
    private final ObjectMapper objectMapper = new ObjectMapper();

    public CatalogImportJdbcAdapter(@Qualifier("appJdbcTemplate") JdbcTemplate appJdbcTemplate) {
        this.appJdbcTemplate = appJdbcTemplate;
    }

    @Override
    public boolean existsByHash(CatalogImportType type, String hash) {
        Map<String, Object> result = appJdbcTemplate.call(connection -> {
            CallableStatement statement = connection.prepareCall("{call " + procedureExists(type) + "(?, ?)}");
            statement.setString(1, hash);
            statement.registerOutParameter(2, Types.BIT);
            return statement;
        }, List.of(new SqlParameter("Hash", Types.VARCHAR), new SqlOutParameter("Existe", Types.BIT)));
        return Boolean.TRUE.equals(result.get("Existe"));
    }

    @Override
    public long save(CatalogImportArchivo archivo, long usuarioId, String correlationId) {
        try {
            String rows = objectMapper.writeValueAsString(archivo.filas().stream().map(row -> Map.of(
                    "hoja", row.hoja(), "fila", row.numero(), "datos", row.datos())).toList());
            String errors = objectMapper.writeValueAsString(archivo.errores());
            Map<String, Object> result = appJdbcTemplate.call(connection -> {
                CallableStatement statement = connection.prepareCall("{call " + procedureSave(archivo.tipo()) + "(?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)}");
                statement.setString(1, archivo.nombre());
                statement.setString(2, archivo.hash());
                statement.setLong(3, usuarioId);
                statement.setString(4, archivo.errores().isEmpty() ? "PREVISUALIZADA" : "CON_ERRORES");
                statement.setInt(5, archivo.totalFilas());
                statement.setInt(6, archivo.filasValidas());
                statement.setString(7, archivo.versionContrato());
                statement.setString(8, correlationId);
                statement.setNString(9, rows);
                statement.setNString(10, errors);
                statement.registerOutParameter(11, Types.BIGINT);
                return statement;
            }, List.of(new SqlParameter("Archivo", Types.VARCHAR), new SqlParameter("Hash", Types.VARCHAR),
                    new SqlParameter("UsuarioId", Types.BIGINT), new SqlParameter("Estado", Types.VARCHAR),
                    new SqlParameter("TotalFilas", Types.INTEGER), new SqlParameter("FilasValidas", Types.INTEGER),
                    new SqlParameter("VersionContrato", Types.VARCHAR), new SqlParameter("CorrelationId", Types.VARCHAR),
                    new SqlParameter("FilasJson", Types.NVARCHAR), new SqlParameter("ErroresJson", Types.NVARCHAR),
                    new SqlOutParameter("CargaId", Types.BIGINT)));
            Number id = (Number) result.get("CargaId");
            if (id == null || id.longValue() < 1) throw new IllegalStateException("El command no devolvió un ID válido");
            return id.longValue();
        } catch (JsonProcessingException exception) {
            throw new IllegalStateException("No se pudieron serializar las filas de staging", exception);
        } catch (DataAccessException exception) {
            if (sqlCode(exception) == 2601 || sqlCode(exception) == 2627) throw new RecursoDuplicadoException();
            throw exception;
        }
    }

    @Override
    @SuppressWarnings("unchecked")
    public Optional<CatalogImportDetalle> findById(CatalogImportType type, long id, int pagina, int tamano) {
        Map<String, Object> result = callDetail(type, id, pagina, tamano);
        List<Object[]> metadata = (List<Object[]>) result.getOrDefault("carga", List.of());
        if (metadata.isEmpty()) return Optional.empty();
        Object[] meta = metadata.getFirst();
        List<CatalogImportFila> rows = new ArrayList<>();
        for (Object[] row : (List<Object[]>) result.getOrDefault("filas", List.of()))
            rows.add(new CatalogImportFila((String) row[0], ((Number) row[1]).intValue(), readMap((String) row[2])));
        List<Long> totals = (List<Long>) result.getOrDefault("total", List.of());
        return Optional.of(new CatalogImportDetalle(((Number) meta[0]).longValue(), type, (String) meta[1],
                (String) meta[2], (String) meta[3], ((Number) meta[4]).intValue(), ((Number) meta[5]).intValue(),
                ((Number) meta[6]).intValue(), (String) meta[7], rows, totals.isEmpty() ? 0 : totals.getFirst(),
                (List<CatalogImportError>) result.getOrDefault("errores", List.of())));
    }

    @Override
    @SuppressWarnings("unchecked")
    public List<CatalogImportError> findErrors(CatalogImportType type, long id, int pagina, int tamano) {
        Map<String, Object> result = appJdbcTemplate.call(connection -> {
            CallableStatement statement = connection.prepareCall("{call " + procedureErrors(type) + "(?, ?, ?)}");
            statement.setLong(1, id); statement.setInt(2, pagina); statement.setInt(3, tamano); return statement;
        }, List.of(new SqlParameter("CargaId", Types.BIGINT), new SqlParameter("Pagina", Types.INTEGER),
                new SqlParameter("Tamano", Types.INTEGER), errorResultSet("errores")));
        return (List<CatalogImportError>) result.getOrDefault("errores", List.of());
    }

    @SuppressWarnings("unchecked")
    private Map<String, Object> callDetail(CatalogImportType type, long id, int pagina, int tamano) {
        return appJdbcTemplate.call(connection -> {
            CallableStatement statement = connection.prepareCall("{call " + procedureDetail(type) + "(?, ?, ?)}");
            statement.setLong(1, id); statement.setInt(2, pagina); statement.setInt(3, tamano); return statement;
        }, List.of(new SqlParameter("CargaId", Types.BIGINT), new SqlParameter("Pagina", Types.INTEGER),
                new SqlParameter("Tamano", Types.INTEGER),
                new SqlReturnResultSet("carga", (rs, n) -> new Object[]{rs.getLong("id"), rs.getString("archivo"),
                        rs.getString("hash"), rs.getString("estado"), rs.getInt("total_filas"),
                        rs.getInt("filas_validas"), rs.getInt("filas_invalidas"), rs.getString("version_contrato")} ),
                new SqlReturnResultSet("filas", (rs, n) -> new Object[]{rs.getString("hoja"), rs.getInt("fila"), rs.getString("datos_json")} ),
                new SqlReturnResultSet("total", (rs, n) -> rs.getLong("total_filas")), errorResultSet("errores")));
    }

    private SqlReturnResultSet errorResultSet(String name) {
        return new SqlReturnResultSet(name, (rs, n) -> new CatalogImportError(rs.getString("hoja"),
                (Integer) rs.getObject("fila"), rs.getString("columna"), rs.getString("valor_enmascarado"),
                rs.getString("codigo"), rs.getString("mensaje")));
    }

    private String procedureExists(CatalogImportType type) {
        return switch (type) {
            case MATERIAL -> "app24.APP24_Q_CATALOGO_MATERIAL_CARGA_POR_HASH";
            case PRODUCTO -> "app24.APP24_Q_CATALOGO_PRODUCTO_CARGA_POR_HASH";
            case CLIENTE -> "app24.APP24_Q_CATALOGO_CLIENTE_CARGA_POR_HASH";
            case PROVEEDOR -> "app24.APP24_Q_CATALOGO_PROVEEDOR_CARGA_POR_HASH";
        };
    }

    private String procedureSave(CatalogImportType type) {
        return switch (type) {
            case MATERIAL -> "app24.APP24_C_CATALOGO_MATERIAL_CARGA_CREAR";
            case PRODUCTO -> "app24.APP24_C_CATALOGO_PRODUCTO_CARGA_CREAR";
            case CLIENTE -> "app24.APP24_C_CATALOGO_CLIENTE_CARGA_CREAR";
            case PROVEEDOR -> "app24.APP24_C_CATALOGO_PROVEEDOR_CARGA_CREAR";
        };
    }

    private String procedureDetail(CatalogImportType type) {
        return switch (type) {
            case MATERIAL -> "app24.APP24_Q_CATALOGO_MATERIAL_CARGA_OBTENER";
            case PRODUCTO -> "app24.APP24_Q_CATALOGO_PRODUCTO_CARGA_OBTENER";
            case CLIENTE -> "app24.APP24_Q_CATALOGO_CLIENTE_CARGA_OBTENER";
            case PROVEEDOR -> "app24.APP24_Q_CATALOGO_PROVEEDOR_CARGA_OBTENER";
        };
    }

    private String procedureErrors(CatalogImportType type) {
        return switch (type) {
            case MATERIAL -> "app24.APP24_Q_CATALOGO_MATERIAL_CARGA_ERRORES";
            case PRODUCTO -> "app24.APP24_Q_CATALOGO_PRODUCTO_CARGA_ERRORES";
            case CLIENTE -> "app24.APP24_Q_CATALOGO_CLIENTE_CARGA_ERRORES";
            case PROVEEDOR -> "app24.APP24_Q_CATALOGO_PROVEEDOR_CARGA_ERRORES";
        };
    }

    @SuppressWarnings("unchecked")
    private Map<String, String> readMap(String json) {
        try { return objectMapper.readValue(json, LinkedHashMap.class); }
        catch (JsonProcessingException exception) { throw new IllegalStateException("La fila persistida no es JSON válido", exception); }
    }

    private Integer sqlCode(Throwable error) {
        for (Throwable current = error; current != null; current = current.getCause())
            if (current instanceof SQLException sql) return sql.getErrorCode();
        return null;
    }
}