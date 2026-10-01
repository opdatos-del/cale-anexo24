package fixtures.fail;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;

/** FAIL: SQL en text block de Java. */
public class TextBlockSql {
    public void leer(JdbcTemplate jdbcTemplate, RowMapper<String> mapper) {
        String sql = """
                SELECT materialkey, clave
                FROM dbo.material
                WHERE clave = ?
                """;
        jdbcTemplate.query(sql, mapper);
    }
}
