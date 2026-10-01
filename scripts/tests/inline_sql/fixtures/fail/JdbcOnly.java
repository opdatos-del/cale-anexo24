package fixtures.fail;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;

/** FAIL: JDBC directo sin literal (query con SQL construido fuera). */
public class JdbcOnly {
    public void leer(JdbcTemplate jdbcTemplate, RowMapper<String> mapper) {
        jdbcTemplate.query("sqlDinamico", mapper);
    }
}
