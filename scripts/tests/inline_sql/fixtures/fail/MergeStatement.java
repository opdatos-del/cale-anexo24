package fixtures.fail;

import org.springframework.jdbc.core.JdbcTemplate;

/** FAIL: MERGE inline. */
public class MergeStatement {
    public void fusionar(JdbcTemplate jdbcTemplate) {
        jdbcTemplate.execute("MERGE INTO dbo.saldos AS t USING #src AS s ON t.id = s.id WHEN MATCHED THEN UPDATE SET t.valor = s.valor");
    }
}
