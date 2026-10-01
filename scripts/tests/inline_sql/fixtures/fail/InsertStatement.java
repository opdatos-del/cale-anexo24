package fixtures.fail;

import org.springframework.jdbc.core.JdbcTemplate;

/** FAIL: INSERT inline. */
public class InsertStatement {
    public void insertar(JdbcTemplate jdbcTemplate) {
        jdbcTemplate.update("INSERT INTO dbo.proveedores (clave, nombre) VALUES (?, ?)", "P1", "Proveedor");
    }
}
