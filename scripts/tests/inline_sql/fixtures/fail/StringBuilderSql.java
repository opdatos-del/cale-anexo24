package fixtures.fail;

/** FAIL: SQL de negocio ensamblado con StringBuilder. */
public class StringBuilderSql {
    public String construir(String filtro) {
        StringBuilder sql = new StringBuilder();
        sql.append("DELETE FROM dbo.saldos WHERE clave = ");
        sql.append(filtro);
        return sql.toString();
    }
}
