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
 * constancias de transferencia. Sólo usa SQL Server Testcontainers; nunca una base LIVE.
 *
 * <p>Auditoría LIVE 2026-10-05 de {@code dbo.CARGA_SUBMAQUILA}: no recibe parámetros,
 * no valida y no tiene error stage. A diferencia de {@code dbo.CARGAAgentes}, el
 * contrato es estrictamente INSERT-only y no deduplica, de modo que un reintento sobre
 * un stage ya poblado duplicaría salidas y partidas. Por eso el wrapper falla cerrado
 * con {@code LEGACY_STAGE_BUSY} ante cualquier fila ajena y trata {@code CONFIRMADA}
 * como estado terminal.</p>
 */
class SubmaquilaLegacyStageConcurrencyTest {

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
            throw new IllegalStateException("Docker no disponible en CI: el arnés SQL de submaquila es obligatorio "
                    + "(SUBMAQUILA_STAGE_CONCURRENCY_CI_REQUIRED).");
        }
        Assumptions.assumeTrue(disponible, "Docker no disponible localmente; se omite el arnés SQL.");
        SQL.start();
        crearBases();
        try (Connection cale = conectar(CALE)) {
            aplicarArchivo(cale, raizFixtures().resolve("01-submaquila-confirm-fixture.sql"));
            aplicarArchivo(cale, raizFixtures().resolve("CARGA_SUBMAQUILA.legacy.sql"));
        }
        aplicarArchivo(conectar(APP), raizRepo().resolve("migrations/18-submaquila-staging-confirmation-v1.sql"));
        aplicarArchivo(conectar(CALE), raizRepo().resolve("procedures/commands/APP24_C_SUBMAQUILA_CARGA_CONFIRMAR.sql"));
        assertContextoWrapper();
    }

    @AfterAll
    static void detenerContenedor() {
        if (disponible) SQL.stop();
    }

    @BeforeEach
    void limpiarDatos() throws Exception {
        try (Connection cale = conectar(CALE); Statement s = cale.createStatement()) {
            s.execute("DELETE FROM dbo.TMPSUBMAQUILA");
            s.execute("DELETE FROM dbo.PSALIDAS");
            s.execute("DELETE FROM dbo.SALIDAS");
        }
        try (Connection app = conectar(APP); Statement s = app.createStatement()) {
            s.execute("DELETE FROM app24.ErrorCargaSubmaquila");
            s.execute("DELETE FROM app24.CargaSubmaquilaFila");
            s.execute("DELETE FROM app24.CargaSubmaquila");
            s.execute("DELETE FROM app24.BitacoraEvento");
        }
    }

    @Test
    void confirmacionValidaCreaUnaSalidaPorFolioFechaYSubmaquilador() throws Exception {
        long carga = crearCarga();
        // Todos los renglones con CLAVE P001 para que la fracción resuelva desde PRODUCTOS.
        agregarFila(carga, renglon("F-001", "2026-10-05", "SUBMAQ Uno", "P001", "10.5", 1), 2);
        agregarFila(carga, renglon("F-001", "2026-10-05", "SUBMAQ Uno", "P001", "2", 2), 3);
        agregarFila(carga, renglon("F-002", "2026-10-05", "SUBMAQ Dos", "P001", "3", 1), 4);

        Map<String, String> resultado = confirmar(carga);

        assertEquals(Long.toString(carga), resultado.get("CargaId"));
        assertEquals("CONFIRMED", resultado.get("Resultado"));
        assertEquals("CONFIRMADA", estadoCarga(carga));
        assertEquals("3", resultado.get("TotalFilas"));
        assertEquals("3", resultado.get("FilasValidas"));
        assertEquals("0", resultado.get("FilasConError"));
        assertNotNull(resultado.get("ConfirmadaEn"));
        // Tres renglones, pero sólo dos grupos folio/fecha/submaquilador.
        assertEquals(2, contar(CALE, "dbo.SALIDAS"));
        assertEquals(3, contar(CALE, "dbo.PSALIDAS"));
        assertEquals("SUBMAQUILA", valor(CALE, "SELECT RTRIM(Tipo_operacion) FROM dbo.SALIDAS ORDER BY SalidaKey"));
        assertEquals("SUB", valor(CALE, "SELECT RTRIM(Cve_pedimento) FROM dbo.SALIDAS ORDER BY SalidaKey"));
        // SalidaKey se numera por folio: F-001 antes que F-002.
        assertEquals("F-001", valor(CALE, "SELECT RTRIM(Documento) FROM dbo.SALIDAS ORDER BY SalidaKey"));
        assertEquals("F-002", valor(CALE, "SELECT RTRIM(Documento) FROM dbo.SALIDAS ORDER BY SalidaKey DESC"));
        assertEquals("SUBMAQ Uno", valor(CALE, "SELECT RTRIM(Transfiere) FROM dbo.SALIDAS ORDER BY SalidaKey"));
        assertEquals("SUBMAQ Dos", valor(CALE, "SELECT RTRIM(Transfiere) FROM dbo.SALIDAS ORDER BY SalidaKey DESC"));
        // Las aserciones siguientes localizan el renglón por su folio resuelto, no por
        // Psalidakey: el legacy numera con ROW_NUMBER() OVER (ORDER BY LINEA) y esa
        // numeración es no determinista cuando dos renglones comparten LINEA.
        assertEquals("7308.10.01", valor(CALE, fraccionDeRenglon("F-001", 1)));
        assertEquals("7308.10.01", valor(CALE, fraccionDeRenglon("F-001", 2)));
        assertEquals("10.5000", valor(CALE, "SELECT CAST(Cantidad AS VARCHAR(20)) FROM dbo.PSALIDAS p "
                + "WHERE EXISTS (SELECT 1 FROM dbo.SALIDAS s WHERE CAST(s.SalidaKey AS NUMERIC(18,0)) = p.Salidalink "
                + "AND s.Documento = 'F-001') AND p.partida = 1"));
        assertEquals("2.0000", valor(CALE, "SELECT CAST(Cantidad AS VARCHAR(20)) FROM dbo.PSALIDAS p "
                + "WHERE EXISTS (SELECT 1 FROM dbo.SALIDAS s WHERE CAST(s.SalidaKey AS NUMERIC(18,0)) = p.Salidalink "
                + "AND s.Documento = 'F-001') AND p.partida = 2"));
        // SALIDALINK debe apuntar a la salida del folio de cada renglón: los tres renglones
        // se reparten entre las dos salidas según su DOCUMENTO.
        assertEquals(2, Integer.parseInt(valor(CALE, "SELECT COUNT(*) FROM dbo.PSALIDAS p "
                + "WHERE EXISTS (SELECT 1 FROM dbo.SALIDAS s WHERE CAST(s.SalidaKey AS NUMERIC(18,0)) = p.Salidalink "
                + "AND s.Documento = 'F-001')")));
        assertEquals(1, Integer.parseInt(valor(CALE, "SELECT COUNT(*) FROM dbo.PSALIDAS p "
                + "WHERE EXISTS (SELECT 1 FROM dbo.SALIDAS s WHERE CAST(s.SalidaKey AS NUMERIC(18,0)) = p.Salidalink "
                + "AND s.Documento = 'F-002')")));
        assertEquals(0, contar(CALE, "dbo.TMPSUBMAQUILA"));
        assertEquals(1, contar(APP, "app24.BitacoraEvento"));
        assertEquals("SUBMAQUILA_CARGA_CONFIRMADA", valor(APP, "SELECT accion FROM app24.BitacoraEvento"));
        transaccionLimpia();
    }

    @Test
    void stageOcupadoFallaCerrado() throws Exception {
        long carga = crearCarga();
        agregarFila(carga, renglon("F-100", "2026-10-05", "SUBMAQ Ajeno", "P001", "1", 1));
        try (Connection cale = conectar(CALE); Statement s = cale.createStatement()) {
            s.execute("INSERT INTO dbo.TMPSUBMAQUILA (FOLIO, FECHA, SUBMAQUILADOR, CLAVE, CANTIDAD, UNIDAD, DESCRIPCION, LINEA) "
                    + "VALUES ('F-AJENO', '2026-10-05', 'Ajeno', 'P001', 1, 'PIEZ', 'Fila ajena', 1)");
        }

        SQLException error = assertThrows(SQLException.class, () -> confirmar(carga));

        assertTrue(error.getMessage().contains("LEGACY_STAGE_BUSY"),
                "Mensaje no contiene LEGACY_STAGE_BUSY: " + error.getMessage());
        assertEquals("PREVISUALIZADA", estadoCarga(carga));
        assertEquals(1, contar(CALE, "dbo.TMPSUBMAQUILA"));
        assertEquals(0, contar(CALE, "dbo.SALIDAS"));
        assertEquals(0, contar(CALE, "dbo.PSALIDAS"));
        transaccionLimpia();
    }

    @Test
    void cargaSinFilasNoProcesa() throws Exception {
        long carga = crearCarga();

        SQLException error = assertThrows(SQLException.class, () -> confirmar(carga));

        assertTrue(error.getMessage().contains("CARGA_SIN_FILAS"),
                "Mensaje no contiene CARGA_SIN_FILAS: " + error.getMessage());
        assertEquals("PREVISUALIZADA", estadoCarga(carga));
        assertEquals(0, contar(CALE, "dbo.SALIDAS"));
        transaccionLimpia();
    }

    @Test
    void cargaConErroresNoProcesa() throws Exception {
        long carga = crearCarga();
        try (Connection app = conectar(APP); Statement s = app.createStatement()) {
            s.execute("INSERT INTO app24.ErrorCargaSubmaquila (carga_id, hoja, fila, columna, valor_enmascarado, codigo, mensaje) "
                    + "VALUES (" + carga + ", 'P', 2, 'Clave', 'no almacenado', 'CLAVE_SUBMAQUILA_VACIA', 'La clave es obligatoria.')");
        }

        SQLException error = assertThrows(SQLException.class, () -> confirmar(carga));

        assertTrue(error.getMessage().contains("CARGA_CON_ERRORES"),
                "Mensaje no contiene CARGA_CON_ERRORES: " + error.getMessage());
        assertEquals(1, contar(APP, "app24.ErrorCargaSubmaquila"));
        assertEquals(0, contar(CALE, "dbo.SALIDAS"));
        transaccionLimpia();
    }

    @Test
    void reconfirmacionSeRechaza() throws Exception {
        long carga = crearCarga();
        agregarFila(carga, renglon("F-200", "2026-10-05", "SUBMAQ Uno", "P001", "1", 1));
        assertEquals("CONFIRMED", confirmar(carga).get("Resultado"));

        SQLException error = assertThrows(SQLException.class, () -> confirmar(carga));

        assertTrue(error.getMessage().contains("ALREADY_CONFIRMED"), error.getMessage());
        assertEquals("CONFIRMADA", estadoCarga(carga));
        assertEquals(1, contar(CALE, "dbo.SALIDAS"));
        assertEquals(1, contar(CALE, "dbo.PSALIDAS"));
        transaccionLimpia();
    }

    @Test
    void cargaInexistenteSeRechaza() throws Exception {
        SQLException error = assertThrows(SQLException.class, () -> confirmar(987654321L));

        assertTrue(error.getMessage().contains("CARGA_NO_ENCONTRADA"), error.getMessage());
    }

    @Test
    void cargaIdInvalidoSeRechaza() throws Exception {
        SQLException error = assertThrows(SQLException.class, () -> confirmar(0L));

        assertTrue(error.getMessage().contains("PARAMETRO_INVALIDO"), error.getMessage());
    }

    @Test
    void escritorConcurrenteEspera() throws Exception {
        long carga = crearCarga();
        agregarFila(carga, renglon("F-300", "2026-10-05", "SUBMAQ Uno", "P001", "1", 1));
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
                    s.execute("INSERT INTO dbo.TMPSUBMAQUILA (FOLIO, FECHA, SUBMAQUILADOR, CLAVE, CANTIDAD, UNIDAD, DESCRIPCION, LINEA) "
                            + "VALUES ('F-POST', '2026-10-05', 'Externo', 'P001', 1, 'PIEZ', 'Fila posterior', 1)");
                    return 1L;
                }
            });
            assertTrue(escritorListo.await(5, TimeUnit.SECONDS));
            iniciarEscritor.countDown();
            assertThrows(TimeoutException.class, () -> escritor.get(2, TimeUnit.SECONDS),
                    "El escritor debe permanecer bloqueado por TABLOCKX/HOLDLOCK.");

            exteriorSql.execute("COMMIT TRANSACTION");

            assertNotNull(escritor.get(15, TimeUnit.SECONDS));
            assertEquals(1, contar(CALE, "dbo.TMPSUBMAQUILA"));
        } finally {
            executor.shutdownNow();
        }
    }

    @Test
    void execCargaSubmaquilaConcurrenteEspera() throws Exception {
        long carga = crearCarga();
        agregarFila(carga, renglon("F-400", "2026-10-05", "SUBMAQ Uno", "P001", "1", 1));
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
                    s.execute("EXEC dbo.CARGA_SUBMAQUILA");
                }
                return null;
            });
            assertTrue(legacyListo.await(5, TimeUnit.SECONDS));
            iniciarLegacy.countDown();
            assertThrows(TimeoutException.class, () -> legacy.get(2, TimeUnit.SECONDS),
                    "El EXEC legacy debe quedar bloqueado hasta el commit externo.");

            exteriorSql.execute("COMMIT TRANSACTION");

            legacy.get(15, TimeUnit.SECONDS);
            // El EXEC legacy concurrente corrió sobre un stage vacío: no debe crear nada.
            assertEquals(1, contar(CALE, "dbo.SALIDAS"));
            assertEquals(1, contar(CALE, "dbo.PSALIDAS"));
            assertEquals(0, contar(CALE, "dbo.TMPSUBMAQUILA"));
        } finally {
            executor.shutdownNow();
        }
    }

    @Test
    void timeoutControladoSinMutacion() throws Exception {
        long carga = crearCarga();
        agregarFila(carga, renglon("F-500", "2026-10-05", "SUBMAQ Uno", "P001", "1", 1));

        try (Connection ocupante = conectar(CALE); Statement ocupanteSql = ocupante.createStatement()) {
            ocupanteSql.execute("BEGIN TRANSACTION");
            ocupanteSql.execute("SELECT TOP (1) TMPSKEY FROM dbo.TMPSUBMAQUILA WITH (TABLOCKX, HOLDLOCK)");

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
            assertEquals(0, contar(CALE, "dbo.SALIDAS"));
        }

        transaccionLimpia();
    }

    @Test
    void rollbackPosteriorAlLegacy() throws Exception {
        long carga = crearCarga();
        agregarFila(carga, renglon("F-600", "2026-10-05", "SUBMAQ Uno", "P001", "1", 1));

        confirmar(carga);

        // dbo.CARGA_SUBMAQUILA no valida: un folio ya existente con dos salidas hace fallar
        // el subquery escalar de SALIDALINK. El wrapper debe revertir todo, sin mutación.
        assertEquals(0, contar(CALE, "dbo.TMPSUBMAQUILA"));
        transaccionLimpia();
    }

    @Test
    void folioDuplicadoEnStageReviertePorSalidalink() throws Exception {
        // Riesgo heredado auditado: si el DOCUMENTO del folio ya tiene más de una salida,
        // el subquery escalar de SALIDALINK del legacy falla con "more than 1 value".
        // El wrapper no lo silencia: revierte y propaga.
        try (Connection cale = conectar(CALE); Statement s = cale.createStatement()) {
            s.execute("INSERT INTO dbo.SALIDAS (SalidaKey, Tipo_operacion, Documento, Fecha, Cve_pedimento, Transfiere) "
                    + "VALUES (1, 'PEDIMENTO', 'F-700', GETDATE(), 'ABC', 'Externo')");
            s.execute("INSERT INTO dbo.SALIDAS (SalidaKey, Tipo_operacion, Documento, Fecha, Cve_pedimento, Transfiere) "
                    + "VALUES (2, 'PEDIMENTO', 'F-700', GETDATE(), 'ABC', 'Externo')");
        }
        long carga = crearCarga();
        agregarFila(carga, renglon("F-700", "2026-10-05", "SUBMAQ Uno", "P001", "1", 1));

        SQLException error = assertThrows(SQLException.class, () -> confirmar(carga));

        assertTrue(error.getMessage().toLowerCase().contains("more than 1"),
                "Se esperaba el error del subquery escalar del legacy: " + error.getMessage());
        assertEquals("PREVISUALIZADA", estadoCarga(carga));
        assertEquals(2, contar(CALE, "dbo.SALIDAS"), "No debe quedar ninguna salida creada por la carga.");
        assertEquals(0, contar(CALE, "dbo.PSALIDAS"));
        assertEquals(0, contar(CALE, "dbo.TMPSUBMAQUILA"));
        assertEquals(0, contar(APP, "app24.BitacoraEvento"));
        transaccionLimpia();
    }

    @Test
    void longitudFolioBoundary() throws Exception {
        // FOLIO es VARCHAR(50): una longitud exacta de 50 debe llegar íntegra a SALIDAS.
        String folio = "F".repeat(50);
        long carga = crearCarga();
        agregarFila(carga, renglon(folio, "2026-10-05", "SUBMAQ Uno", "P001", "1", 1));

        Map<String, String> resultado = confirmar(carga);

        assertEquals("CONFIRMED", resultado.get("Resultado"));
        assertEquals(1, contar(CALE, "dbo.SALIDAS"));
        assertEquals(folio, valor(CALE, "SELECT RTRIM(Documento) FROM dbo.SALIDAS"));
        transaccionLimpia();
    }

    @Test
    void descripcionBoundary250() throws Exception {
        String descripcion = "D".repeat(250);
        long carga = crearCarga();
        agregarFila(carga, renglon("F-800", "2026-10-05", "SUBMAQ Uno", "P001", "1", 1, descripcion));

        Map<String, String> resultado = confirmar(carga);

        assertEquals("CONFIRMED", resultado.get("Resultado"));
        assertEquals(descripcion, valor(CALE, "SELECT Descripcion FROM dbo.PSALIDAS"));
        transaccionLimpia();
    }

    @Test
    void migracionIdempotente() throws Exception {
        try (Connection app = conectar(APP)) {
            aplicarArchivo(app, raizRepo().resolve("migrations/18-submaquila-staging-confirmation-v1.sql"));
            aplicarArchivo(app, raizRepo().resolve("migrations/18-submaquila-staging-confirmation-v1.sql"));
        }
        assertEquals(1, contar(APP, "app24.Actividad WHERE clave = 'SUBMAQUILA_CONFIRMAR'"));
        assertEquals(1, contar(APP, "app24.Actividad WHERE clave = 'SUBMAQUILA_CARGAR'"));
        int total = Integer.parseInt(valor(APP,
                "SELECT COUNT(*) FROM app24.PerfilActividad pa JOIN app24.Actividad a ON a.id = pa.actividad_id "
                        + "JOIN app24.PerfilApp p ON p.id = pa.perfil_id WHERE a.clave IN ('SUBMAQUILA_CONFIRMAR','SUBMAQUILA_CARGAR') "
                        + "AND p.nombre = 'ADMINISTRADOR'"));
        assertEquals(2, total, "El ADMINISTRADOR debe tener SUBMAQUILA_CONFIRMAR y SUBMAQUILA_CARGAR asignadas");
    }

    @Test
    void stagingCreadoRechazaConfirmada() throws Exception {
        long id = invocarCrearStaging("CONFIRMADA");
        assertEquals(0, id, "APP24_C_SUBMAQUILA_CARGA_CREAR debió rechazar @Estado='CONFIRMADA'.");
        assertEquals(0, contar(APP, "app24.CargaSubmaquila"));
    }

    @Test
    void stagingCreadoAceptaPrevisualizada() throws Exception {
        long id = invocarCrearStaging("PREVISUALIZADA");
        assertTrue(id > 0);
        assertEquals("PREVISUALIZADA", estadoCarga(id));
    }

    private long invocarCrearStaging(String estado) throws SQLException {
        try (Connection app = conectar(APP);
             CallableStatement cs = app.prepareCall(
                     "EXEC app24.APP24_C_SUBMAQUILA_CARGA_CREAR @Archivo=?, @Hash=?, @UsuarioId=?, @Estado=?, @TotalFilas=?, "
                             + "@FilasValidas=?, @VersionContrato=?, @CorrelationId=?, @FilasJson=?, @ErroresJson=?, @CargaId=?")) {
            cs.setString(1, "it.xlsx");
            cs.setString(2, "s" + String.format("%063d", System.nanoTime() % 1000000));
            cs.setLong(3, 7001L);
            cs.setString(4, estado);
            cs.setInt(5, 1);
            cs.setInt(6, 1);
            cs.setString(7, "SUBMAQUILA-V1");
            cs.setString(8, "it-correlation");
            cs.setNString(9, "[{\"hoja\":\"P\",\"fila\":2,\"datos\":" + renglon("F-900", "2026-10-05", "SUBMAQ Uno", "P001", "1", 1) + "}]");
            cs.setNString(10, "[]");
            cs.registerOutParameter(11, java.sql.Types.BIGINT);
            try {
                cs.execute();
            } catch (SQLException error) {
                assertTrue(error.getMessage().contains("PARAMETRO_INVALIDO"), error.getMessage());
                return 0L;
            }
            long id = cs.getLong(11);
            while (cs.getMoreResults() || cs.getUpdateCount() != -1) {
                // Drena cualquier resultado residual del command.
            }
            return id;
        }
    }

    /** Localiza un renglón por el folio de su SALIDALINK, sin depender de Psalidakey. */
    private static String fraccionDeRenglon(String folio, int linea) {
        return "SELECT RTRIM(p.Fraccion) FROM dbo.PSALIDAS p "
                + "WHERE p.partida = " + linea + " AND EXISTS (SELECT 1 FROM dbo.SALIDAS s "
                + "WHERE CAST(s.SalidaKey AS NUMERIC(18,0)) = p.Salidalink AND s.Documento = '" + folio + "')";
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
            Integer idCale = scalarInt(cale, "SELECT OBJECT_ID('dbo.APP24_C_SUBMAQUILA_CARGA_CONFIRMAR')");
            assertNotNull(idCale);
            assertNotEquals(0, idCale, "CALE_IMMEX no contiene dbo.APP24_C_SUBMAQUILA_CARGA_CONFIRMAR.");
        }
        try (Connection app = conectar(APP)) {
            assertEquals(0, scalarInt(app, "SELECT OBJECT_ID('dbo.APP24_C_SUBMAQUILA_CARGA_CONFIRMAR')"),
                    "ANEXO24_DEV NO debe contener dbo.APP24_C_SUBMAQUILA_CARGA_CONFIRMAR (context leak).");
            assertNotEquals(0, scalarInt(app, "SELECT OBJECT_ID('app24.APP24_C_SUBMAQUILA_CARGA_CREAR')"));
            assertNotEquals(0, scalarInt(app, "SELECT OBJECT_ID('app24.APP24_Q_SUBMAQUILA_CARGA_OBTENER')"));
            assertNotEquals(0, scalarInt(app, "SELECT OBJECT_ID('app24.APP24_Q_SUBMAQUILA_CARGA_ERRORES')"));
            assertNotEquals(0, scalarInt(app, "SELECT OBJECT_ID('app24.APP24_Q_SUBMAQUILA_CARGA_POR_HASH')"));
        }
    }

    private static Path raizFixtures() {
        Path desdeBackend = Path.of("src", "test", "resources", "sql", "submaquilas");
        return Files.exists(desdeBackend)
                ? desdeBackend
                : Path.of("backend", "src", "test", "resources", "sql", "submaquilas");
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
             var ps = app.prepareStatement("INSERT INTO app24.CargaSubmaquila "
                     + "(archivo, hash, usuario_id, estado, total_filas, filas_validas, filas_invalidas, version_contrato, correlation_id) "
                     + "VALUES ('it.xlsx', REPLICATE('a', 64), 7001, 'PREVISUALIZADA', 0, 0, 0, 'SUBMAQUILA-V1', 'it-correlation')",
                     Statement.RETURN_GENERATED_KEYS)) {
            ps.executeUpdate();
            try (ResultSet rs = ps.getGeneratedKeys()) {
                rs.next();
                return rs.getLong(1);
            }
        }
    }

    private void agregarFila(long cargaId, String datosJson) throws SQLException {
        agregarFila(cargaId, datosJson, 2);
    }

    /**
     * @param cargaId carga destino
     * @param datosJson renglón serializado
     * @param fila número de fila del archivo; UQ_CargaSubmaquilaFila es (carga_id, hoja, fila)
     */
    private void agregarFila(long cargaId, String datosJson, int fila) throws SQLException {
        try (Connection app = conectar(APP);
             var ps = app.prepareStatement("INSERT INTO app24.CargaSubmaquilaFila "
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
        try (CallableStatement cs = cale.prepareCall("{call dbo.APP24_C_SUBMAQUILA_CARGA_CONFIRMAR(?)}")) {
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
             CallableStatement cs = cale.prepareCall("{call dbo.APP24_C_SUBMAQUILA_CARGA_CONFIRMAR(?)}")) {
            cs.setQueryTimeout(timeoutSegundos);
            cs.setLong(1, cargaId);
            cs.execute();
        }
    }

    private String estadoCarga(long cargaId) throws SQLException {
        return valor(APP, "SELECT estado FROM app24.CargaSubmaquila WHERE id = " + cargaId);
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

    private static String renglon(String folio, String fecha, String submaquilador, String clave,
                                   String cantidad, int linea) {
        return renglon(folio, fecha, submaquilador, clave, cantidad, linea, "Renglon sintetico");
    }

    private static String renglon(String folio, String fecha, String submaquilador, String clave,
                                   String cantidad, int linea, String descripcion) {
        return "{" +
                "\"Folio\":\"" + folio + "\"," +
                "\"Fecha\":\"" + fecha + "\"," +
                "\"Submaquilador\":\"" + submaquilador + "\"," +
                "\"Clave\":\"" + clave + "\"," +
                "\"Cantidad\":\"" + cantidad + "\"," +
                "\"Unidad\":\"PIEZ\"," +
                "\"Descripcion\":\"" + descripcion + "\"," +
                "\"Linea\":\"" + linea + "\"}";
    }
}
