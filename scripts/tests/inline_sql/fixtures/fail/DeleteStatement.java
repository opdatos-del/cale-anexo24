package fixtures.fail;

/** FAIL: DELETE inline. */
public class DeleteStatement {
    public String borrar() {
        String sql = "DELETE FROM dbo.saldos WHERE clave = ?";
        return sql;
    }
}
