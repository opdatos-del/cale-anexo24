package com.jovycandy.anexo24.administration.profiles.infrastructure.persistence;

import com.jovycandy.anexo24.administration.activities.domain.model.ActividadAdministracion;
import com.jovycandy.anexo24.administration.profiles.domain.model.PerfilPermisosDetalle;
import com.jovycandy.anexo24.administration.profiles.domain.port.PerfilPermisosConsultaRepository;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.core.SqlParameter;
import org.springframework.jdbc.core.SqlReturnResultSet;
import org.springframework.stereotype.Repository;

import java.sql.CallableStatement;
import java.sql.Types;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/** Adaptador read-only de permisos de perfil mediante el SP F5C de app24. */
@Repository
public class PerfilPermisosConsultaJdbcAdapter implements PerfilPermisosConsultaRepository {
    static final String PERFIL_PERMISOS_LISTAR = "app24.APP24_Q_PERFIL_PERMISOS_LISTAR";
    private static final String RESULTADO = "items";

    /** El SP devuelve una fila de perfil aun cuando actividad_id es NULL. */
    private static final RowMapper<FilaPermiso> PERMISOS_MAPPER = (rs, rowNum) -> new FilaPermiso(
            rs.getLong("perfil_id"), rs.getString("perfil_nombre"), rs.getString("perfil_estado"),
            rs.getObject("actividad_id", Long.class), rs.getString("clave"), rs.getString("nombre"),
            rs.getString("recurso"), rs.getString("accion"));

    private final JdbcTemplate appJdbcTemplate;

    public PerfilPermisosConsultaJdbcAdapter(@Qualifier("appJdbcTemplate") JdbcTemplate appJdbcTemplate) {
        this.appJdbcTemplate = appJdbcTemplate;
    }

    @Override
    @SuppressWarnings("unchecked")
    public Optional<PerfilPermisosDetalle> findPermissionsByProfileId(Long perfilId) {
        Map<String, Object> resultado = appJdbcTemplate.call(connection -> {
            CallableStatement statement = connection.prepareCall("{call " + PERFIL_PERMISOS_LISTAR + "(?)}");
            statement.setLong(1, perfilId);
            return statement;
        }, List.of(new SqlParameter("PerfilId", Types.BIGINT), new SqlReturnResultSet(RESULTADO, PERMISOS_MAPPER)));
        List<FilaPermiso> filas = (List<FilaPermiso>) resultado.getOrDefault(RESULTADO, List.of());
        if (filas.isEmpty()) return Optional.empty();

        FilaPermiso perfil = filas.getFirst();
        List<ActividadAdministracion> permisos = filas.stream().filter(fila -> fila.actividadId() != null)
                .map(fila -> new ActividadAdministracion(fila.actividadId(), fila.clave(), fila.nombre(),
                        fila.recurso(), fila.accion())).toList();
        return Optional.of(new PerfilPermisosDetalle(perfil.perfilId(), perfil.perfilNombre(), perfil.perfilEstado(), permisos));
    }

    private record FilaPermiso(Long perfilId, String perfilNombre, String perfilEstado, Long actividadId,
                               String clave, String nombre, String recurso, String accion) {
    }
}
