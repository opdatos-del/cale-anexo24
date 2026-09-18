package com.jovycandy.anexo24.catalogs.materials.infrastructure.persistence;

import com.jovycandy.anexo24.catalogs.materials.domain.model.Material;
import com.jovycandy.anexo24.catalogs.materials.domain.port.MaterialRepository;
import com.jovycandy.anexo24.shared.api.Pagina;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * Implementación del puerto de materiales contra el Módulo C.
 *
 * <p>Consultas parametrizadas (principio 2 del contrato de integración)
 * sobre {@code dbo.material} de CALE_IMMEX.</p>
 */
@Repository
public class MaterialJdbcAdapter implements MaterialRepository {

    private final JdbcTemplate jdbcTemplate;

    /** Mapea una fila de la tabla material a un objeto de dominio. */
    private static final RowMapper<Material> MAPPER = (rs, rowNum) -> new Material(
            rs.getBigDecimal("materialkey"),
            rs.getString("clave"),
            rs.getString("descripcion"),
            rs.getString("fraccion"),
            rs.getString("unidad"),
            rs.getString("unidadt"),
            rs.getString("tipomaterial"),
            rs.getString("tipo"),
            rs.getBigDecimal("FactorUM"),
            rs.getBigDecimal("IGIE"));

    /**
     * Constructor con la plantilla del Módulo C.
     *
     * @param jdbcTemplate plantilla JDBC de CALE_IMMEX
     */
    public MaterialJdbcAdapter(@Qualifier("jdbcTemplate") JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Pagina<Material> findPage(String filtro, int pagina, int tamano) {
        String where = "";
        Object[] params;
        if (filtro != null && !filtro.isBlank()) {
            where = " WHERE clave LIKE ? OR descripcion LIKE ? OR fraccion LIKE ?";
            String patron = "%" + filtro + "%";
            params = new Object[]{patron, patron, patron};
        } else {
            params = new Object[]{};
        }

        long total = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM dbo.material" + where, Long.class, params);

        int offset = (pagina - 1) * tamano;
        List<Material> items = jdbcTemplate.query(
                "SELECT materialkey, clave, descripcion, fraccion, unidad, unidadt, "
                        + "tipomaterial, tipo, FactorUM, IGIE FROM dbo.material"
                        + where + " ORDER BY clave, materialkey OFFSET ? ROWS FETCH NEXT ? ROWS ONLY",
                MAPPER, appendParams(params, offset, tamano));
        return new Pagina<>(items, total, pagina, tamano);
    }

    /**
     * Une los parámetros de filtro con los de paginación.
     *
     * @param base   parámetros de filtro
     * @param offset desplazamiento de página
     * @param tamano tamaño de página
     * @return arreglo combinado de parámetros
     */
    private Object[] appendParams(Object[] base, int offset, int tamano) {
        Object[] resultado = new Object[base.length + 2];
        System.arraycopy(base, 0, resultado, 0, base.length);
        resultado[base.length] = offset;
        resultado[base.length + 1] = tamano;
        return resultado;
    }
}