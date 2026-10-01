package fixtures.fail;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;

/** FAIL: SELECT directo contra una vista. */
public class ViewSelect {
    public void leer(JdbcTemplate jdbcTemplate, RowMapper<String> mapper) {
        jdbcTemplate.query("SELECT * FROM dbo.V_INFORMEDESCARGAS", mapper);
    }
}
