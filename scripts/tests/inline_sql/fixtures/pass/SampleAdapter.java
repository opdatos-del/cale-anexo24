package fixtures.pass;

import java.sql.CallableStatement;
import java.sql.Connection;
import java.sql.Types;
import java.util.List;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.SqlOutParameter;

/** PASS: sólo invoca SP; sin SQL funcional inline. */
public class SampleAdapter {
    private final JdbcTemplate jdbcTemplate;

    public SampleAdapter(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    /** Invoca el procedimiento versionado con plantilla {call ...}. */
    public void listar() {
        jdbcTemplate.call(connection -> {
            // comentario con SELECT y FROM que no debe contar
            CallableStatement statement = connection.prepareCall("{call dbo.APP24_Q_ALMACENES_LISTAR(?, ?, ?, ?)}");
            statement.registerOutParameter(4, Types.BIGINT);
            return statement;
        }, List.of(new SqlOutParameter("Total", Types.BIGINT)));
    }

    /** Crea la llamada sin SQL de negocio. */
    public CallableStatement preparar(Connection connection) throws Exception {
        return connection.prepareCall("{call dbo.APP24_Q_UNIDADES_LISTAR(?, ?, ?, ?)}");
    }
}
