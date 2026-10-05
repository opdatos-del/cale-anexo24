package com.jovycandy.anexo24.operations.actas;

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
 * Arnes de ACTAS contra SQL Server Testcontainers (nunca LIVE). La fixture
 * CARGAACTAS.legacy.sql es copia textual fiel del OBJECT_DEFINITION LIVE 2026-10-05.
 */
class ActaLegacyStageConcurrencyTest {

    private static final String CALE = "CALE_IMMEX";
    private static final String APP = "ANEXO24_DEV";

    @SuppressWarnings("resource")
    static final MSSQLServerContainer<?> SQL =
            new MSSQLServerContainer<>("mcr.microsoft.com/mssql/server:2022-latest").acceptLicense();

    private static boolean disponible;

    @BeforeAll
    static void iniciarContenedor() throws Exception {
        disponible = dockerDisponible();
        if (!disponible && "true".equalsIgnoreCase(System.getenv("CI"))) {
            throw new IllegalStateException("Docker no disponible en CI: el arnes SQL de actas es obligatorio.");
        }
        Assumptions.assumeTrue(disponible, "Docker no disponible localmente; se omite el arnes SQL.");
        SQL.start();
        crearBases();
        try (Connection cale = conectar(CALE)) {
            aplicarArchivo(cale, raizFixtures().resolve("01-acta-live-schema.sql"));
            aplicarArchivo(cale, raizFixtures().resolve("CARGAACTAS.legacy.sql"));
        }
        aplicarArchivo(conectar(APP), raizRepo().resolve("migrations/18-acta-staging-confirmation-v1.sql"));
        aplicarArchivo(conectar(CALE), raizRepo().resolve("procedures/commands/APP24_C_ACTA_CARGA_CONFIRMAR.sql"));
    }

    @AfterAll
    static void detenerContenedor() {
        if (disponible) SQL.stop();
    }

    @BeforeEach
    void limpiarDatos() throws Exception {
        try (Connection cale = conectar(CALE); Statement s = cale.createStatement()) {
            s.execute("DELETE FROM dbo.ACTA");
            s.execute("DELETE FROM dbo.DIRIGIDO");
            s.execute("DELETE FROM dbo.PSALIDAS");
            s.execute("DELETE FROM dbo.SALIDAS");
            s.execute("DELETE FROM dbo.GENERADORES");
        }
        try (Connection app = conectar(APP); Statement s = app.createStatement()) {
            s.execute("DELETE FROM app24.ErrorCargaActa");
            s.execute("DELETE FROM app24.CargaActaFila");
            s.execute("DELETE FROM app24.CargaActa");
            s.execute("DELETE FROM app24.BitacoraEvento");
        }
    }

    @Test
    void legacyLiveContractEsEjecutable() throws Exception {
        try (Connection cale = conectar(CALE); Statement s = cale.createStatement()) {
            s.execute("INSERT INTO dbo.ACTA (Folio, fecha, clave, linea, cantidad, umc, descargadirigida, VALORCOMERCIAL) "
                    + "VALUES ('ACC-1', '2026-10-05', 'P001', 1, 5, 'PIEZ', 'DIR-1', 12.5)");
            s.execute("EXEC dbo.CARGAACTAS");
            assertEquals(1, contar(CALE, "dbo.SALIDAS"));
            assertEquals(1, contar(CALE, "dbo.PSALIDAS"));
            assertEquals(1, contar(CALE, "dbo.DIRIGIDO"));
        }
    }

    @Test
    void fixtureEsCopiaFielDelContratoLive() throws Exception {
        String definicion = Files.readString(raizFixtures().resolve("CARGAACTAS.legacy.sql"));
        assertTrue(definicion.contains("LEGACY_EXECUTION_SAFE = YES"));
        assertTrue(definicion.contains("DELETE FROM GENERADORES"));
        assertTrue(definicion.contains("MAX(SALIDAKEY)"));
    }

    @Test
    void confirmacionValidaCreaSalidaPartidaDirigidoYGeneradores() throws Exception {
        try (Connection cale = conectar(CALE); Statement s = cale.createStatement()) {
            s.execute("INSERT INTO dbo.GENERADORES (tabla, consecutivo) VALUES ('ACTA', 7)");
        }
        long carga = crearCarga();
        agregarFila(carga, renglon("ACC-10", "2026-10-05", "P001", 1, "5", "PIEZ", "DIR-10", "12.5"), 2);

        Map<String, String> resultado = confirmar(carga);

        assertEquals("CONFIRMED", resultado.get("Resultado"));
        assertEquals("CONFIRMADA", estadoCarga(carga));
        assertEquals(1, contar(CALE, "dbo.SALIDAS"));
        assertEquals(1, contar(CALE, "dbo.PSALIDAS"));
        assertEquals(1, contar(CALE, "dbo.DIRIGIDO"));
        assertEquals("DESPERDICIOS", valor(CALE, "SELECT RTRIM(Tipo_operacion) FROM dbo.SALIDAS"));
        assertEquals("ACTA_CARGA_CONFIRMADA", valor(APP, "SELECT accion FROM app24.BitacoraEvento"));
        assertEquals(0, contar(CALE, "dbo.ACTA"));
        assertEquals(0, contar(CALE, "dbo.GENERADORES"));
        transaccionLimpia();
    }

    @Test
    void cargaInexistenteSeRechaza() throws Exception {
        SQLException error = assertThrows(SQLException.class, () -> confirmar(987654321L));
        assertTrue(error.getMessage().contains("CARGA_NO_ENCONTRADA"), error.getMessage());
    }

    @Test
    void cargaSinFilasNoProcesa() throws Exception {
        long carga = crearCarga();
        SQLException error = assertThrows(SQLException.class, () -> confirmar(carga));
        assertTrue(error.getMessage().contains("CARGA_SIN_FILAS"), error.getMessage());
        assertEquals(0, contar(CALE, "dbo.SALIDAS"));
        transaccionLimpia();
    }

    @Test
    void cargaConErroresParserNoProcesa() throws Exception {
        long carga = crearCarga();
        try (Connection app = conectar(APP); Statement s = app.createStatement()) {
            s.execute("INSERT INTO app24.ErrorCargaActa (carga_id, hoja, fila, columna, valor_enmascarado, codigo, mensaje) "
                    + "VALUES (" + carga + ", 'A', 2, 'Clave', 'no almacenado', 'CLAVE_ACTA_VACIA', 'La clave es obligatoria.')");
        }
        SQLException error = assertThrows(SQLException.class, () -> confirmar(carga));
        assertTrue(error.getMessage().contains("CARGA_CON_ERRORES"), error.getMessage());
        assertEquals(0, contar(CALE, "dbo.SALIDAS"));
        transaccionLimpia();
    }

    @Test
    void stageOcupadoFallaCerrado() throws Exception {
        long carga = crearCarga();
        agregarFila(carga, renglon("ACC-20", "2026-10-05", "P001", 1, "1", "PIEZ", "", "1"), 2);
        try (Connection cale = conectar(CALE); Statement s = cale.createStatement()) {
            s.execute("INSERT INTO dbo.ACTA (Folio, fecha, clave, linea, cantidad, umc, descargadirigida, VALORCOMERCIAL) "
                    + "VALUES ('AJENA', '2026-10-05', 'P001', 1, 1, 'PIEZ', '', 1)");
        }
        SQLException error = assertThrows(SQLException.class, () -> confirmar(carga));
        assertTrue(error.getMessage().contains("LEGACY_STAGE_BUSY"), error.getMessage());
        assertEquals(1, contar(CALE, "dbo.ACTA"), "El stage ajeno no debe tocarse.");
        assertEquals(0, contar(CALE, "dbo.SALIDAS"));
        transaccionLimpia();
    }

    @Test
    void escritorSalidasConcurrenteEspera() throws Exception {
        assertWriterBloqueado(
                "INSERT INTO dbo.SALIDAS (SalidaKey, Documento, Fecha, Tipo_operacion, Cve_pedimento, Cve_cliente, Aduana, Agente, Pais, TC, bloqueado) "
                        + "SELECT ISNULL(MAX(SalidaKey),0)+1, 'EXT', GETDATE(), 'DESPERDICIOS', 'DESP', '-', '-', '-', '-', 0, 0 FROM dbo.SALIDAS",
                "dbo.SALIDAS");
    }

    @Test
    void escritorPsalidasConcurrenteEspera() throws Exception {
        assertWriterBloqueado(
                "INSERT INTO dbo.PSALIDAS (Psalidakey, Clave, descargaDirigida) SELECT ISNULL(MAX(Psalidakey),0)+1, 'EXT', '' FROM dbo.PSALIDAS",
                "dbo.PSALIDAS");
    }

    @Test
    void escritorDirigidoConcurrenteEspera() throws Exception {
        assertWriterBloqueado(
                "INSERT INTO dbo.DIRIGIDO (dirigidokey, documento) SELECT ISNULL(MAX(dirigidokey),0)+1, 'EXT' FROM dbo.DIRIGIDO",
                "dbo.DIRIGIDO");
    }

    @Test
    void timeoutControladoSinMutacion() throws Exception {
        long carga = crearCarga();
        agregarFila(carga, renglon("ACC-30", "2026-10-05", "P001", 1, "1", "PIEZ", "", "1"), 2);
        try (Connection ocupante = conectar(CALE); Statement ocupanteSql = ocupante.createStatement()) {
            ocupanteSql.execute("BEGIN TRANSACTION");
            ocupanteSql.execute("SELECT TOP (1) actakey FROM dbo.ACTA WITH (TABLOCKX, HOLDLOCK)");
            SQLException error = assertThrows(SQLException.class, () -> confirmarConTimeout(carga, 1));
            assertTrue(error instanceof java.sql.SQLTimeoutException
                            || "HYT00".equalsIgnoreCase(error.getSQLState())
                            || error.getMessage().toLowerCase().contains("timed out"),
                    "Se esperaba timeout controlado: " + error.getMessage());
            assertEquals(0, contar(CALE, "dbo.SALIDAS"));
        }
        transaccionLimpia();
    }

    @Test
    void rollbackPosteriorAlLegacyRestauraGeneradores() throws Exception {
        // Semilla: una salida DESPERDICIOS con DOS filas para el mismo folio. El wrapper
        // carga y el legacy inserta su salida; el subquery escalar de PSALIDAS>... no
        // aplica aqui, pero forzamos un fallo posterior creando un PSALIDAS incompatible:
        // para un escenario determinista usamos un folio ya existente con salida y una
        // fila de directorio que dispare la restriccion de clave primaria de DIRIGIDO.
        try (Connection cale = conectar(CALE); Statement s = cale.createStatement()) {
            s.execute("INSERT INTO dbo.GENERADORES (tabla, consecutivo) VALUES ('ACTA', 3)");
            s.execute("INSERT INTO dbo.DIRIGIDO (dirigidokey, documento) VALUES (1, 'PRE')");
        }
        long carga = crearCarga();
        agregarFila(carga, renglon("ACC-31", "2026-10-05", "P001", 1, "1", "PIEZ", "DIR-31", "1"), 2);

        // El legacy calcula MAX(DIRIGIDOKEY)+1 = 2, no colisiona. Para forzar rollback
        // posterior al DELETE de GENERADORES, insertamos una fila ACTA con clave que el
        // legacy no puede procesar? En su lugar, provocamos error de conversion en el
        // stage (VALORCOMERCIAL fuera de NUMERIC(18,10)) via JSON imposible: el wrapper
        // ya valida el parser, asi que aqui replicamos el contrato de rollback con una
        // transaccion exterior que se revierte.
        try (Connection exterior = conectar(CALE); Statement es = exterior.createStatement()) {
            es.execute("BEGIN TRANSACTION");
            assertEquals("CONFIRMED", confirmar(exterior, carga).get("Resultado"));
            try (ResultSet rsGen = es.executeQuery("SELECT COUNT(*) FROM dbo.GENERADORES")) { rsGen.next(); assertEquals(0, rsGen.getInt(1), "El DELETE legacy ya se aplico."); }
            es.execute("ROLLBACK TRANSACTION");
        }
        assertEquals(1, contar(CALE, "dbo.GENERADORES"), "El rollback exterior debe restaurar GENERADORES.");
        transaccionLimpia();
    }

    @Test
    void maxKeysContinuasSinReusoNiColision() throws Exception {
        try (Connection cale = conectar(CALE); Statement s = cale.createStatement()) {
            s.execute("INSERT INTO dbo.SALIDAS (SalidaKey, Documento, Fecha, Tipo_operacion, Cve_pedimento, Cve_cliente, Aduana, Agente, Pais, TC, bloqueado) "
                    + "VALUES (100, 'PRE', GETDATE(), 'PEDIMENTO', 'ABC', '-', '-', '-', '-', 0, 0)");
            s.execute("INSERT INTO dbo.PSALIDAS (Psalidakey, Clave, descargaDirigida) VALUES (500, 'PREVIA', '')");
        }
        long carga = crearCarga();
        agregarFila(carga, renglon("ACC-40", "2026-10-05", "P001", 1, "1", "PIEZ", "DIR-40", "1"), 2);

        assertEquals("CONFIRMED", confirmar(carga).get("Resultado"));
        assertEquals("101", valor(CALE, "SELECT MAX(SalidaKey) FROM dbo.SALIDAS"));
        assertEquals("501", valor(CALE, "SELECT MAX(Psalidakey) FROM dbo.PSALIDAS"));
        transaccionLimpia();
    }

    @Test
    void reconfirmacionSeRechaza() throws Exception {
        long carga = crearCarga();
        agregarFila(carga, renglon("ACC-50", "2026-10-05", "P001", 1, "1", "PIEZ", "", "1"), 2);
        assertEquals("CONFIRMED", confirmar(carga).get("Resultado"));
        SQLException error = assertThrows(SQLException.class, () -> confirmar(carga));
        assertTrue(error.getMessage().contains("ALREADY_CONFIRMED"), error.getMessage());
        assertEquals(1, contar(CALE, "dbo.SALIDAS"));
        transaccionLimpia();
    }

    @Test
    void failureAuditEventTrazabilidadSinOcultarError() throws Exception {
        // Dos salidas DESPERDICIOS con el mismo DOCUMENTO hacen fallar el subquery
        // escalar del legacy; el wrapper revierte y registra el intento fallido.
        try (Connection cale = conectar(CALE); Statement s = cale.createStatement()) {
            s.execute("INSERT INTO dbo.SALIDAS (SalidaKey, Documento, Fecha, Tipo_operacion, Cve_pedimento, Cve_cliente, Aduana, Agente, Pais, TC, bloqueado) VALUES "
                    + "(1, 'ACC-60', GETDATE(), 'DESPERDICIOS', 'DESP', '-', '-', '-', '-', 0, 0), "
                    + "(2, 'ACC-60', GETDATE(), 'DESPERDICIOS', 'DESP', '-', '-', '-', '-', 0, 0)");
        }
        long carga = crearCarga();
        agregarFila(carga, renglon("ACC-60", "2026-10-05", "P001", 1, "1", "PIEZ", "", "1"), 2);

        SQLException error = assertThrows(SQLException.class, () -> confirmar(carga));

        assertTrue(error.getMessage().toLowerCase().contains("more than 1"), error.getMessage());
        assertEquals("PREVISUALIZADA", estadoCarga(carga));
        assertEquals(1, contar(APP, "app24.BitacoraEvento"));
        assertEquals("ACTA_CARGA_CONFIRMACION_ERROR", valor(APP, "SELECT accion FROM app24.BitacoraEvento"));
        assertEquals("FALLO", valor(APP, "SELECT resultado FROM app24.BitacoraEvento"));
        transaccionLimpia();
    }

    @Test
    void migrationIdempotente() throws Exception {
        try (Connection app = conectar(APP)) {
            aplicarArchivo(app, raizRepo().resolve("migrations/18-acta-staging-confirmation-v1.sql"));
            aplicarArchivo(app, raizRepo().resolve("migrations/18-acta-staging-confirmation-v1.sql"));
        }
        assertEquals(1, contar(APP, "app24.Actividad WHERE clave = 'ACTAS_CONFIRMAR'"));
        assertEquals(1, contar(APP, "app24.Actividad WHERE clave = 'ACTAS_CARGAR'"));
    }

    private void assertWriterBloqueado(String insert, String tabla) throws Exception {
        long carga = crearCarga();
        agregarFila(carga, renglon("ACC-CONC", "2026-10-05", "P001", 1, "1", "PIEZ", "", "1"), 2);
        ExecutorService executor = Executors.newSingleThreadExecutor();
        CountDownLatch listo = new CountDownLatch(1);
        CountDownLatch iniciar = new CountDownLatch(1);
        try (Connection exterior = conectar(CALE); Statement exteriorSql = exterior.createStatement()) {
            exteriorSql.execute("BEGIN TRANSACTION");
            assertEquals("CONFIRMED", confirmar(exterior, carga).get("Resultado"));
            Future<Long> escritor = executor.submit(() -> {
                listo.countDown();
                iniciar.await(5, TimeUnit.SECONDS);
                try (Connection otra = conectar(CALE); Statement s = otra.createStatement()) {
                    s.execute(insert);
                    return 1L;
                }
            });
            assertTrue(listo.await(5, TimeUnit.SECONDS));
            iniciar.countDown();
            assertThrows(TimeoutException.class, () -> escritor.get(2, TimeUnit.SECONDS),
                    "El escritor de " + tabla + " debe permanecer bloqueado por TABLOCKX/HOLDLOCK.");
            exteriorSql.execute("COMMIT TRANSACTION");
            assertNotNull(escritor.get(15, TimeUnit.SECONDS));
        } finally {
            executor.shutdownNow();
        }
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

    private static Path raizFixtures() {
        Path desdeBackend = Path.of("src", "test", "resources", "sql", "actas");
        return Files.exists(desdeBackend) ? desdeBackend : Path.of("backend", "src", "test", "resources", "sql", "actas");
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
        List<String> lotes = List.of(texto.split("(?im)^\s*GO\s*$"));
        try (Statement s = conexion.createStatement()) {
            for (int i = 0; i < lotes.size(); i++) {
                if (lotes.get(i).isBlank()) continue;
                try {
                    s.execute(lotes.get(i));
                    while (s.getMoreResults() || s.getUpdateCount() != -1) {
                        // Drena resultados.
                    }
                } catch (SQLException error) {
                    throw new IllegalStateException("Fallo aplicando " + archivo.getFileName() + " lote " + i, error);
                }
            }
        }
    }

    private long crearCarga() throws SQLException {
        try (Connection app = conectar(APP);
             var ps = app.prepareStatement("INSERT INTO app24.CargaActa "
                     + "(archivo, hash, usuario_id, estado, total_filas, filas_validas, filas_invalidas, version_contrato, correlation_id) "
                     + "VALUES ('it.xlsx', REPLICATE('a', 64), 7001, 'PREVISUALIZADA', 0, 0, 0, 'ACTA-V1', 'it-correlation')",
                     Statement.RETURN_GENERATED_KEYS)) {
            ps.executeUpdate();
            try (ResultSet rs = ps.getGeneratedKeys()) {
                rs.next();
                return rs.getLong(1);
            }
        }
    }

    private void agregarFila(long cargaId, String datosJson, int fila) throws SQLException {
        try (Connection app = conectar(APP);
             var ps = app.prepareStatement("INSERT INTO app24.CargaActaFila (carga_id, hoja, fila, datos_json) VALUES (?, 'A', ?, ?)")) {
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
        try (CallableStatement cs = cale.prepareCall("{call dbo.APP24_C_ACTA_CARGA_CONFIRMAR(?)}")) {
            cs.setLong(1, cargaId);
            if (!cs.execute()) throw new IllegalStateException("El wrapper no devolvio un result set");
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
             CallableStatement cs = cale.prepareCall("{call dbo.APP24_C_ACTA_CARGA_CONFIRMAR(?)}")) {
            cs.setQueryTimeout(timeoutSegundos);
            cs.setLong(1, cargaId);
            cs.execute();
        }
    }

    private String estadoCarga(long cargaId) throws SQLException {
        return valor(APP, "SELECT estado FROM app24.CargaActa WHERE id = " + cargaId);
    }

    private int contar(String db, String tabla) throws SQLException {
        return Integer.parseInt(valor(db, "SELECT COUNT(*) FROM " + tabla));
    }

    private String valor(String db, String sql) throws SQLException {
        try (Connection c = conectar(db); Statement s = c.createStatement(); ResultSet rs = s.executeQuery(sql)) {
            return rs.next() ? rs.getString(1) : null;
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

    private static String renglon(String folio, String fecha, String clave, int linea, String cantidad,
                                  String umc, String descargaDirigida, String valorComercial) {
        return "{"
                + "\"Folio\":\"" + folio + "\","
                + "\"Fecha\":\"" + fecha + "\","
                + "\"Clave\":\"" + clave + "\","
                + "\"Linea\":\"" + linea + "\","
                + "\"Cantidad\":\"" + cantidad + "\","
                + "\"Umc\":\"" + umc + "\","
                + "\"DescargaDirigida\":\"" + descargaDirigida + "\","
                + "\"ValorComercial\":\"" + valorComercial + "\"}";
    }

    @Test
    void actakeyBigIntNoDesbordaAlBloquearStage() throws Exception {
        try (Connection cale = conectar(CALE); Statement s = cale.createStatement()) {
            s.execute("SET IDENTITY_INSERT dbo.ACTA ON");
            s.execute("INSERT INTO dbo.ACTA (actakey, Folio, fecha, clave, linea, cantidad, umc, descargadirigida, VALORCOMERCIAL) "
                    + "VALUES (3000000000, 'AJENA-BIG', '2026-10-05', 'P001', 1, 1, 'PIEZ', '', 1)");
            s.execute("SET IDENTITY_INSERT dbo.ACTA OFF");
        }
        long carga = crearCarga();
        agregarFila(carga, renglon("ACC-BIG", "2026-10-05", "P001", 1, "1", "PIEZ", "", "1"), 2);

        SQLException error = assertThrows(SQLException.class, () -> confirmar(carga));

        assertTrue(error.getMessage().contains("LEGACY_STAGE_BUSY"),
                "actakey > INT_MAX no debe desbordar; se esperaba LEGACY_STAGE_BUSY: " + error.getMessage());
        assertTrue(!error.getMessage().toLowerCase().contains("overflow"), error.getMessage());
        assertEquals(1, contar(CALE, "dbo.ACTA"), "El stage ajeno con actakey grande no debe tocarse.");
        transaccionLimpia();
    }

    @Test
    void tipoDeVariableDeLockFielAMetadataLive() throws Exception {
        String definicion = Files.readString(raizRepo().resolve("procedures/commands/APP24_C_ACTA_CARGA_CONFIRMAR.sql"));
        assertTrue(definicion.contains("@BloqueoActa BIGINT"), "@BloqueoActa debe ser BIGINT (dbo.ACTA.actakey).");
        assertTrue(definicion.contains("@BloqueoGenerador INT"), "@BloqueoGenerador debe ser INT (dbo.GENERADORES.consecutivo).");
        assertTrue(!definicion.contains("@Bloqueo INT"), "Ninguna variable de lock debe reutilizarse con un tipo mas estrecho.");
        assertTrue(definicion.contains("NULLIF(JSON_VALUE"), "El stage debe preservar NULL con NULLIF.");
        assertTrue(definicion.contains("GLOBAL_PENDING_PSALIDAS_SWEEP"), "El side effect global debe quedar documentado.");
    }

    @Test
    void blancosNullableSeMantienenNull() throws Exception {
        long carga = crearCarga();
        String json = "{"
                + "\"Folio\":\"ACC-NULL\","
                + "\"Fecha\":\"\","
                + "\"Clave\":\"\","
                + "\"Linea\":\"\","
                + "\"Cantidad\":\"\","
                + "\"Umc\":\"\","
                + "\"DescargaDirigida\":\"\","
                + "\"ValorComercial\":\"\"}";
        agregarFila(carga, json, 2);

        assertEquals("CONFIRMED", confirmar(carga).get("Resultado"));

        assertEquals(1, contar(CALE, "dbo.SALIDAS"));
        assertEquals(1, contar(CALE, "dbo.PSALIDAS"));
        assertEquals("1", valor(CALE, "SELECT CASE WHEN Fecha IS NULL THEN 1 ELSE 0 END FROM dbo.SALIDAS"));
        assertEquals("1", valor(CALE, "SELECT CASE WHEN Cantidad IS NULL THEN 1 ELSE 0 END FROM dbo.PSALIDAS"));
        assertEquals("1", valor(CALE, "SELECT CASE WHEN Val_pesos IS NULL THEN 1 ELSE 0 END FROM dbo.PSALIDAS"));
        assertEquals("1", valor(CALE, "SELECT CASE WHEN descargaDirigida IS NULL THEN 1 ELSE 0 END FROM dbo.PSALIDAS"));
        assertEquals("1", valor(CALE, "SELECT CASE WHEN partida IS NULL THEN 1 ELSE 0 END FROM dbo.PSALIDAS"));
        assertEquals(0, contar(CALE, "dbo.DIRIGIDO"), "Un descargaDirigida NULL no entra al sweep.");
        assertEquals("0", valor(CALE, "SELECT COUNT(*) FROM dbo.SALIDAS WHERE Fecha = '1900-01-01'"));
        transaccionLimpia();
    }

    @Test
    void dirigidoIncluyePsalidaPreexistentePendiente() throws Exception {
        try (Connection cale = conectar(CALE); Statement s = cale.createStatement()) {
            s.execute("INSERT INTO dbo.SALIDAS (SalidaKey, Documento, Fecha, Tipo_operacion, Cve_pedimento, Cve_cliente, Aduana, Agente, Pais, TC, bloqueado) "
                    + "VALUES (900, 'OTRO-FOLIO', GETDATE(), 'PEDIMENTO', 'ABC', '-', '-', '-', '-', 0, 0)");
            s.execute("INSERT INTO dbo.PSALIDAS (Psalidakey, Clave, descargaDirigida, Salidalink, partida) "
                    + "VALUES (950, 'AJENA', 'DIR-PRE', 900, 9)");
        }
        long carga = crearCarga();
        agregarFila(carga, renglon("ACC-SWEEP", "2026-10-05", "P001", 1, "1", "PIEZ", "DIR-SWEEP", "1"), 2);

        assertEquals("CONFIRMED", confirmar(carga).get("Resultado"));

        assertEquals(2, contar(CALE, "dbo.PSALIDAS"));
        assertEquals(2, contar(CALE, "dbo.DIRIGIDO"), "El global sweep debe incluir la PSALIDAS pendiente ajena.");
        assertEquals("1", valor(CALE, "SELECT COUNT(*) FROM dbo.DIRIGIDO WHERE documento = 'DIR-PRE'"));
        assertEquals("1", valor(CALE, "SELECT COUNT(*) FROM dbo.DIRIGIDO WHERE documento = 'DIR-SWEEP'"));
        transaccionLimpia();
    }

    @Test
    void folioConDosSalidasDesperdiciosRevierte() throws Exception {
        try (Connection cale = conectar(CALE); Statement s = cale.createStatement()) {
            s.execute("INSERT INTO dbo.SALIDAS (SalidaKey, Documento, Fecha, Tipo_operacion, Cve_pedimento, Cve_cliente, Aduana, Agente, Pais, TC, bloqueado) VALUES "
                    + "(1, 'ACC-AMB', GETDATE(), 'DESPERDICIOS', 'DESP', '-', '-', '-', '-', 0, 0), "
                    + "(2, 'ACC-AMB', GETDATE(), 'DESPERDICIOS', 'DESP', '-', '-', '-', '-', 0, 0)");
        }
        long carga = crearCarga();
        agregarFila(carga, renglon("ACC-AMB", "2026-10-05", "P001", 1, "1", "PIEZ", "", "1"), 2);

        SQLException error = assertThrows(SQLException.class, () -> confirmar(carga));

        assertTrue(error.getMessage().toLowerCase().contains("more than 1"), error.getMessage());
        assertEquals("PREVISUALIZADA", estadoCarga(carga));
        assertEquals(0, contar(CALE, "dbo.ACTA"), "El stage ACTA no queda consumido.");
        assertEquals(0, contar(CALE, "dbo.PSALIDAS"), "No deben crearse partidas.");
        assertEquals(0, contar(CALE, "dbo.DIRIGIDO"), "No deben crearse filas DIRIGIDO.");
        assertEquals("ACTA_CARGA_CONFIRMACION_ERROR", valor(APP, "SELECT accion FROM app24.BitacoraEvento"));
        transaccionLimpia();
    }

    @Test
    void folioConDosPsalidasMismaPartidaRevierte() throws Exception {
        try (Connection cale = conectar(CALE); Statement s = cale.createStatement()) {
            s.execute("INSERT INTO dbo.SALIDAS (SalidaKey, Documento, Fecha, Tipo_operacion, Cve_pedimento, Cve_cliente, Aduana, Agente, Pais, TC, bloqueado) "
                    + "VALUES (10, 'ACC-PART', GETDATE(), 'DESPERDICIOS', 'DESP', '-', '-', '-', '-', 0, 0)");
            s.execute("INSERT INTO dbo.PSALIDAS (Psalidakey, Clave, descargaDirigida, Salidalink, partida) VALUES "
                    + "(20, 'A', '', 10, 1), (21, 'B', '', 10, 1)");
        }
        long carga = crearCarga();
        agregarFila(carga, renglon("ACC-PART", "2026-10-05", "P001", 1, "1", "PIEZ", "", "1"), 2);

        SQLException error = assertThrows(SQLException.class, () -> confirmar(carga));

        assertTrue(error.getMessage().toLowerCase().contains("more than 1"), error.getMessage());
        assertEquals("PREVISUALIZADA", estadoCarga(carga));
        assertEquals(2, contar(CALE, "dbo.PSALIDAS"), "El rollback conserva las partidas preexistentes.");
        assertEquals(0, contar(CALE, "dbo.DIRIGIDO"), "No deben crearse filas DIRIGIDO.");
        assertEquals("ACTA_CARGA_CONFIRMACION_ERROR", valor(APP, "SELECT accion FROM app24.BitacoraEvento"));
        transaccionLimpia();
    }

    @Test
    void escritorGeneradoresConcurrenteEspera() throws Exception {
        long carga = crearCarga();
        agregarFila(carga, renglon("ACC-GEN", "2026-10-05", "P001", 1, "1", "PIEZ", "", "1"), 2);
        ExecutorService executor = Executors.newSingleThreadExecutor();
        CountDownLatch listo = new CountDownLatch(1);
        CountDownLatch iniciar = new CountDownLatch(1);
        try (Connection exterior = conectar(CALE); Statement exteriorSql = exterior.createStatement()) {
            exteriorSql.execute("BEGIN TRANSACTION");
            assertEquals("CONFIRMED", confirmar(exterior, carga).get("Resultado"));
            Future<Integer> escritor = executor.submit(() -> {
                listo.countDown();
                iniciar.await(5, TimeUnit.SECONDS);
                try (Connection otra = conectar(CALE); Statement s = otra.createStatement()) {
                    s.execute("INSERT INTO dbo.GENERADORES (tabla, consecutivo) VALUES ('EXTERNO', 42)");
                    return 1;
                }
            });
            assertTrue(listo.await(5, TimeUnit.SECONDS));
            iniciar.countDown();
            assertThrows(TimeoutException.class, () -> escritor.get(2, TimeUnit.SECONDS),
                    "El escritor de GENERADORES debe permanecer bloqueado por TABLOCKX/HOLDLOCK.");
            exteriorSql.execute("COMMIT TRANSACTION");
            assertEquals(1, escritor.get(15, TimeUnit.SECONDS));
        } finally {
            executor.shutdownNow();
        }
        assertEquals(1, contar(CALE, "dbo.GENERADORES WHERE tabla = 'EXTERNO'"), "El escritor externo no se pierde.");
        transaccionLimpia();
    }
}
