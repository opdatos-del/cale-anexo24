package fixture;

/**
 * Fixture sintético para el self-test del gate de permisos runtime.
 * No se compila: sólo se escanea como texto, igual que los adapters reales.
 */
class DemoAdapter {

    private static final String CALE_SP = "dbo.APP24_Q_DEMO_LISTAR";

    String procedimientoApp(boolean errores) {
        String procedure = errores ? "app24.APP24_Q_DEMO_ERRORES" : "app24.APP24_Q_DEMO_OBTENER";
        return procedure;
    }

    void invocar(java.sql.Connection conexion, boolean errores) throws Exception {
        conexion.prepareCall("{call " + CALE_SP + "(?)}");
        conexion.prepareCall("{call " + procedimientoApp(errores) + "(?)}");
    }
}
