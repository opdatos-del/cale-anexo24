package com.jovycandy.anexo24.catalogs.generaldata.infrastructure.persistence;

import com.jovycandy.anexo24.catalogs.generaldata.domain.model.DatosGenerales;
import com.jovycandy.anexo24.catalogs.generaldata.domain.port.DatosGeneralesRepository;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/** Adapter del procedimiento read-only de datos generales. */
@Repository
public class DatosGeneralesJdbcAdapter implements DatosGeneralesRepository {
    static final String PROCEDURE = "dbo.APP24_Q_DATOS_GENERALES_OBTENER";
    static final RowMapper<DatosGenerales> MAPPER = (rs, rowNum) -> new DatosGenerales(
            rs.getString("razon_social"),
            rs.getString("rfc"),
            rs.getString("registro_immex"),
            rs.getString("domicilio_fiscal"));
    private final JdbcTemplate jdbcTemplate;

    public DatosGeneralesJdbcAdapter(@Qualifier("jdbcTemplate") JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    @Override
    public Optional<DatosGenerales> find() {
        List<DatosGenerales> filas = jdbcTemplate.query(
                connection -> connection.prepareCall("{call " + PROCEDURE + "()}"), MAPPER);
        if (filas.size() > 1) {
            throw new IllegalStateException("La fuente DatosGenerales debe contener como máximo un registro.");
        }
        return filas.stream().findFirst();
    }
}
