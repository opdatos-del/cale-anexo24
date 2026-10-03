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

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Arnés de concurrencia contra una réplica mínima y sintética de la etapa legacy
 * de materiales. Sólo usa SQL Server Testcontainers; nunca usa una base LIVE.
 */
class MaterialLegacyStageConcurrencyTest {

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
            throw new IllegalStateException("Docker no disponible en CI: el arnés SQL de materiales es obligatorio "
                    + "(MATERIAL_STAGE_CONCURRENCY_CI_REQUIRED).");
        }
        Assumptions.assumeTrue(disponible, "Docker no disponible localmente; se omite el arnés SQL.");
        SQL.start();
        crearBases();
        aplicarFixtures();
    }

    @AfterAll
    static void detenerContenedor() {
        if (disponible) SQL.stop();
    }

    @BeforeEach
    void limpiarDatos() throws Exception {
        try (Connection cale = conectar(CALE); Statement s = cale.createStatement()) {
            s.execute("DELETE FROM dbo.FACTORESMP");
            s.execute("DELETE FROM dbo.MATERIAL");
            s.execute("DELETE FROM dbo.ECARGAMATERIAL");
            s.execute("DELETE FROM dbo.CARGAMATERIAL");
            s.execute("DELETE FROM dbo.APP24_MaterialLegacyExecution");
        }
        try (Connection app = conectar(APP); Statement s = app.createStatement()) {
            s.execute("DELETE FROM app24.ErrorCargaMaterial");
            s.execute("DELETE FROM app24.CargaCatalogoMaterialFila");
            s.execute("DELETE FROM app24.CargaCatalogoMaterial");
            s.execute("DELETE FROM app24.BitacoraEvento");
        }
    }

    @Test
    void confirmaCargaValidaYLimpiaLaEtapaLegacy() throws Exception {
        long carga = crearCarga();
        agregarFila(carga, filaValida("MAT-0001", "KG"));

        Map<String, String> resultado = confirmar(carga);

        assertEquals(Long.toString(carga), resultado.get("CargaId"));
        assertEquals("CONFIRMED", resultado.get("Resultado"));
        assertEquals("CONFIRMADA", estadoCarga(carga));
        assertEquals("1", resultado.get("TotalFilas"));
        assertEquals("1", resultado.get("FilasValidas"));
        assertEquals("0", resultado.get("FilasConError"));
        assertNotNull(resultado.get("ConfirmadaEn"));
        assertEquals(1, contar(CALE, "dbo.MATERIAL"));
        assertEquals("MAT-0001", valor(CALE, "SELECT CLAVE FROM dbo.MATERIAL"));
        assertEquals(1, contar(CALE, "dbo.FACTORESMP"));
        assertEquals(0, contar(CALE, "dbo.CARGAMATERIAL"));
        assertEquals(0, contar(CALE, "dbo.ECARGAMATERIAL"));
        assertEquals(1, contar(APP, "app24.BitacoraEvento"));
        assertEquals("MATERIAL_CARGA_CONFIRMADA", valor(APP, "SELECT accion FROM app24.BitacoraEvento"));
        assertEquals("7001", valor(APP, "SELECT usuario_id FROM app24.BitacoraEvento"));
        assertEquals("it-correlation", valor(APP, "SELECT correlation_id FROM app24.BitacoraEvento"));
        assertEquals("1", valor(APP, "SELECT total_filas FROM app24.CargaCatalogoMaterial WHERE id = " + carga));
        assertEquals("1", valor(APP, "SELECT filas_validas FROM app24.CargaCatalogoMaterial WHERE id = " + carga));
        assertNotNull(valor(APP, "SELECT fecha_confirmacion FROM app24.CargaCatalogoMaterial WHERE id = " + carga));
        transaccionLimpia();
    }

    @Test
    void errorDeNegocioRevierteLaMutacionAutoritativaYPersisteErrores() throws Exception {
        long carga = crearCarga();
        agregarFila(carga, filaValida("MAT-INVALIDA", "NO-EXISTE"));

        Map<String, String> resultado = confirmar(carga);

        assertEquals("BUSINESS_ERRORS", resultado.get("Resultado"));
        assertEquals("1", resultado.get("FilasConError"));
        assertEquals("CON_ERRORES", estadoCarga(carga));
        // El wrapper normaliza los múltiples mensajes legacy en la fila de origen.
        assertEquals(1, contar(APP, "app24.ErrorCargaMaterial"));
        assertEquals("M", valor(APP, "SELECT TOP 1 hoja FROM app24.ErrorCargaMaterial"));
        assertEquals("1", valor(APP, "SELECT TOP 1 fila FROM app24.ErrorCargaMaterial"));
        assertEquals("CargaMaterial", valor(APP, "SELECT TOP 1 columna FROM app24.ErrorCargaMaterial"));
        assertTrue(valor(APP, "SELECT mensaje FROM app24.ErrorCargaMaterial").contains("UNIDAD COMERCIAL"));
        assertEquals("1", valor(APP, "SELECT filas_invalidas FROM app24.CargaCatalogoMaterial WHERE id = " + carga));
        assertEquals(0, contar(CALE, "dbo.MATERIAL"));
        assertEquals(0, contar(CALE, "dbo.FACTORESMP"));
        assertEquals(0, contar(CALE, "dbo.CARGAMATERIAL"));
        assertEquals(0, contar(CALE, "dbo.ECARGAMATERIAL"));
        // La ejecución autoritativa se revierte, pero la transacción de errores deja bitácora funcional.
        assertEquals(1, contar(APP, "app24.BitacoraEvento"));
        assertEquals("MATERIAL_CARGA_CONFIRMACION_ERROR", valor(APP, "SELECT accion FROM app24.BitacoraEvento"));
        assertEquals("FALLO", valor(APP, "SELECT resultado FROM app24.BitacoraEvento"));
        assertEquals("7001", valor(APP, "SELECT usuario_id FROM app24.BitacoraEvento"));
        assertEquals("it-correlation", valor(APP, "SELECT correlation_id FROM app24.BitacoraEvento"));
        transaccionLimpia();
    }

    @Test
    void etapaLegacyPreexistenteFallaCerradaYPreservaTodo() throws Exception {
        long carga = crearCarga();
        agregarFila(carga, filaValida("MAT-NUEVA", "KG"));
        long legacyKey = insertarEtapaLegacy("LEGACY-OCUPADA", "KG");
        ejecutar(CALE, "INSERT INTO dbo.ECARGAMATERIAL (CARGAMATERIALKEY, ERROR) VALUES (" + legacyKey
                + ", 'ERROR_PREEXISTENTE')");

        SQLException error = assertThrows(SQLException.class, () -> confirmar(carga));

        assertTrue(error.getMessage().contains("LEGACY_STAGE_BUSY"));
        assertEquals("PREVISUALIZADA", estadoCarga(carga));
        assertEquals(1, contar(CALE, "dbo.CARGAMATERIAL"));
        assertEquals(1, contar(CALE, "dbo.ECARGAMATERIAL"));
        assertEquals("LEGACY-OCUPADA", valor(CALE, "SELECT CLAVEMATERIAL FROM dbo.CARGAMATERIAL"));
        assertEquals("ERROR_PREEXISTENTE", valor(CALE, "SELECT ERROR FROM dbo.ECARGAMATERIAL"));
        assertEquals(0, contar(APP, "app24.ErrorCargaMaterial"));
        transaccionLimpia();
    }

    @Test
    void escritorConcurrenteEsperaAlCommitExternoYLuegoPersiste() throws Exception {
        long carga = crearCarga();
        agregarFila(carga, filaValida("MAT-BLOQUEO-1", "KG"));
        ExecutorService executor = Executors.newSingleThreadExecutor();
        CountDownLatch escritorListo = new CountDownLatch(1);
        CountDownLatch iniciarEscritor = new CountDownLatch(1);

        try (Connection exterior = conectar(CALE); Statement exteriorSql = exterior.createStatement()) {
            exteriorSql.execute("BEGIN TRANSACTION");
            assertEquals("CONFIRMED", confirmar(exterior, carga).get("Resultado"));

            Future<Long> escritor = executor.submit(() -> {
                escritorListo.countDown();
                iniciarEscritor.await(5, TimeUnit.SECONDS);
                try (Connection otraConexionFisica = conectar(CALE)) {
                    return insertarEtapaLegacy(otraConexionFisica, "LEGACY-POST-COMMIT", "KG");
                }
            });
            assertTrue(escritorListo.await(5, TimeUnit.SECONDS));
            iniciarEscritor.countDown();
            assertThrows(TimeoutException.class, () -> escritor.get(2, TimeUnit.SECONDS),
                    "El escritor legacy debe permanecer bloqueado por TABLOCKX/HOLDLOCK.");

            exteriorSql.execute("COMMIT TRANSACTION");

            assertNotNull(escritor.get(15, TimeUnit.SECONDS));
            assertEquals(1, contar(CALE, "dbo.CARGAMATERIAL"));
            assertEquals("LEGACY-POST-COMMIT", valor(CALE, "SELECT CLAVEMATERIAL FROM dbo.CARGAMATERIAL"));
        } finally {
            executor.shutdownNow();
        }
    }

    @Test
    void confirmacionBloqueadaTieneTimeoutControladoYSinMutacion() throws Exception {
        long carga = crearCarga();
        agregarFila(carga, filaValida("MAT-TIMEOUT", "KG"));

        try (Connection ocupante = conectar(CALE); Statement ocupanteSql = ocupante.createStatement()) {
            ocupanteSql.execute("BEGIN TRANSACTION");
            ocupanteSql.execute("SELECT TOP (1) CARGAMATERIALKEY FROM dbo.CARGAMATERIAL WITH (TABLOCKX, HOLDLOCK)");

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
            assertEquals(0, contar(CALE, "dbo.MATERIAL"));
            assertEquals(0, contar(CALE, "dbo.FACTORESMP"));
        }

        transaccionLimpia();
    }

    @Test
    void execLegacyConcurrenteEsperaAlCommitExterno() throws Exception {
        long carga = crearCarga();
        agregarFila(carga, filaValida("MAT-BLOQUEO-2", "KG"));
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
                    s.execute("EXEC dbo.CARGA_MATERIALES");
                }
                return null;
            });
            assertTrue(legacyListo.await(5, TimeUnit.SECONDS));
            iniciarLegacy.countDown();
            assertThrows(TimeoutException.class, () -> legacy.get(2, TimeUnit.SECONDS),
                    "El EXEC legacy debe quedar bloqueado hasta el commit externo.");

            exteriorSql.execute("COMMIT TRANSACTION");

            legacy.get(15, TimeUnit.SECONDS);
            assertEquals(1, contar(CALE, "dbo.MATERIAL"));
            assertEquals(0, contar(CALE, "dbo.CARGAMATERIAL"));
        } finally {
            executor.shutdownNow();
        }
    }


    @Test
    void reconfirmacionSeRechazaSinSegundaEjecucionLegacy() throws Exception {
        long carga = crearCarga();
        agregarFila(carga, filaValida("MAT-IDEMPOTENTE", "KG"));
        assertEquals("CONFIRMED", confirmar(carga).get("Resultado"));

        SQLException error = assertThrows(SQLException.class, () -> confirmar(carga));

        assertTrue(error.getMessage().contains("ALREADY_CONFIRMED"));
        assertEquals("CONFIRMADA", estadoCarga(carga));
        assertEquals(1, contar(CALE, "dbo.MATERIAL"));
        assertEquals(1, contar(APP, "app24.BitacoraEvento"));
        transaccionLimpia();
    }

    @Test
    void migracionDeConfirmacionEsIdempotenteYConcedeActividadAlAdministrador() throws Exception {
        try (Connection app = conectar(APP)) {
            aplicarArchivo(app, raizRepo().resolve("migrations/13-material-confirmar-state-permission.sql"));
        }
        assertEquals(1, contar(APP, "app24.Actividad WHERE clave = 'MATERIALES_CONFIRMAR'"));
        assertEquals(1, contar(APP, "app24.PerfilActividad pa JOIN app24.Actividad a ON a.id = pa.actividad_id "
                + "JOIN app24.PerfilApp p ON p.id = pa.perfil_id WHERE a.clave = 'MATERIALES_CONFIRMAR' "
                + "AND p.nombre = 'ADMINISTRADOR'"));
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
        Path fixture = raizFixtures().resolve("01-material-confirm-fixture.sql");
        try (Connection cale = conectar(CALE)) {
            aplicarArchivo(cale, fixture);
            // Se carga el fuente exacto auditado después del DDL mínimo que requiere.
            aplicarArchivo(cale, raizFixtures().resolve("CARGA_MATERIALES.legacy.sql"));
            aplicarArchivo(cale, raizRepo().resolve("migrations/13-material-confirmar-state-permission.sql"));
            // El arnés usa el artefacto de producción; el wrapper del fixture sólo era guía sintética.
            aplicarArchivo(cale, raizRepo().resolve("procedures/commands/APP24_C_MATERIAL_CARGA_CONFIRMAR.sql"));
        }
    }

    private static Path raizFixtures() {
        Path desdeBackend = Path.of("src", "test", "resources", "sql", "materials");
        return Files.exists(desdeBackend)
                ? desdeBackend
                : Path.of("backend", "src", "test", "resources", "sql", "materials");
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
             var ps = app.prepareStatement("INSERT INTO app24.CargaCatalogoMaterial "
                     + "(archivo, hash, usuario_id, estado, total_filas, filas_validas, filas_invalidas, version_contrato, correlation_id) "
                     + "VALUES ('it.xlsx', REPLICATE('a', 64), 7001, 'PREVISUALIZADA', 0, 0, 0, 'MATERIAL-V1', 'it-correlation')",
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
             var ps = app.prepareStatement("INSERT INTO app24.CargaCatalogoMaterialFila "
                     + "(carga_id, hoja, fila, datos_json) VALUES (?, 'M', 1, ?)")) {
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
        try (CallableStatement cs = cale.prepareCall("{call dbo.APP24_C_MATERIAL_CARGA_CONFIRMAR(?)}")) {
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
             CallableStatement cs = cale.prepareCall("{call dbo.APP24_C_MATERIAL_CARGA_CONFIRMAR(?)}")) {
            cs.setQueryTimeout(timeoutSegundos);
            cs.setLong(1, cargaId);
            cs.execute();
        }
    }

    private long insertarEtapaLegacy(String clave, String unidad) throws SQLException {
        try (Connection cale = conectar(CALE)) {
            return insertarEtapaLegacy(cale, clave, unidad);
        }
    }

    private long insertarEtapaLegacy(Connection cale, String clave, String unidad) throws SQLException {
        try (var ps = cale.prepareStatement("INSERT INTO dbo.CARGAMATERIAL "
                + "(CLAVEMATERIAL, DESCRIPCIONCOMERCIAL, FRACCION, UNIDADCOMERCIAL, UNIDADTARIFA, TIPOM) "
                + "VALUES (?, 'Material legacy', '17019999', ?, ?, 'IMPORTADO')", Statement.RETURN_GENERATED_KEYS)) {
            ps.setString(1, clave);
            ps.setString(2, unidad);
            ps.setString(3, unidad);
            ps.executeUpdate();
            try (ResultSet rs = ps.getGeneratedKeys()) {
                rs.next();
                return rs.getLong(1);
            }
        }
    }

    private String estadoCarga(long cargaId) throws SQLException {
        return valor(APP, "SELECT estado FROM app24.CargaCatalogoMaterial WHERE id = " + cargaId);
    }


    private int contar(String db, String tabla) throws SQLException {
        return Integer.parseInt(valor(db, "SELECT COUNT(*) FROM " + tabla));
    }

    private String valor(String db, String sql) throws SQLException {
        try (Connection c = conectar(db); Statement s = c.createStatement(); ResultSet rs = s.executeQuery(sql)) {
            return rs.next() ? rs.getString(1) : null;
        }
    }

    private void ejecutar(String db, String sql) throws SQLException {
        try (Connection c = conectar(db); Statement s = c.createStatement()) {
            s.execute(sql);
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

    private static String filaValida(String clave, String unidad) {
        return "{" +
                "\"ClaveMaterial\":\"" + clave + "\"," +
                "\"ClaveMaterialProveedor\":\"PROV-" + clave + "\"," +
                "\"DescripcionComercial\":\"Material sintetico\"," +
                "\"UnidadComercial\":\"" + unidad + "\"," +
                "\"UnidadTarifa\":\"" + unidad + "\"," +
                "\"Fraccion\":\"17019999\"," +
                "\"KG\":\"2.5\"," +
                "\"GR\":\"0\",\"ML\":\"0\",\"MCUA\":\"0\",\"MCUB\":\"0\",\"PZA\":\"0\"," +
                "\"LT\":\"0\",\"PAR\":\"0\",\"MI\":\"0\",\"JGO\":\"0\",\"TON\":\"0\",\"BAR\":\"0\"," +
                "\"GRN\":\"0\",\"DECE\":\"0\",\"CIEN\":\"0\",\"DOCE\":\"0\",\"CAJA\":\"0\",\"BOTELLA\":\"0\"," +
                "\"DIVISION\":\"1\",\"ENTIDAD\":\"1\",\"TIPOM\":\"IMPORTADO\"," +
                "\"NumeroSerie\":\"SERIE-1\",\"MARCA\":\"MARCA-SINTETICA\",\"MODELO\":\"MODELO-SINTETICO\"}";
    }
}
