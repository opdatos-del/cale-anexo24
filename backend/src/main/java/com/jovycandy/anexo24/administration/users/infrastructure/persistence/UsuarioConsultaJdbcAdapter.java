package com.jovycandy.anexo24.administration.users.infrastructure.persistence;

import com.jovycandy.anexo24.administration.users.domain.model.UsuarioAdministracion;
import com.jovycandy.anexo24.administration.users.domain.port.UsuarioConsultaRepository;
import com.jovycandy.anexo24.shared.api.Pagina;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Repository;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * Implementación read-only de la consulta administrativa de usuarios.
 *
 * <p>Usa exclusivamente {@code appJdbcTemplate} contra el esquema
 * {@code app24} de ANEXO24_DEV; nunca consulta CALE_IMMEX ni selecciona
 * {@code password_hash} (FASE 2).</p>
 */
@Repository
public class UsuarioConsultaJdbcAdapter implements UsuarioConsultaRepository {

    private static final String SELECT_BASE =
            "SELECT u.id, u.clave, u.nombre, u.correo, u.estado, u.vigencia, "
                    + "u.perfil_id, p.nombre AS perfil_nombre "
                    + "FROM app24.UsuarioApp u "
                    + "JOIN app24.PerfilApp p ON p.id = u.perfil_id";

    private static final String SELECT_COUNT =
            "SELECT COUNT(*) FROM app24.UsuarioApp u "
                    + "JOIN app24.PerfilApp p ON p.id = u.perfil_id";

    /** Mapea una fila de UsuarioApp JOIN PerfilApp a la proyección administrativa. */
    private static final RowMapper<UsuarioAdministracion> MAPPER = (rs, rowNum) ->
            new UsuarioAdministracion(
                    rs.getLong("id"),
                    rs.getString("clave"),
                    rs.getString("nombre"),
                    rs.getString("correo"),
                    rs.getString("estado"),
                    rs.getDate("vigencia") == null ? null : rs.getDate("vigencia").toLocalDate(),
                    rs.getLong("perfil_id"),
                    rs.getString("perfil_nombre"));

    private final JdbcTemplate appJdbcTemplate;

    /**
     * Constructor con la plantilla del esquema complementario.
     *
     * @param appJdbcTemplate plantilla JDBC de ANEXO24_DEV
     */
    public UsuarioConsultaJdbcAdapter(@Qualifier("appJdbcTemplate") JdbcTemplate appJdbcTemplate) {
        this.appJdbcTemplate = appJdbcTemplate;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Pagina<UsuarioAdministracion> findPage(
            String clave,
            String nombre,
            String correo,
            String estado,
            Long perfilId,
            int pagina,
            int tamano) {
        Filtros filtros = construirFiltros(clave, nombre, correo, estado, perfilId);

        long total = appJdbcTemplate.queryForObject(
                SELECT_COUNT + filtros.where(), Long.class, filtros.params());

        long offset = (long) (pagina - 1) * tamano;
        List<UsuarioAdministracion> items = appJdbcTemplate.query(
                SELECT_BASE + filtros.where()
                        + " ORDER BY u.clave ASC, u.id ASC OFFSET ? ROWS FETCH NEXT ? ROWS ONLY",
                MAPPER, appendParams(filtros.params(), offset, tamano));
        return new Pagina<>(items, total, pagina, tamano);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Optional<UsuarioAdministracion> findById(Long id) {
        List<UsuarioAdministracion> resultados = appJdbcTemplate.query(
                SELECT_BASE + " WHERE u.id = ?", MAPPER, id);
        return resultados.stream().findFirst();
    }

    /**
     * Construye la cláusula WHERE y sus parámetros a partir de los filtros.
     *
     * @param clave    filtro exacto opcional
     * @param nombre   filtro parcial opcional con comodines escapados
     * @param correo   filtro exacto opcional
     * @param estado   filtro exacto opcional
     * @param perfilId filtro exacto opcional
     * @return cláusula WHERE (vacía si no hay filtros) y parámetros ordenados
     */
    private Filtros construirFiltros(String clave, String nombre, String correo,
                                     String estado, Long perfilId) {
        List<String> condiciones = new ArrayList<>();
        List<Object> parametros = new ArrayList<>();
        if (clave != null) {
            condiciones.add("u.clave = ?");
            parametros.add(clave);
        }
        if (correo != null) {
            condiciones.add("u.correo = ?");
            parametros.add(correo);
        }
        if (perfilId != null) {
            condiciones.add("u.perfil_id = ?");
            parametros.add(perfilId);
        }
        if (estado != null) {
            condiciones.add("u.estado = ?");
            parametros.add(estado);
        }
        if (nombre != null) {
            condiciones.add("LOWER(u.nombre) LIKE LOWER(?) ESCAPE '\\'");
            parametros.add("%" + escaparComodines(nombre) + "%");
        }
        String where = condiciones.isEmpty() ? "" : " WHERE " + String.join(" AND ", condiciones);
        return new Filtros(where, parametros.toArray());
    }

    /**
     * Escapa comodines SQL del patrón parcial para busquedas literales.
     *
     * @param valor texto del filtro
     * @return texto con {@code \}, {@code %} y {@code _} escapados
     */
    private String escaparComodines(String valor) {
        return valor.replace("\\", "\\\\")
                .replace("%", "\\%")
                .replace("_", "\\_");
    }

    /**
     * Une los parámetros de filtro con los de paginación.
     *
     * @param base   parámetros de filtro
     * @param offset desplazamiento de página (aritmética long)
     * @param tamano tamaño de página
     * @return arreglo combinado de parámetros
     */
    private Object[] appendParams(Object[] base, long offset, int tamano) {
        Object[] resultado = new Object[base.length + 2];
        System.arraycopy(base, 0, resultado, 0, base.length);
        resultado[base.length] = offset;
        resultado[base.length + 1] = tamano;
        return resultado;
    }

    /** Cláusula WHERE y parámetros asociados de una consulta. */
    private record Filtros(String where, Object[] params) {
    }
}