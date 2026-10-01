package fixtures.fail.allowlist;

import org.springframework.jdbc.core.JdbcTemplate;

/**
 * FAIL: estar en un archivo allowlisted NO habilita SQL arbitrario.
 * checkDatabase("SELECT 1") está permitido; checkOther("SELECT COUNT(*) FROM dbo.usuarios") no.
 */
public class SystemStatusController {
    private String checkDatabase(JdbcTemplate jdbcTemplate) {
        jdbcTemplate.queryForObject("SELECT 1", Integer.class);
        return "UP";
    }

    private long checkOther(JdbcTemplate jdbcTemplate) {
        return jdbcTemplate.queryForObject("SELECT COUNT(*) FROM dbo.usuarios", Long.class);
    }
}
