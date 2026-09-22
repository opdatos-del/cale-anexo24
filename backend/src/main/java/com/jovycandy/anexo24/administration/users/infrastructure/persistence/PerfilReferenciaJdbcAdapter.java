package com.jovycandy.anexo24.administration.users.infrastructure.persistence;

import com.jovycandy.anexo24.administration.users.domain.port.PerfilReferenciaRepository;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/** Adaptador read-only de referencia de perfiles contra {@code app24}. */
@Repository
public class PerfilReferenciaJdbcAdapter implements PerfilReferenciaRepository {
    private final JdbcTemplate appJdbcTemplate;

    public PerfilReferenciaJdbcAdapter(@Qualifier("appJdbcTemplate") JdbcTemplate appJdbcTemplate) {
        this.appJdbcTemplate = appJdbcTemplate;
    }

    @Override
    public Optional<String> findEstadoById(Long perfilId) {
        List<String> estados = appJdbcTemplate.query(
                "SELECT estado FROM app24.PerfilApp WHERE id = ?",
                (rs, rowNum) -> rs.getString("estado"), perfilId);
        return estados.stream().findFirst();
    }
}
