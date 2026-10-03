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
 * clientes. Sólo usa SQL Server Testcontainers; nunca usa una base LIVE.
 */
class ClientLegacyStageConcurrencyTest {

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
            throw new IllegalStateException("Docker no disponible en CI: el arnés SQL de clientes es obligatorio "
                    + "(CLIENT_STAGE_CONCURRENCY_CI_REQUIRED).");
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
            s.execute("DELETE FROM dbo.clientes");
            s.execute("DELETE FROM dbo.ECARGACLIENTES");
            s.execute("DELETE FROM dbo.TMPCLIENTES");
            s.execute("DELETE FROM dbo.ECARGAPROVEEDORES");
        }
        try (Connection app = conectar(APP); Statement s = app.createStatement()) {
            s.execute("DELETE FROM app24.ErrorCargaCliente");
            s.execute("DELETE FROM app24.CargaCatalogoClienteFila");
            s.execute("DELETE FROM app24.CargaCatalogoCliente");
            s.execute("DELETE FROM app24.BitacoraEvento");
        }
    }

    @Test
    void confirmacionValida() throws Exception {
        long carga = crearCarga();
        agregarFila(carga, filaValida("CLI-001"));

        Map<String, String> resultado = confirmar(carga);

        assertEquals(Long.toString(carga), resultado.get("CargaId"));
        assertEquals("CONFIRMED", resultado.get("Resultado"));
        assertEquals("CONFIRMADA", estadoCarga(carga));
        assertEquals("1", resultado.get("TotalFilas"));
        assertEquals("1", resultado.get("FilasValidas"));
        assertEquals("0", resultado.get("FilasConError"));
        assertNotNull(resultado.get("ConfirmadaEn"));
        assertEquals(1, contar(CALE, "dbo.clientes"));
        assertEquals("CLI-001", valor(CALE, "SELECT RTRIM(Clave) FROM dbo.clientes"));
        assertEquals(0, contar(CALE, "dbo.TMPCLIENTES"));
        assertEquals(0, contar(CALE, "dbo.ECARGACLIENTES"));
        assertEquals(1, contar(APP, "app24.BitacoraEvento"));
        assertEquals("CLIENTE_CARGA_CONFIRMADA", valor(APP, "SELECT accion FROM app24.BitacoraEvento"));
        transaccionLimpia();
    }

    @Test
    void errorLegacyRevierte() throws Exception {
        long carga = crearCarga();
        agregarFila(carga, filaInvalidaSinIdFiscal());

        Map<String, String> resultado = confirmar(carga);

        assertEquals("BUSINESS_ERRORS", resultado.get("Resultado"));
        assertEquals("1", resultado.get("FilasConError"));
        assertEquals("CON_ERRORES", estadoCarga(carga));
        assertEquals(1, contar(APP, "app24.ErrorCargaCliente"));
        assertTrue(valor(APP, "SELECT mensaje FROM app24.ErrorCargaCliente").contains("ID FISCAL"));
        assertEquals(0, contar(CALE, "dbo.clientes"));
        assertEquals(0, contar(CALE, "dbo.TMPCLIENTES"));
        assertEquals(0, contar(CALE, "dbo.ECARGACLIENTES"));
        assertEquals(1, contar(APP, "app24.BitacoraEvento"));
        assertEquals("CLIENTE_CARGA_CONFIRMACION_ERROR", valor(APP, "SELECT accion FROM app24.BitacoraEvento"));
        assertEquals("FALLO", valor(APP, "SELECT resultado FROM app24.BitacoraEvento"));
        transaccionLimpia();
    }

    @Test
    void stagePreexistenteFallaCerrado() throws Exception {
        long carga = crearCarga();
        agregarFila(carga, filaValida("CLI-NUEVA"));
        try (Connection cale = conectar(CALE); Statement s = cale.createStatement()) {
            s.execute("INSERT INTO dbo.TMPCLIENTES (Clave, Nombre, Idfiscal) VALUES ('CLI-AJENA', 'Cliente ajeno', 'XAXX010101000')");
        }

        SQLException error = assertThrows(SQLException.class, () -> confirmar(carga));

        assertTrue(error.getMessage().contains("LEGACY_STAGE_BUSY"), "Mensaje no contiene LEGACY_STAGE_BUSY: " + error.getMessage());
        assertEquals("PREVISUALIZADA", estadoCarga(carga));
        assertEquals(1, contar(CALE, "dbo.TMPCLIENTES"));
        assertEquals(0, contar(APP, "app24.ErrorCargaCliente"));
        transaccionLimpia();
    }

    @Test
    void escritorConcurrenteEspera() throws Exception {
        long carga = crearCarga();
        agregarFila(carga, filaValida("CLI-BLOQUEO-1"));
        ExecutorService executor = Executors.newSingleThreadExecutor();
        CountDownLatch escritorListo = new CountDownLatch(1);
        CountDownLatch iniciarEscritor = new CountDownLatch(1);

        try (Connection exterior = conectar(CALE); Statement exteriorSql = exterior.createStatement()) {
            exteriorSql.execute("BEGIN TRANSACTION");
            assertEquals("CONFIRMED", confirmar(exterior, carga).get("Resultado"));

            Future<Long> escritor = executor.submit(() -> {
                escritorListo.countDown();
                iniciarEscritor.await(5, TimeUnit.SECONDS);
                try (Connection otra = conectar(CALE); Statement s = otra.createStatement()) {
                    s.execute("INSERT INTO dbo.TMPCLIENTES (Clave, Nombre, Idfiscal) VALUES ('CLI-POST', 'Externo', 'XAXX010101000')");
                    return 1L;
                }
            });
            assertTrue(escritorListo.await(5, TimeUnit.SECONDS));
            iniciarEscritor.countDown();
            assertThrows(TimeoutException.class, () -> escritor.get(2, TimeUnit.SECONDS),
                    "El escritor legacy debe permanecer bloqueado por TABLOCKX/HOLDLOCK.");

            exteriorSql.execute("COMMIT TRANSACTION");

            assertNotNull(escritor.get(15, TimeUnit.SECONDS));
            assertEquals(1, contar(CALE, "dbo.TMPCLIENTES"));
        } finally {
            executor.shutdownNow();
        }
    }

    @Test
    void execCargaClientesConcurrenteEspera() throws Exception {
        long carga = crearCarga();
        agregarFila(carga, filaValida("CLI-BLOQUEO-2"));
        ExecutorService executor = Executors.newSingleThreadExecutor();
        CountDownLatch legacyListo = new CountDownLatch(1);
        CountDownLatch iniciarLegacy = new CountDownLatch(1);

        try (Connection exterior = conectar(CALE); Statement exteriorSql = exterior.createStatement()) {
            exteriorSql.execute("BEGIN TRANSACTION");
            assertEquals("CONFIRMED", confirmar(exterior, carga).get("Resultado"));

            Future<Void> legacy = executor.submit(() -> {
                legacyListo.countDown();
                iniciarLegacy.await(5, TimeUnit.SECONDS);
                try (Connection otra = conectar(CALE); Statement s = otra.createStatement()) {
                    s.execute("EXEC dbo.CARGACLIENTES");
                }
                return null;
            });
            assertTrue(legacyListo.await(5, TimeUnit.SECONDS));
            iniciarLegacy.countDown();
            assertThrows(TimeoutException.class, () -> legacy.get(2, TimeUnit.SECONDS),
                    "El EXEC legacy debe quedar bloqueado hasta el commit externo.");

            exteriorSql.execute("COMMIT TRANSACTION");

            legacy.get(15, TimeUnit.SECONDS);
            assertEquals(1, contar(CALE, "dbo.clientes"));
            assertEquals(0, contar(CALE, "dbo.TMPCLIENTES"));
        } finally {
            executor.shutdownNow();
        }
    }

    @Test
    void reconfirmacionSeRechaza() throws Exception {
        long carga = crearCarga();
        agregarFila(carga, filaValida("CLI-IDEMPOTENTE"));
        assertEquals("CONFIRMED", confirmar(carga).get("Resultado"));

        SQLException error = assertThrows(SQLException.class, () -> confirmar(carga));

        assertTrue(error.getMessage().contains("ALREADY_CONFIRMED"));
        assertEquals("CONFIRMADA", estadoCarga(carga));
        assertEquals(1, contar(CALE, "dbo.clientes"));
        assertEquals(1, contar(APP, "app24.BitacoraEvento"));
        transaccionLimpia();
    }

    @Test
    void migracionIdempotente() throws Exception {
        try (Connection app = conectar(APP)) {
            aplicarArchivo(app, raizRepo().resolve("migrations/15-cliente-confirmar-state-permission.sql"));
        }
        assertEquals(1, contar(APP, "app24.Actividad WHERE clave = 'CLIENTES_CONFIRMAR'"));
        assertEquals(1, contar(APP, "app24.PerfilActividad pa JOIN app24.Actividad a ON a.id = pa.actividad_id "
                + "JOIN app24.PerfilApp p ON p.id = pa.perfil_id WHERE a.clave = 'CLIENTES_CONFIRMAR' "
                + "AND p.nombre = 'ADMINISTRADOR'"));
    }

    @Test
    void timeoutControladoSinMutacion() throws Exception {
        long carga = crearCarga();
        agregarFila(carga, filaValida("CLI-TIMEOUT"));

        try (Connection ocupante = conectar(CALE); Statement ocupanteSql = ocupante.createStatement()) {
            ocupanteSql.execute("BEGIN TRANSACTION");
            ocupanteSql.execute("SELECT TOP (1) TMPCLIENTEKEY FROM dbo.TMPCLIENTES WITH (TABLOCKX, HOLDLOCK)");

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
            assertEquals(0, contar(CALE, "dbo.clientes"));
        }

        transaccionLimpia();
    }

    @Test
    void clienteExistenteMantieneRegistro() throws Exception {
        try (Connection cale = conectar(CALE); Statement s = cale.createStatement()) {
            s.execute("INSERT INTO dbo.clientes (Clave, Nombre, Idfiscal, clientekey) "
                    + "VALUES ('CLI-EXISTENTE', 'Cliente previo', 'XAXX010101000', 100)");
        }

        long carga = crearCarga();
        agregarFila(carga, filaValida("CLI-EXISTENTE"));

        Map<String, String> resultado = confirmar(carga);

        assertEquals("CONFIRMED", resultado.get("Resultado"));
        assertEquals("CONFIRMADA", estadoCarga(carga));
        // El SP legacy es INSERT-only: no actualiza ni reporta el cliente omitido.
        assertEquals("0", resultado.get("FilasConError"));
        assertEquals(1, contar(CALE, "dbo.clientes"));
        assertEquals("Cliente previo", valor(CALE, "SELECT Nombre FROM dbo.clientes WHERE RTRIM(Clave) = 'CLI-EXISTENTE'"));
        assertEquals(0, contar(CALE, "dbo.ECARGACLIENTES"));
        assertEquals(0, contar(APP, "app24.ErrorCargaCliente"));
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
            s.execute("ALTER DATABASE [" + CALE + "] SET COMPATIBILITY_LEVEL = 100");
        }
    }

    private static void aplicarFixtures() throws Exception {
        try (Connection cale = conectar(CALE)) {
            aplicarArchivo(cale, raizFixtures().resolve("01-cliente-confirm-fixture.sql"));
            aplicarArchivo(cale, raizFixtures().resolve("CARGACLIENTES.legacy.sql"));
            aplicarArchivo(cale, raizRepo().resolve("migrations/15-cliente-confirmar-state-permission.sql"));
            aplicarArchivo(cale, raizRepo().resolve("procedures/commands/APP24_C_CLIENTE_CARGA_CONFIRMAR.sql"));
        }
    }

    private static void assertContextoWrapper() throws SQLException {
        try (Connection cale = conectar(CALE)) {
            Integer idCale = scalarInt(cale, "SELECT OBJECT_ID('dbo.APP24_C_CLIENTE_CARGA_CONFIRMAR')");
            assertNotNull(idCale);
            assertNotEquals(0, idCale, "CALE_IMMEX no contiene dbo.APP24_C_CLIENTE_CARGA_CONFIRMAR.");
        }
        try (Connection app = conectar(APP)) {
            assertEquals(0, scalarInt(app, "SELECT OBJECT_ID('dbo.APP24_C_CLIENTE_CARGA_CONFIRMAR')"),
                    "ANEXO24_DEV NO debe contener dbo.APP24_C_CLIENTE_CARGA_CONFIRMAR (context leak).");
        }
    }

    private static Path raizFixtures() {
        Path desdeBackend = Path.of("src", "test", "resources", "sql", "clients");
        return Files.exists(desdeBackend)
                ? desdeBackend
                : Path.of("backend", "src", "test", "resources", "sql", "clients");
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
             var ps = app.prepareStatement("INSERT INTO app24.CargaCatalogoCliente "
                     + "(archivo, hash, usuario_id, estado, total_filas, filas_validas, filas_invalidas, version_contrato, correlation_id) "
                     + "VALUES ('it.xlsx', REPLICATE('a', 64), 7001, 'PREVISUALIZADA', 0, 0, 0, 'CLIENTE-V1', 'it-correlation')",
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
             var ps = app.prepareStatement("INSERT INTO app24.CargaCatalogoClienteFila "
                     + "(carga_id, hoja, fila, datos_json) VALUES (?, 'C', 1, ?)")) {
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
        try (CallableStatement cs = cale.prepareCall("{call dbo.APP24_C_CLIENTE_CARGA_CONFIRMAR(?)}")) {
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
             CallableStatement cs = cale.prepareCall("{call dbo.APP24_C_CLIENTE_CARGA_CONFIRMAR(?)}")) {
            cs.setQueryTimeout(timeoutSegundos);
            cs.setLong(1, cargaId);
            cs.execute();
        }
    }

    private String estadoCarga(long cargaId) throws SQLException {
        return valor(APP, "SELECT estado FROM app24.CargaCatalogoCliente WHERE id = " + cargaId);
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

    private static String filaValida(String clave) {
        return "{" +
                "\"Clave\":\"" + clave + "\"," +
                "\"Nombre\":\"Cliente sintetico\"," +
                "\"IdFiscal\":\"XAXX010101000\"," +
                "\"TipoNE\":\"01\"," +
                "\"Programa\":\"PRG\"," +
                "\"CalleNumero\":\"Av 1\"," +
                "\"Codigo\":\"0001\"," +
                "\"Colonia\":\"Centro\"," +
                "\"Entidad\":\"01\"," +
                "\"Pais\":\"MX\"," +
                "\"Telefono\":\"555\"," +
                "\"Correo\":\"c@c.c\"," +
                "\"Fax\":\"\"," +
                "\"ApellidoPaterno\":\"P\"," +
                "\"ApellidoMaterno\":\"M\"," +
                "\"Calle\":\"Av 1\"," +
                "\"CalleNumeroInterior\":\"0\"," +
                "\"Localidad\":\"CDMX\"," +
                "\"Referencia\":\"REF\"," +
                "\"Municipio\":\"MX\"," +
                "\"TipoIdentificador\":\"RFC\"," +
                "\"CodigoPostal\":\"01000\"}";
    }

    private static String filaInvalidaSinIdFiscal() {
        return "{" +
                "\"Clave\":\"CLI-INV\"," +
                "\"Nombre\":\"Cliente invalido\"," +
                "\"IdFiscal\":\"\"," +
                "\"TipoNE\":\"01\"," +
                "\"Programa\":\"PRG\"," +
                "\"CalleNumero\":\"Av 1\"," +
                "\"Codigo\":\"0001\"," +
                "\"Colonia\":\"Centro\"," +
                "\"Entidad\":\"01\"," +
                "\"Pais\":\"MX\"," +
                "\"Telefono\":\"555\"," +
                "\"Correo\":\"c@c.c\"," +
                "\"Fax\":\"\"," +
                "\"ApellidoPaterno\":\"P\"," +
                "\"ApellidoMaterno\":\"M\"," +
                "\"Calle\":\"Av 1\"," +
                "\"CalleNumeroInterior\":\"0\"," +
                "\"Localidad\":\"CDMX\"," +
                "\"Referencia\":\"REF\"," +
                "\"Municipio\":\"MX\"," +
                "\"TipoIdentificador\":\"\"," +
                "\"CodigoPostal\":\"01000\"}";
    }
}