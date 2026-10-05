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
 * Arnés de concurrencia contra una réplica sintética del stage legacy de agentes
 * aduanales. Sólo usa SQL Server Testcontainers; nunca usa una base LIVE.
 *
 * <p>Auditoría LIVE 2026-10-05: {@code dbo.TMPagentes} y {@code dbo.ECARGAagentes}
 * sólo son referenciadas por {@code dbo.CARGAAgentes} (1 módulo en sys.sql_modules),
 * por lo que NO son stage compartido. Aun así el wrapper las aísla con
 * TABLOCKX+HOLDLOCK y falla cerrado para cursorizar ejecuciones no coordinadas.</p>
 */
class AgentLegacyStageConcurrencyTest {

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
            throw new IllegalStateException("Docker no disponible en CI: el arnés SQL de agentes es obligatorio "
                    + "(AGENT_STAGE_CONCURRENCY_CI_REQUIRED).");
        }
        Assumptions.assumeTrue(disponible, "Docker no disponible localmente; se omite el arnés SQL.");
        SQL.start();
        crearBases();
        try (Connection cale = conectar(CALE)) {
            aplicarArchivo(cale, raizFixtures().resolve("01-agente-confirm-fixture.sql"));
            aplicarArchivo(cale, raizFixtures().resolve("CARGAAgentes.legacy.sql"));
        }
        aplicarArchivo(conectar(APP), raizRepo().resolve("migrations/17-agente-staging-confirmation-v1.sql"));
        aplicarArchivo(conectar(CALE), raizRepo().resolve("procedures/commands/APP24_C_AGENTE_CARGA_CONFIRMAR.sql"));
        assertContextoWrapper();
    }

    @AfterAll
    static void detenerContenedor() {
        if (disponible) SQL.stop();
    }

    @BeforeEach
    void limpiarDatos() throws Exception {
        try (Connection cale = conectar(CALE); Statement s = cale.createStatement()) {
            s.execute("DELETE FROM dbo.agentes");
            s.execute("DELETE FROM dbo.ECARGAagentes");
            s.execute("DELETE FROM dbo.TMPagentes");
        }
        try (Connection app = conectar(APP); Statement s = app.createStatement()) {
            s.execute("DELETE FROM app24.ErrorCargaAgente");
            s.execute("DELETE FROM app24.CargaCatalogoAgenteFila");
            s.execute("DELETE FROM app24.CargaCatalogoAgente");
            s.execute("DELETE FROM app24.BitacoraEvento");
        }
    }

    @Test
    void confirmacionValida() throws Exception {
        long carga = crearCarga();
        agregarFila(carga, filaValida("AG001", "3302"));

        Map<String, String> resultado = confirmar(carga);

        assertEquals(Long.toString(carga), resultado.get("CargaId"));
        assertEquals("CONFIRMED", resultado.get("Resultado"));
        assertEquals("CONFIRMADA", estadoCarga(carga));
        assertEquals("1", resultado.get("TotalFilas"));
        assertEquals("1", resultado.get("FilasValidas"));
        assertEquals("0", resultado.get("FilasConError"));
        assertNotNull(resultado.get("ConfirmadaEn"));
        assertEquals(1, contar(CALE, "dbo.agentes"));
        assertEquals("AG001", valor(CALE, "SELECT RTRIM(Clave) FROM dbo.agentes"));
        assertEquals(0, contar(CALE, "dbo.TMPagentes"));
        assertEquals(0, contar(CALE, "dbo.ECARGAagentes"));
        assertEquals(1, contar(APP, "app24.BitacoraEvento"));
        assertEquals("AGENTE_CARGA_CONFIRMADA", valor(APP, "SELECT accion FROM app24.BitacoraEvento"));
        transaccionLimpia();
    }

    @Test
    void errorLegacyRevierte() throws Exception {
        long carga = crearCarga();
        agregarFila(carga, filaValida("AG002", "123"));

        Map<String, String> resultado = confirmar(carga);

        assertEquals("BUSINESS_ERRORS", resultado.get("Resultado"));
        assertEquals("1", resultado.get("FilasConError"));
        assertEquals("CON_ERRORES", estadoCarga(carga));
        assertEquals(1, contar(APP, "app24.ErrorCargaAgente"));
        assertTrue(valor(APP, "SELECT mensaje FROM app24.ErrorCargaAgente").contains("patente"));
        assertEquals(0, contar(CALE, "dbo.agentes"));
        assertEquals(0, contar(CALE, "dbo.TMPagentes"));
        assertEquals(0, contar(CALE, "dbo.ECARGAagentes"));
        assertEquals(1, contar(APP, "app24.BitacoraEvento"));
        assertEquals("AGENTE_CARGA_CONFIRMACION_ERROR", valor(APP, "SELECT accion FROM app24.BitacoraEvento"));
        assertEquals("FALLO", valor(APP, "SELECT resultado FROM app24.BitacoraEvento"));
        transaccionLimpia();
    }

    @Test
    void stageOcupadoFallaCerrado() throws Exception {
        long carga = crearCarga();
        agregarFila(carga, filaValida("AG003", "3302"));
        try (Connection cale = conectar(CALE); Statement s = cale.createStatement()) {
            s.execute("INSERT INTO dbo.TMPagentes (Clave, Nombre, Patente) VALUES ('AGXXX', 'Ajeno', '9999')");
        }

        SQLException error = assertThrows(SQLException.class, () -> confirmar(carga));

        assertTrue(error.getMessage().contains("LEGACY_STAGE_BUSY"), "Mensaje no contiene LEGACY_STAGE_BUSY: " + error.getMessage());
        assertEquals("PREVISUALIZADA", estadoCarga(carga));
        assertEquals(1, contar(CALE, "dbo.TMPagentes"));
        assertEquals(0, contar(APP, "app24.ErrorCargaAgente"));
        assertEquals(0, contar(CALE, "dbo.agentes"));
        transaccionLimpia();
    }

    @Test
    void errorStageOcupadoFallaCerrado() throws Exception {
        long carga = crearCarga();
        agregarFila(carga, filaValida("AG004", "3302"));
        try (Connection cale = conectar(CALE); Statement s = cale.createStatement()) {
            s.execute("INSERT INTO dbo.ECARGAagentes (TMPKEY, ERROR, CLAVE) VALUES (999, 'error ajeno', 'AG-AJENA')");
        }

        SQLException error = assertThrows(SQLException.class, () -> confirmar(carga));

        assertTrue(error.getMessage().contains("LEGACY_STAGE_BUSY"), "Mensaje no contiene LEGACY_STAGE_BUSY: " + error.getMessage());
        assertEquals("PREVISUALIZADA", estadoCarga(carga));
        assertEquals(1, contar(CALE, "dbo.ECARGAagentes"));
        assertEquals(0, contar(CALE, "dbo.agentes"));
        transaccionLimpia();
    }

    @Test
    void escritorConcurrenteEspera() throws Exception {
        long carga = crearCarga();
        agregarFila(carga, filaValida("AG005", "3302"));
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
                    s.execute("INSERT INTO dbo.TMPagentes (Clave, Nombre, Patente) VALUES ('AGPOST', 'Externo', '9999')");
                    return 1L;
                }
            });
            assertTrue(escritorListo.await(5, TimeUnit.SECONDS));
            iniciarEscritor.countDown();
            assertThrows(TimeoutException.class, () -> escritor.get(2, TimeUnit.SECONDS),
                    "El escritor debe permanecer bloqueado por TABLOCKX/HOLDLOCK.");

            exteriorSql.execute("COMMIT TRANSACTION");

            assertNotNull(escritor.get(15, TimeUnit.SECONDS));
            assertEquals(1, contar(CALE, "dbo.TMPagentes"));
        } finally {
            executor.shutdownNow();
        }
    }

    @Test
    void execCargaAgentesConcurrenteEspera() throws Exception {
        long carga = crearCarga();
        agregarFila(carga, filaValida("AG006", "3302"));
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
                    s.execute("EXEC dbo.CARGAAgentes");
                }
                return null;
            });
            assertTrue(legacyListo.await(5, TimeUnit.SECONDS));
            iniciarLegacy.countDown();
            assertThrows(TimeoutException.class, () -> legacy.get(2, TimeUnit.SECONDS),
                    "El EXEC legacy debe quedar bloqueado hasta el commit externo.");

            exteriorSql.execute("COMMIT TRANSACTION");

            legacy.get(15, TimeUnit.SECONDS);
            assertEquals(1, contar(CALE, "dbo.agentes"));
            assertEquals(0, contar(CALE, "dbo.TMPagentes"));
        } finally {
            executor.shutdownNow();
        }
    }

    @Test
    void timeoutControladoSinMutacion() throws Exception {
        long carga = crearCarga();
        agregarFila(carga, filaValida("AG007", "3302"));

        try (Connection ocupante = conectar(CALE); Statement ocupanteSql = ocupante.createStatement()) {
            ocupanteSql.execute("BEGIN TRANSACTION");
            ocupanteSql.execute("SELECT TOP (1) AGENTEKEY FROM dbo.TMPagentes WITH (TABLOCKX, HOLDLOCK)");

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
            assertEquals(0, contar(CALE, "dbo.agentes"));
        }

        transaccionLimpia();
    }

    @Test
    void rollbackPosteriorALegacy() throws Exception {
        long carga = crearCarga();
        agregarFila(carga, filaValida("AG008", "12"));

        confirmar(carga);

        assertEquals(0, contar(CALE, "dbo.agentes"));
        assertEquals(0, contar(CALE, "dbo.TMPagentes"));
        assertEquals(0, contar(CALE, "dbo.ECARGAagentes"));
        transaccionLimpia();
    }

    @Test
    void reconfirmacionSeRechaza() throws Exception {
        long carga = crearCarga();
        agregarFila(carga, filaValida("AG009", "3302"));
        assertEquals("CONFIRMED", confirmar(carga).get("Resultado"));

        SQLException error = assertThrows(SQLException.class, () -> confirmar(carga));

        assertTrue(error.getMessage().contains("ALREADY_CONFIRMED"));
        assertEquals("CONFIRMADA", estadoCarga(carga));
        assertEquals(1, contar(CALE, "dbo.agentes"));
        assertEquals(1, contar(APP, "app24.BitacoraEvento"));
        transaccionLimpia();
    }

    @Test
    void migracionIdempotente() throws Exception {
        try (Connection app = conectar(APP)) {
            aplicarArchivo(app, raizRepo().resolve("migrations/17-agente-staging-confirmation-v1.sql"));
            aplicarArchivo(app, raizRepo().resolve("migrations/17-agente-staging-confirmation-v1.sql"));
        }
        assertEquals(1, contar(APP, "app24.Actividad WHERE clave = 'AGENTES_CONFIRMAR'"));
        assertEquals(1, contar(APP, "app24.Actividad WHERE clave = 'AGENTES_CARGAR'"));
        int total = Integer.parseInt(valor(APP,
                "SELECT COUNT(*) FROM app24.PerfilActividad pa JOIN app24.Actividad a ON a.id = pa.actividad_id "
                        + "JOIN app24.PerfilApp p ON p.id = pa.perfil_id WHERE a.clave IN ('AGENTES_CONFIRMAR','AGENTES_CARGAR') "
                        + "AND p.nombre = 'ADMINISTRADOR'"));
        assertEquals(2, total, "El ADMINISTRADOR debe tener AGENTES_CONFIRMAR y AGENTES_CARGAR asignadas");
    }

    @Test
    void agenteExistenteMantieneContratoLegacy() throws Exception {
        try (Connection cale = conectar(CALE); Statement s = cale.createStatement()) {
            s.execute("INSERT INTO dbo.agentes (Clave, Nombre, Patente) VALUES ('AG010', 'Agente previo', '1111')");
        }

        long carga = crearCarga();
        agregarFila(carga, filaValida("AG010", "3302"));

        Map<String, String> resultado = confirmar(carga);

        assertEquals("CONFIRMED", resultado.get("Resultado"));
        assertEquals("CONFIRMADA", estadoCarga(carga));
        assertEquals("0", resultado.get("FilasConError"));
        assertEquals(1, contar(CALE, "dbo.agentes"));
        assertEquals("Agente previo", valor(CALE, "SELECT RTRIM(Nombre) FROM dbo.agentes WHERE RTRIM(Clave) = 'AG010'"));
        assertEquals(0, contar(CALE, "dbo.ECARGAagentes"));
        assertEquals(0, contar(APP, "app24.ErrorCargaAgente"));
        transaccionLimpia();
    }

    @Test
    void longitudClaveBoundary() throws Exception {
        // Clave de longitud EXACTA 10: TMPagentes.Clave y agentes.Clave son CHAR(10).
        String clave10 = "A".repeat(10);
        long carga = crearCarga();
        agregarFila(carga, filaValida(clave10, "3302"));

        Map<String, String> resultado = confirmar(carga);

        assertEquals("CONFIRMED", resultado.get("Resultado"));
        assertEquals("CONFIRMADA", estadoCarga(carga));
        assertEquals(1, contar(CALE, "dbo.agentes"));
        assertEquals(clave10, valor(CALE, "SELECT RTRIM(Clave) FROM dbo.agentes WHERE RTRIM(Clave) = '" + clave10 + "'"));
        transaccionLimpia();
    }

    @Test
    void duplicadosEnArchivo() throws Exception {
        long carga = crearCarga();
        agregarFila(carga, filaValida("AG011", "3302"), 1);
        agregarFila(carga, filaValida("AG011", "3302"), 2);

        Map<String, String> resultado = confirmar(carga);

        assertEquals("BUSINESS_ERRORS", resultado.get("Resultado"));
        assertEquals("2", resultado.get("FilasConError"));
        assertEquals("CON_ERRORES", estadoCarga(carga));
        assertEquals(2, contar(APP, "app24.ErrorCargaAgente"));
        assertTrue(valor(APP, "SELECT mensaje FROM app24.ErrorCargaAgente").contains("DUPLICADAS"));
        assertEquals(0, contar(CALE, "dbo.agentes"));
        assertEquals(0, contar(CALE, "dbo.TMPagentes"));
        assertEquals(0, contar(CALE, "dbo.ECARGAagentes"));
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
        try (Connection app = conectar(APP); Statement s = app.createStatement()) {
            s.execute("IF SCHEMA_ID('app24') IS NULL EXEC('CREATE SCHEMA app24')");
            s.execute("IF OBJECT_ID('app24.PerfilApp', 'U') IS NULL CREATE TABLE app24.PerfilApp (id INT IDENTITY(1,1) NOT NULL PRIMARY KEY, nombre VARCHAR(100) NOT NULL UNIQUE)");
            s.execute("SET IDENTITY_INSERT app24.PerfilApp ON; INSERT INTO app24.PerfilApp (id, nombre) VALUES (1, 'ADMINISTRADOR'); SET IDENTITY_INSERT app24.PerfilApp OFF");
            s.execute("IF OBJECT_ID('app24.Actividad', 'U') IS NULL CREATE TABLE app24.Actividad (id INT IDENTITY(1,1) NOT NULL PRIMARY KEY, clave VARCHAR(100) NOT NULL UNIQUE, nombre VARCHAR(200) NOT NULL, recurso VARCHAR(100) NOT NULL, accion VARCHAR(100) NOT NULL)");
            s.execute("IF OBJECT_ID('app24.PerfilActividad', 'U') IS NULL CREATE TABLE app24.PerfilActividad (perfil_id INT NOT NULL, actividad_id INT NOT NULL, CONSTRAINT PK_PerfilActividad PRIMARY KEY (perfil_id, actividad_id))");
            s.execute("IF OBJECT_ID('app24.UsuarioApp', 'U') IS NULL CREATE TABLE app24.UsuarioApp (id BIGINT IDENTITY(1,1) NOT NULL PRIMARY KEY)");
            s.execute("SET IDENTITY_INSERT app24.UsuarioApp ON; INSERT INTO app24.UsuarioApp (id) VALUES (7001); SET IDENTITY_INSERT app24.UsuarioApp OFF");
            s.execute("IF OBJECT_ID('app24.BitacoraEvento', 'U') IS NULL CREATE TABLE app24.BitacoraEvento (id BIGINT IDENTITY(1,1) NOT NULL PRIMARY KEY, usuario_id BIGINT NOT NULL, modulo VARCHAR(100) NOT NULL, accion VARCHAR(80) NOT NULL, detalle VARCHAR(500) NOT NULL, correlation_id VARCHAR(100) NOT NULL, resultado VARCHAR(50) NOT NULL, fecha DATETIME2 NOT NULL DEFAULT SYSUTCDATETIME())");
            s.execute("IF OBJECT_ID('app24.APP24_C_BITACORA_REGISTRAR', 'P') IS NULL EXEC('CREATE PROCEDURE app24.APP24_C_BITACORA_REGISTRAR @UsuarioId BIGINT, @Modulo VARCHAR(100), @Accion VARCHAR(80), @Detalle VARCHAR(500), @CorrelacionId VARCHAR(100), @Resultado VARCHAR(50), @EventoId BIGINT OUTPUT AS BEGIN SET NOCOUNT ON; INSERT INTO app24.BitacoraEvento (usuario_id, modulo, accion, detalle, correlation_id, resultado) VALUES (@UsuarioId, @Modulo, @Accion, @Detalle, @CorrelacionId, @Resultado); SET @EventoId = SCOPE_IDENTITY(); END')");
        }
    }

    private static void assertContextoWrapper() throws SQLException {
        try (Connection cale = conectar(CALE)) {
            Integer idCale = scalarInt(cale, "SELECT OBJECT_ID('dbo.APP24_C_AGENTE_CARGA_CONFIRMAR')");
            assertNotNull(idCale);
            assertNotEquals(0, idCale, "CALE_IMMEX no contiene dbo.APP24_C_AGENTE_CARGA_CONFIRMAR.");
        }
        try (Connection app = conectar(APP)) {
            assertEquals(0, scalarInt(app, "SELECT OBJECT_ID('dbo.APP24_C_AGENTE_CARGA_CONFIRMAR')"),
                    "ANEXO24_DEV NO debe contener dbo.APP24_C_AGENTE_CARGA_CONFIRMAR (context leak).");
            assertNotEquals(0, scalarInt(app, "SELECT OBJECT_ID('app24.APP24_C_CATALOGO_AGENTE_CARGA_CREAR')"));
            assertNotEquals(0, scalarInt(app, "SELECT OBJECT_ID('app24.APP24_Q_CATALOGO_AGENTE_CARGA_OBTENER')"));
            assertNotEquals(0, scalarInt(app, "SELECT OBJECT_ID('app24.APP24_Q_CATALOGO_AGENTE_CARGA_ERRORES')"));
            assertNotEquals(0, scalarInt(app, "SELECT OBJECT_ID('app24.APP24_Q_CATALOGO_AGENTE_CARGA_POR_HASH')"));
        }
    }

    private static Path raizFixtures() {
        Path desdeBackend = Path.of("src", "test", "resources", "sql", "agents");
        return Files.exists(desdeBackend)
                ? desdeBackend
                : Path.of("backend", "src", "test", "resources", "sql", "agents");
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
             var ps = app.prepareStatement("INSERT INTO app24.CargaCatalogoAgente "
                     + "(archivo, hash, usuario_id, estado, total_filas, filas_validas, filas_invalidas, version_contrato, correlation_id) "
                     + "VALUES ('it.xlsx', REPLICATE('a', 64), 7001, 'PREVISUALIZADA', 0, 0, 0, 'AGENTE-V1', 'it-correlation')",
                     Statement.RETURN_GENERATED_KEYS)) {
            ps.executeUpdate();
            try (ResultSet rs = ps.getGeneratedKeys()) {
                rs.next();
                return rs.getLong(1);
            }
        }
    }

    private void agregarFila(long cargaId, String datosJson) throws SQLException {
        agregarFila(cargaId, datosJson, 1);
    }

    private void agregarFila(long cargaId, String datosJson, int fila) throws SQLException {
        try (Connection app = conectar(APP);
             var ps = app.prepareStatement("INSERT INTO app24.CargaCatalogoAgenteFila "
                     + "(carga_id, hoja, fila, datos_json) VALUES (?, 'P', ?, ?)")) {
            ps.setLong(1, cargaId);
            ps.setInt(2, fila);
            ps.setNString(3, datosJson);
            ps.executeUpdate();
        }
    }

    private Map<String, String> confirmar(long cargaId) throws SQLException {
        try (Connection cale = conectar(CALE)) {
            return confirmar(cale, cargaId);
        }
    }

    private Map<String, String> confirmar(Connection cale, long cargaId) throws SQLException {
        try (CallableStatement cs = cale.prepareCall("{call dbo.APP24_C_AGENTE_CARGA_CONFIRMAR(?)}")) {
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
             CallableStatement cs = cale.prepareCall("{call dbo.APP24_C_AGENTE_CARGA_CONFIRMAR(?)}")) {
            cs.setQueryTimeout(timeoutSegundos);
            cs.setLong(1, cargaId);
            cs.execute();
        }
    }

    private String estadoCarga(long cargaId) throws SQLException {
        return valor(APP, "SELECT estado FROM app24.CargaCatalogoAgente WHERE id = " + cargaId);
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

    private static String filaValida(String clave, String patente) {
        return "{" +
                "\"Clave\":\"" + clave + "\"," +
                "\"Nombre\":\"Agente sintetico\"," +
                "\"Domicilio\":\"Calle 1\"," +
                "\"Rfc\":\"AAC980101ABC\"," +
                "\"Patente\":\"" + patente + "\"}";
    }
}