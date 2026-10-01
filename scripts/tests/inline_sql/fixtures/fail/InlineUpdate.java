package fixtures.fail;

import java.sql.Statement;

/** FAIL: write SQL inline desde Java. */
public class InlineUpdate {
    public void actualizar(Statement statement) throws Exception {
        statement.execute("UPDATE dbo.productos SET activo = 1");
    }
}
