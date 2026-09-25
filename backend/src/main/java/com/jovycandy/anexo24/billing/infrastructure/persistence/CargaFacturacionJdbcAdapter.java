package com.jovycandy.anexo24.billing.infrastructure.persistence;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.jovycandy.anexo24.billing.domain.model.ArchivoFacturacion;
import com.jovycandy.anexo24.billing.domain.port.CargaFacturacionRepository;
import com.jovycandy.anexo24.shared.exception.RecursoDuplicadoException;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.dao.DataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.SqlOutParameter;
import org.springframework.jdbc.core.SqlParameter;
import org.springframework.stereotype.Repository;

import java.sql.CallableStatement;
import java.sql.SQLException;
import java.sql.Types;
import java.util.List;
import java.util.Map;

@Repository
public class CargaFacturacionJdbcAdapter implements CargaFacturacionRepository {
    static final String EXISTE = "app24.APP24_Q_FACTURACION_CARGA_POR_HASH";
    static final String CREAR = "app24.APP24_C_FACTURACION_CARGA_CREAR";
    private final JdbcTemplate appJdbcTemplate;
    private final ObjectMapper objectMapper = new ObjectMapper();

    public CargaFacturacionJdbcAdapter(@Qualifier("appJdbcTemplate") JdbcTemplate appJdbcTemplate) {
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
    public long save(ArchivoFacturacion archivo, long usuarioId, String correlationId) {
        try {
            String errors = objectMapper.writeValueAsString(archivo.errores());
            Map<String, Object> result = appJdbcTemplate.call(connection -> {
                CallableStatement statement = connection.prepareCall("{call " + CREAR + "(?, ?, ?, ?, ?, ?, ?, ?, ?)}");
                statement.setString(1, archivo.nombre());
                statement.setString(2, archivo.hash());
                statement.setLong(3, usuarioId);
                statement.setString(4, archivo.errores().isEmpty() ? "PREVISUALIZADA" : "INVALIDA");
                statement.setInt(5, archivo.filas());
                statement.setInt(6, archivo.filas() - invalidRows(archivo));
                statement.setNString(7, errors);
                statement.setString(8, correlationId);
                statement.registerOutParameter(9, Types.BIGINT);
                return statement;
            }, List.of(new SqlParameter("Archivo", Types.VARCHAR), new SqlParameter("Hash", Types.VARCHAR),
                    new SqlParameter("UsuarioId", Types.BIGINT), new SqlParameter("Estado", Types.VARCHAR),
                    new SqlParameter("TotalRegistros", Types.INTEGER), new SqlParameter("RegistrosValidos", Types.INTEGER),
                    new SqlParameter("ErroresJson", Types.NVARCHAR), new SqlParameter("CorrelationId", Types.VARCHAR),
                    new SqlOutParameter("CargaId", Types.BIGINT)));
            Number id = (Number) result.get("CargaId");
            if (id == null || id.longValue() < 1) throw new IllegalStateException("El command no devolvió un ID válido");
            return id.longValue();
        } catch (JsonProcessingException e) {
            throw new IllegalStateException("No se pudieron serializar los errores de validación", e);
        } catch (DataAccessException e) {
            if (sqlCode(e) == 2601 || sqlCode(e) == 2627 || sqlCode(e) == 51104) throw new RecursoDuplicadoException();
            throw e;
        }
    }

    private int invalidRows(ArchivoFacturacion archivo) {
        if (archivo.errores().stream().anyMatch(error -> error.fila() == null)) return archivo.filas();
        return (int) archivo.errores().stream()
                .map(error -> error.hoja() + "\u0000" + error.fila()).distinct().count();
    }

    private Integer sqlCode(Throwable error) {
        for (Throwable current = error; current != null; current = current.getCause())
            if (current instanceof SQLException sql) return sql.getErrorCode();
        return null;
    }
}
