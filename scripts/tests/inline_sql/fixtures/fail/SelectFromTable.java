package fixtures.fail;

import org.springframework.jdbc.core.JdbcTemplate;

/** FAIL: SQL funcional inline y JDBC directo sobre una tabla. */
public class SelectFromTable {
    public long contar(JdbcTemplate jdbcTemplate) {
        return jdbcTemplate.queryForObject("SELECT COUNT(*) FROM dbo.material", Long.class);
    }
}
