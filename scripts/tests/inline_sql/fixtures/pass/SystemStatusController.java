package fixtures.pass;

import org.springframework.jdbc.core.JdbcTemplate;

/**
 * PASS: health-check técnico allowlisted (TECHNICAL_SQL_EXCEPTION_001).
 * SELECT 1 no es dato de negocio.
 */
public class SystemStatusController {
    private String checkDatabase(JdbcTemplate jdbcTemplate) {
        jdbcTemplate.queryForObject("SELECT 1", Integer.class);
        return "UP";
    }
}
