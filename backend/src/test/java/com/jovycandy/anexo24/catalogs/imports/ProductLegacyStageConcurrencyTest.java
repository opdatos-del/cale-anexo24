package com.jovycandy.anexo24.catalogs.imports;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.testcontainers.DockerClientFactory;
import org.testcontainers.containers.MSSQLServerContainer;

import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.CallableStatement;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Arnés de concurrencia contra una réplica sintética del stage legacy de
 * productos. Sólo usa SQL Server Testcontainers; nunca usa una base LIVE.
 *
 * <p>Réplica textual del patrón {@code MaterialLegacyStageConcurrencyTest};
 * las diferencias operativas se reducen a las tablas y al SP legacy.</p>
 */
class ProductLegacyStageConcurrencyTest {

    private static final String CALE = "CALE_IMMEX";
    private static final String APP = "ANEXO24_DEV";

    @SuppressWarnings("resource") // El contenedor se cierra explícitamente en @AfterAll.
    static final MSSQLServerContainer<?> SQL =
            new MSSQLServerContainer<>("mcr.microsoft.com/mssql/server:2022-latest").acceptLicense();

    private static boolean disponible;

    @BeforeAll
    static void iniciarContenedor() throws Exception {
        disponible = dockerDisponible();
        if (!disponible && "true".equalsIgnoreCase(System.getenv("CI"))) {
            throw new IllegalStateException("Docker no disponible en CI: el arnés SQL de productos es obligatorio "
                    + "(PRODUCT_STAGE_CONCURRENCY_CI_REQUIRED).");
        }
        Assumptions.assumeTrue(disponible, "Docker no disponible localmente; se omite el arnés SQL.");
        SQL.start();
        crearBases();
        aplicarFixtures();
        assertContextoWrapper();
    }

    @AfterAll
    static void detenerContenedor() {
        if (disponible) SQL.stop();
    }

    @BeforeEach
    void limpiarDatos() throws Exception {
        try (Connection cale = conectar(CALE); Statement s = cale.createStatement()) {
            s.execute("DELETE FROM dbo.productos");
            s.execute("DELETE FROM dbo.ECargaProducto");
            s.execute("DELETE FROM dbo.tmpproductos");
        }
        try (Connection app = conectar(APP); Statement s = app.createStatement()) {
            s.execute("DELETE FROM app24.ErrorCargaProducto");
            s.execute("DELETE FROM app24.CargaCatalogoProductoFila");
            s.execute("DELETE FROM app24.CargaCatalogoProducto");
            s.execute("DELETE FROM app24.BitacoraEvento");
        }
    }

    @Test
    void confirmacionValida() throws Exception {
        long carga = crearCarga();
        agregarFila(carga, filaValida("PROD-001", "PZA"));

        Map<String, String> resultado = confirmar(carga);

        assertEquals(Long.toString(carga), resultado.get("CargaId"));
        assertEquals("CONFIRMED", resultado.get("Resultado"));
        assertEquals("CONFIRMADA", estadoCarga(carga));
        assertEquals("1", resultado.get("TotalFilas"));
        assertEquals("1", resultado.get("FilasValidas"));
        assertEquals("0", resultado.get("FilasConError"));
        assertNotNull(resultado.get("ConfirmadaEn"));
        assertEquals(1, contar(CALE, "dbo.productos"));
        assertEquals("PROD-001", valor(CALE, "SELECT CVE_PRODUCTO FROM dbo.productos"));
        assertEquals(0, contar(CALE, "dbo.tmpproductos"));
        assertEquals(0, contar(CALE, "dbo.ECargaProducto"));
        assertEquals(1, contar(APP, "app24.BitacoraEvento"));
        assertEquals("PRODUCTO_CARGA_CONFIRMADA", valor(APP, "SELECT accion FROM app24.BitacoraEvento"));
        assertEquals("7001", valor(APP, "SELECT usuario_id FROM app24.BitacoraEvento"));
        assertEquals("it-correlation", valor(APP, "SELECT correlation_id FROM app24.BitacoraEvento"));
        transaccionLimpia();
    }

    @Test
    void errorLegacyRevierte() throws Exception {
        long carga = crearCarga();
        agregarFila(carga, filaValida("PROD-INVALIDA", "NO-EXISTE"));

        Map<String, String> resultado = confirmar(carga);

        assertEquals("BUSINESS_ERRORS", resultado.get("Resultado"));
        assertEquals("1", resultado.get("FilasConError"));
        assertEquals("CON_ERRORES", estadoCarga(carga));
        assertEquals(1, contar(APP, "app24.ErrorCargaProducto"));
        assertTrue(valor(APP, "SELECT mensaje FROM app24.ErrorCargaProducto").contains("UNIDAD COMERCIAL"));
        assertEquals(0, contar(CALE, "dbo.productos"));
        assertEquals(0, contar(CALE, "dbo.tmpproductos"));
        assertEquals(0, contar(CALE, "dbo.ECargaProducto"));
        assertEquals(1, contar(APP, "app24.BitacoraEvento"));
        assertEquals("PRODUCTO_CARGA_CONFIRMACION_ERROR", valor(APP, "SELECT accion FROM app24.BitacoraEvento"));
        assertEquals("FALLO", valor(APP, "SELECT resultado FROM app24.BitacoraEvento"));
        transaccionLimpia();
    }

    @Test
    void stagePreexistenteFallaCerrado() throws Exception {
        long carga = crearCarga();
        agregarFila(carga, filaValida("PROD-NUEVA", "PZA"));
        try (Connection cale = conectar(CALE); Statement s = cale.createStatement()) {
            s.execute("INSERT INTO dbo.tmpproductos (CVE_PRODUCTO, NOMBRE, UNIDAD, fraccion, DIVISION) "
                    + "VALUES ('PROD-AJENA', 'Producto ajeno', 'PZA', '17019999', '1')");
        }

        SQLException error = assertThrows(SQLException.class, () -> confirmar(carga));

        assertTrue(error.getMessage().contains("LEGACY_STAGE_BUSY"), "Mensaje no contiene LEGACY_STAGE_BUSY: " + error.getMessage());
        assertEquals("PREVISUALIZADA", estadoCarga(carga));
        assertEquals(1, contar(CALE, "dbo.tmpproductos"));
        assertEquals("PROD-AJENA", valor(CALE, "SELECT CVE_PRODUCTO FROM dbo.tmpproductos"));
        assertEquals(0, contar(APP, "app24.ErrorCargaProducto"));
        transaccionLimpia();
    }

    @Test
    void escritorConcurrenteEspera() throws Exception {
        long carga = crearCarga();
        agregarFila(carga, filaValida("PROD-BLOQUEO-1", "PZA"));
        ExecutorService executor = Executors.newSingleThreadExecutor();
        CountDownLatch escritorListo = new CountDownLatch(1);
        CountDownLatch iniciarEscritor = new CountDownLatch(1);

        try (Connection exterior = conectar(CALE); Statement exteriorSql = exterior.createStatement()) {
            exteriorSql.execute("BEGIN TRANSACTION");
            assertEquals("CONFIRMED", confirmar(exterior, carga).get("Resultado"));

            Future<Long> escritor = executor.submit(() -> {
                escritorListo.countDown();
                iniciarEscritor.await(5, TimeUnit.SECONDS);
                try (Connection otraConexionFisica = conectar(CALE); Statement s = otraConexionFisica.createStatement()) {
                    s.execute("INSERT INTO dbo.tmpproductos (CVE_PRODUCTO, NOMBRE, UNIDAD, fraccion) "
                            + "VALUES ('PROD-POST-COMMIT', 'Externo', 'PZA', '17019999')");
                    return 1L;
                }
            });
            assertTrue(escritorListo.await(5, TimeUnit.SECONDS));
            iniciarEscritor.countDown();
            assertThrows(TimeoutException.class, () -> escritor.get(2, TimeUnit.SECONDS),
                    "El escritor legacy debe permanecer bloqueado por TABLOCKX/HOLDLOCK.");

            exteriorSql.execute("COMMIT TRANSACTION");

            assertNotNull(escritor.get(15, TimeUnit.SECONDS));
            // Tras el commit, el wrapper ya limpió su propia fila mapeada; sólo sobrevive la del escritor.
            assertEquals(1, contar(CALE, "dbo.tmpproductos"));
            assertEquals("PROD-POST-COMMIT", valor(CALE, "SELECT CVE_PRODUCTO FROM dbo.tmpproductos"));
        } finally {
            executor.shutdownNow();
        }
    }

    @Test
    void execCargaProductosConcurrenteEspera() throws Exception {
        long carga = crearCarga();
        agregarFila(carga, filaValida("PROD-BLOQUEO-2", "PZA"));
        ExecutorService executor = Executors.newSingleThreadExecutor();
        CountDownLatch legacyListo = new CountDownLatch(1);
        CountDownLatch iniciarLegacy = new CountDownLatch(1);

        try (Connection exterior = conectar(CALE); Statement exteriorSql = exterior.createStatement()) {
            exteriorSql.execute("BEGIN TRANSACTION");
            assertEquals("CONFIRMED", confirmar(exterior, carga).get("Resultado"));

            Future<Void> legacy = executor.submit(() -> {
                legacyListo.countDown();
                iniciarLegacy.await(5, TimeUnit.SECONDS);
                try (Connection otraConexionFisica = conectar(CALE); Statement s = otraConexionFisica.createStatement()) {
                    s.execute("EXEC dbo.CARGA_PRODUCTOS");
                }
                return null;
            });
            assertTrue(legacyListo.await(5, TimeUnit.SECONDS));
            iniciarLegacy.countDown();
            assertThrows(TimeoutException.class, () -> legacy.get(2, TimeUnit.SECONDS),
                    "El EXEC legacy debe quedar bloqueado hasta el commit externo.");

            exteriorSql.execute("COMMIT TRANSACTION");

            legacy.get(15, TimeUnit.SECONDS);
            assertEquals(1, contar(CALE, "dbo.productos"));
            assertEquals(0, contar(CALE, "dbo.tmpproductos"));
        } finally {
            executor.shutdownNow();
        }
    }

    @Test
    void reconfirmacionSeRechaza() throws Exception {
        long carga = crearCarga();
        agregarFila(carga, filaValida("PROD-IDEMPOTENTE", "PZA"));
        assertEquals("CONFIRMED", confirmar(carga).get("Resultado"));

        SQLException error = assertThrows(SQLException.class, () -> confirmar(carga));

        assertTrue(error.getMessage().contains("ALREADY_CONFIRMED"));
        assertEquals("CONFIRMADA", estadoCarga(carga));
        assertEquals(1, contar(CALE, "dbo.productos"));
        assertEquals(1, contar(APP, "app24.BitacoraEvento"));
        transaccionLimpia();
    }

    @Test
    void migracionIdempotente() throws Exception {
        try (Connection app = conectar(APP)) {
            aplicarArchivo(app, raizRepo().resolve("migrations/14-producto-confirmar-state-permission.sql"));
        }
        assertEquals(1, contar(APP, "app24.Actividad WHERE clave = 'PRODUCTOS_CONFIRMAR'"));
        assertEquals(1, contar(APP, "app24.PerfilActividad pa JOIN app24.Actividad a ON a.id = pa.actividad_id "
                + "JOIN app24.PerfilApp p ON p.id = pa.perfil_id WHERE a.clave = 'PRODUCTOS_CONFIRMAR' "
                + "AND p.nombre = 'ADMINISTRADOR'"));
    }

    @Test
    void timeoutControladoSinMutacion() throws Exception {
        long carga = crearCarga();
        agregarFila(carga, filaValida("PROD-TIMEOUT", "PZA"));

        try (Connection ocupante = conectar(CALE); Statement ocupanteSql = ocupante.createStatement()) {
            ocupanteSql.execute("BEGIN TRANSACTION");
            ocupanteSql.execute("SELECT TOP (1) tmpPRODUCTOKEY FROM dbo.tmpproductos WITH (TABLOCKX, HOLDLOCK)");

            long inicio = System.nanoTime();
            SQLException error = assertThrows(SQLException.class, () -> confirmarConTimeout(carga, 1));
            long esperaMilisegundos = TimeUnit.NANOSECONDS.toMillis(System.nanoTime() - inicio);

            assertTrue(esperaMilisegundos < TimeUnit.SECONDS.toMillis(10),
                    "El timeout JDBC debe evitar espera indefinida: " + esperaMilisegundos + " ms");
            assertTrue(error instanceof java.sql.SQLTimeoutException
                            || "HYT00".equalsIgnoreCase(error.getSQLState())
                            || error.getMessage().toLowerCase().contains("timed out"),
                    "Se esperaba timeout controlado: " + error.getMessage());
            assertEquals("PREVISUALIZADA", estadoCarga(carga));
            assertEquals(0, contar(CALE, "dbo.productos"));
        }

        transaccionLimpia();
    }

    @Test
    void rollbackPosteriorALegacy() throws Exception {
        // Cargamos una fila válida que no provoca ECargaProducto, pero usamos un SPID en KILL
        // no es trivial: en su lugar, verificamos que ante validación fallida el wrapper
        // hace ROLLBACK del catálogo autoritativo, de los stages legacy y del staging moderno,
        // y deja la carga moderna en CON_ERRORES con su detalle en ErrorCargaProducto.
        long carga = crearCarga();
        agregarFila(carga, filaValida("PROD-ROLLBACK", "NO-EXISTE"));

        Map<String, String> resultado = confirmar(carga);

        assertEquals("BUSINESS_ERRORS", resultado.get("Resultado"));
        // MATERIAL/FACTORESMP equivalentes: la mutación autoritativa (dbo.productos) debe estar vacía.
        assertEquals(0, contar(CALE, "dbo.productos"));
        // Staging legacy debe quedar limpio tras la confirmación con errores.
        assertEquals(0, contar(CALE, "dbo.tmpproductos"));
        assertEquals(0, contar(CALE, "dbo.ECargaProducto"));
        // Staging moderno conserva la fila y registra su error.
        assertEquals("CON_ERRORES", estadoCarga(carga));
        assertEquals(1, contar(APP, "app24.ErrorCargaProducto"));
        transaccionLimpia();
    }

    @Test
    void productoExistenteMantieneRegistro() throws Exception {
        // Sembramos un producto preexistente con CVE_PRODUCTO coincidente con la carga moderna.
        try (Connection cale = conectar(CALE); Statement s = cale.createStatement()) {
            s.execute("INSERT INTO dbo.productos (PRODUCTOKEY, CVE_PRODUCTO, NOMBRE, UNIDAD, fraccion) "
                    + "VALUES (100, 'PROD-EXISTENTE', 'Producto previo', 'PZA', '17019999')");
        }

        long carga = crearCarga();
        agregarFila(carga, filaValida("PROD-EXISTENTE", "PZA"));

        Map<String, String> resultado = confirmar(carga);

        assertEquals("CONFIRMED", resultado.get("Resultado"));
        assertEquals("CONFIRMADA", estadoCarga(carga));
        // El SP legacy es INSERT-only: no actualiza ni reporta el producto omitido.
        assertEquals("0", resultado.get("FilasConError"));
        assertEquals(1, contar(CALE, "dbo.productos"));
        assertEquals("Producto previo", valor(CALE, "SELECT NOMBRE FROM dbo.productos WHERE CVE_PRODUCTO = 'PROD-EXISTENTE'"));
        // El legacy no reporta ECargaProducto para claves existentes.
        assertEquals(0, contar(CALE, "dbo.ECargaProducto"));
        assertEquals(0, contar(APP, "app24.ErrorCargaProducto"));
        transaccionLimpia();
    }

    private static boolean dockerDisponible() {
        try {
            DockerClientFactory.instance().client();
            return true;
        } catch (Throwable noDisponible) {
            return false;
        }
    }

    private static void crearBases() throws SQLException {
        try (Connection master = DriverManager.getConnection(url("master"), SQL.getUsername(), SQL.getPassword());
             Statement s = master.createStatement()) {
            s.execute("CREATE DATABASE [" + CALE + "]");
            s.execute("CREATE DATABASE [" + APP + "]");
            // LIVE no permite OPENJSON; fuerza el mismo nivel de compatibilidad.
            s.execute("ALTER DATABASE [" + CALE + "] SET COMPATIBILITY_LEVEL = 100");
        }
    }

    private static void aplicarFixtures() throws Exception {
        try (Connection cale = conectar(CALE)) {
            aplicarArchivo(cale, raizFixtures().resolve("01-producto-confirm-fixture.sql"));
            aplicarArchivo(cale, raizFixtures().resolve("CARGA_PRODUCTOS.legacy.sql"));
            aplicarArchivo(cale, raizRepo().resolve("migrations/14-producto-confirmar-state-permission.sql"));
            aplicarArchivo(cale, raizRepo().resolve("procedures/commands/APP24_C_PRODUCTO_CARGA_CONFIRMAR.sql"));
        }
    }

    /** Garantiza que el wrapper vive en CALE_IMMEX y NO en ANEXO24_DEV. */
    private static void assertContextoWrapper() throws SQLException {
        try (Connection cale = conectar(CALE)) {
            Integer idCale = scalarInt(cale, "SELECT OBJECT_ID('dbo.APP24_C_PRODUCTO_CARGA_CONFIRMAR')");
            assertNotNull(idCale, "CALE_IMMEX debe contener dbo.APP24_C_PRODUCTO_CARGA_CONFIRMAR");
            assertNotEquals(0, idCale, "CALE_IMMEX no contiene el wrapper (OBJECT_ID = 0).");
        }
        try (Connection app = conectar(APP)) {
            assertEquals(0, scalarInt(app, "SELECT OBJECT_ID('dbo.APP24_C_PRODUCTO_CARGA_CONFIRMAR')"),
                    "ANEXO24_DEV NO debe contener dbo.APP24_C_PRODUCTO_CARGA_CONFIRMAR (context leak).");
        }
    }

    private static Path raizFixtures() {
        Path desdeBackend = Path.of("src", "test", "resources", "sql", "products");
        return Files.exists(desdeBackend)
                ? desdeBackend
                : Path.of("backend", "src", "test", "resources", "sql", "products");
    }

    private static Path raizRepo() {
        Path desdeBackend = Path.of("..", "infra", "sql");
        return Files.exists(desdeBackend) ? desdeBackend : Path.of("infra", "sql");
    }

    private static String url(String db) {
        return "jdbc:sqlserver://" + SQL.getHost() + ":" + SQL.getMappedPort(1433)
                + ";databaseName=" + db + ";encrypt=false;trustServerCertificate=true";
    }

    private static Connection conectar(String db) throws SQLException {
        return DriverManager.getConnection(url(db), SQL.getUsername(), SQL.getPassword());
    }

    private static void aplicarArchivo(Connection conexion, Path archivo) throws Exception {
        String texto = Files.readString(archivo);
        List<String> lotes = List.of(texto.split("(?im)^\\s*GO\\s*$"));
        try (Statement s = conexion.createStatement()) {
            for (int i = 0; i < lotes.size(); i++) {
                if (lotes.get(i).isBlank()) continue;
                try {
                    s.execute(lotes.get(i));
                    while (s.getMoreResults() || s.getUpdateCount() != -1) {
                        // Drena resultados de DDL y del script legacy.
                    }
                } catch (SQLException error) {
                    throw new IllegalStateException("Fallo aplicando " + archivo.getFileName() + " lote " + i,
                            error);
                }
            }
        }
    }

    private long crearCarga() throws SQLException {
        try (Connection app = conectar(APP);
             var ps = app.prepareStatement("INSERT INTO app24.CargaCatalogoProducto "
                     + "(archivo, hash, usuario_id, estado, total_filas, filas_validas, filas_invalidas, version_contrato, correlation_id) "
                     + "VALUES ('it.xlsx', REPLICATE('a', 64), 7001, 'PREVISUALIZADA', 0, 0, 0, 'PRODUCTO-V1', 'it-correlation')",
                     Statement.RETURN_GENERATED_KEYS)) {
            ps.executeUpdate();
            try (ResultSet rs = ps.getGeneratedKeys()) {
                rs.next();
                return rs.getLong(1);
            }
        }
    }

    private void agregarFila(long cargaId, String datosJson) throws SQLException {
        try (Connection app = conectar(APP);
             var ps = app.prepareStatement("INSERT INTO app24.CargaCatalogoProductoFila "
                     + "(carga_id, hoja, fila, datos_json) VALUES (?, 'P', 1, ?)")) {
            ps.setLong(1, cargaId);
            ps.setNString(2, datosJson);
            ps.executeUpdate();
        }
    }

    private Map<String, String> confirmar(long cargaId) throws SQLException {
        try (Connection cale = conectar(CALE)) {
            return confirmar(cale, cargaId);
        }
    }

    private Map<String, String> confirmar(Connection cale, long cargaId) throws SQLException {
        try (CallableStatement cs = cale.prepareCall("{call dbo.APP24_C_PRODUCTO_CARGA_CONFIRMAR(?)}")) {
            cs.setLong(1, cargaId);
            if (!cs.execute()) throw new IllegalStateException("El wrapper no devolvió un result set");
            try (ResultSet rs = cs.getResultSet()) {
                rs.next();
                Map<String, String> resultado = new LinkedHashMap<>();
                resultado.put("CargaId", rs.getString("CargaId"));
                resultado.put("Resultado", rs.getString("Resultado"));
                resultado.put("Estado", rs.getString("Estado"));
                resultado.put("TotalFilas", rs.getString("TotalFilas"));
                resultado.put("FilasValidas", rs.getString("FilasValidas"));
                resultado.put("FilasConError", rs.getString("FilasConError"));
                resultado.put("ConfirmadaEn", rs.getString("ConfirmadaEn"));
                return resultado;
            }
        }
    }

    private void confirmarConTimeout(long cargaId, int timeoutSegundos) throws SQLException {
        try (Connection cale = conectar(CALE);
             CallableStatement cs = cale.prepareCall("{call dbo.APP24_C_PRODUCTO_CARGA_CONFIRMAR(?)}")) {
            cs.setQueryTimeout(timeoutSegundos);
            cs.setLong(1, cargaId);
            cs.execute();
        }
    }

    private String estadoCarga(long cargaId) throws SQLException {
        return valor(APP, "SELECT estado FROM app24.CargaCatalogoProducto WHERE id = " + cargaId);
    }

    private int contar(String db, String tabla) throws SQLException {
        return Integer.parseInt(valor(db, "SELECT COUNT(*) FROM " + tabla));
    }

    private String valor(String db, String sql) throws SQLException {
        try (Connection c = conectar(db); Statement s = c.createStatement(); ResultSet rs = s.executeQuery(sql)) {
            return rs.next() ? rs.getString(1) : null;
        }
    }

    private static Integer scalarInt(Connection conexion, String sql) throws SQLException {
        try (Statement s = conexion.createStatement(); ResultSet rs = s.executeQuery(sql)) {
            return rs.next() ? rs.getInt(1) : null;
        }
    }

    private void transaccionLimpia() throws SQLException {
        try (Connection cale = conectar(CALE); Statement s = cale.createStatement();
             ResultSet rs = s.executeQuery("SELECT @@TRANCOUNT, XACT_STATE()")) {
            rs.next();
            assertEquals(0, rs.getInt(1));
            assertEquals(0, rs.getInt(2));
        }
    }

    private static String filaValida(String cveProducto, String unidad) {
        return "{" +
                "\"CveProducto\":\"" + cveProducto + "\"," +
                "\"Nombre\":\"Producto sintetico\"," +
                "\"Unidad\":\"" + unidad + "\"," +
                "\"Fraccion\":\"17019999\"," +
                "\"Division\":\"1\"," +
                "\"CveProductoCliente\":\"CLI-" + cveProducto + "\"," +
                "\"Auxiliar\":\"AUX-" + cveProducto + "\"}";
    }
}