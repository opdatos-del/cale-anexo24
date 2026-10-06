package com.jovycandy.anexo24.operations.constancias;

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
 * Arnes de CONSTANCIAS contra SQL Server Testcontainers (nunca LIVE). La fixture
 * CARGACONSTANCIAS.legacy.sql es copia textual fiel del OBJECT_DEFINITION LIVE 2026-10-06.
 */
class ConstanciaLegacyStageConcurrencyTest {

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
            throw new IllegalStateException("Docker no disponible en CI: el arnes SQL de constancias es obligatorio.");
        }
        Assumptions.assumeTrue(disponible, "Docker no disponible localmente; se omite el arnes SQL.");
        SQL.start();
        crearBases();
        try (Connection cale = conectar(CALE)) {
            aplicarArchivo(cale, raizFixtures().resolve("01-constancias-live-schema.sql"));
            aplicarArchivo(cale, raizFixtures().resolve("CARGACONSTANCIAS.legacy.sql"));
        }
        aplicarArchivo(conectar(APP), raizRepo().resolve("migrations/19-constancia-staging-confirmation-v1.sql"));
        aplicarArchivo(conectar(CALE), raizRepo().resolve("procedures/commands/APP24_C_CONSTANCIA_CARGA_CONFIRMAR.sql"));
    }

    @AfterAll
    static void detenerContenedor() {
        if (disponible) SQL.stop();
    }

    @BeforeEach
    void limpiarDatos() throws Exception {
        try (Connection cale = conectar(CALE); Statement s = cale.createStatement()) {
            s.execute("DELETE FROM dbo.Constanciatransf");
            s.execute("DELETE FROM dbo.errorcarga");
            s.execute("DELETE FROM dbo.dirigido");
            s.execute("DELETE FROM dbo.psalidas");
            s.execute("DELETE FROM dbo.salidas");
            s.execute("DELETE FROM dbo.productos");
            s.execute("DELETE FROM dbo.generadores");
            s.execute("DELETE FROM dbo.settings");
        }
        try (Connection app = conectar(APP); Statement s = app.createStatement()) {
            s.execute("DELETE FROM app24.ErrorCargaConstancia");
            s.execute("DELETE FROM app24.CargaConstanciaFila");
            s.execute("DELETE FROM app24.CargaConstancia");
            s.execute("DELETE FROM app24.BitacoraEvento");
        }
    }

    @Test
    void legacyLiveContractEsEjecutable() throws Exception {
        try (Connection cale = conectar(CALE); Statement s = cale.createStatement()) {
            s.execute("INSERT INTO dbo.productos (PRODUCTOKEY, CVE_PRODUCTO, NOMBRE, UNIDAD, fraccion) "
                    + "VALUES (1, 'P001', 'Producto uno', 'PZA', '00000000')");
            s.execute("INSERT INTO dbo.Constanciatransf (NUMERODEFOLIO, SEC, FECHACREACION, PROV, LIN, NOPARTE, "
                    + "DESCRIPCION, CANTIDAD, PEDIMENTO, ADUANA, MER, PERIODO, Val_dolares, Val_Comercial) "
                    + "VALUES ('CONST-1', '1', '2026-10-06', 'PROV-1', 1, 'P001', 'Producto uno', 5, 'PED-1', 'ADU', 'M', "
                    + "'2026-10-06', 10, 20)");
            s.execute("EXEC dbo.CARGACONSTANCIAS");
            assertEquals(1, contar(CALE, "dbo.salidas"));
            assertEquals(1, contar(CALE, "dbo.psalidas"));
            assertEquals(0, contar(CALE, "dbo.errorcarga"));
            assertEquals(0, contar(CALE, "dbo.dirigido"));
        }
    }

    @Test
    void fixtureEsCopiaFielDelContratoLive() throws Exception {
        String definicion = Files.readString(raizFixtures().resolve("CARGACONSTANCIAS.legacy.sql"));
        assertTrue(definicion.contains("LEGACY_EXECUTION_SAFE = YES"));
        assertTrue(definicion.contains("DELETE FROM ERRORCARGA"));
        assertTrue(definicion.contains("I_CARGAPRODUCTOSCONST"));
        assertTrue(definicion.contains("DELETE FROM GENERADORES"));
        assertTrue(definicion.contains("MAX(SALIDAKEY)"));
        assertTrue(definicion.contains("UPDATE SALIDAS"));
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
        Path desdeBackend = Path.of("src", "test", "resources", "sql", "constancias");
        return Files.exists(desdeBackend) ? desdeBackend : Path.of("backend", "src", "test", "resources", "sql", "constancias");
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

    private void sembrarProducto(String cve) throws SQLException {
        try (Connection cale = conectar(CALE); Statement s = cale.createStatement()) {
            s.execute("INSERT INTO dbo.productos (PRODUCTOKEY, CVE_PRODUCTO, NOMBRE, UNIDAD, fraccion) "
                    + "SELECT ISNULL(MAX(PRODUCTOKEY),0)+1, '" + cve + "', 'Producto " + cve + "', 'PZA', '00000000' FROM dbo.productos");
        }
    }

    private void sembrarSetting(String valor) throws SQLException {
        try (Connection cale = conectar(CALE); Statement s = cale.createStatement()) {
            s.execute("INSERT INTO dbo.settings (settings, value) VALUES ('I_CARGAPRODUCTOSCONST', '" + valor + "')");
        }
    }

    private long crearCarga() throws SQLException {
        try (Connection app = conectar(APP);
             var ps = app.prepareStatement("INSERT INTO app24.CargaConstancia "
                     + "(archivo, hash, usuario_id, estado, total_filas, filas_validas, filas_invalidas, version_contrato, correlation_id) "
                     + "VALUES ('it.xlsx', REPLICATE('a', 64), 7001, 'PREVISUALIZADA', 0, 0, 0, 'CONSTANCIA-V1', 'it-correlation')",
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
             var ps = app.prepareStatement("INSERT INTO app24.CargaConstanciaFila (carga_id, hoja, fila, datos_json) VALUES (?, 'A', ?, ?)")) {
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
        try (CallableStatement cs = cale.prepareCall("{call dbo.APP24_C_CONSTANCIA_CARGA_CONFIRMAR(?)}")) {
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
             CallableStatement cs = cale.prepareCall("{call dbo.APP24_C_CONSTANCIA_CARGA_CONFIRMAR(?)}")) {
            cs.setQueryTimeout(timeoutSegundos);
            cs.setLong(1, cargaId);
            cs.execute();
        }
    }

    private String estadoCarga(long cargaId) throws SQLException {
        return valor(APP, "SELECT estado FROM app24.CargaConstancia WHERE id = " + cargaId);
    }

    private void assertWriterBloqueado(String insert, String tabla, String cveProducto) throws Exception {
        if (cveProducto != null) sembrarProducto(cveProducto);
        long carga = crearCarga();
        agregarFila(carga, renglon("CONST-CONC", "1", "2026-10-06", "PROV-1", 1, cveProducto, "Producto", "5", "PED-1", "ADU", "M", "2026-10-06", "10", "20"), 2);
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

    private static String filaValida(String noparte) {
        return renglon("CONST-FOLIO", "1", "2026-10-06", "PROV-1", 1, noparte, "Producto", "5", "PED-1", "ADU", "M", "2026-10-06", "10", "20");
    }

    private static String renglon(String folio, String sec, String fecha, String prov, int lin, String noparte,
                                  String descripcion, String cantidad, String pedimento, String aduana,
                                  String mer, String periodo, String valDolares, String valComercial) {
        return "{"
                + "\"NUMERODEFOLIO\":\"" + folio + "\","
                + "\"SEC\":\"" + sec + "\","
                + "\"FECHACREACION\":\"" + fecha + "\","
                + "\"PROV\":\"" + prov + "\","
                + "\"LIN\":\"" + lin + "\","
                + "\"NOPARTE\":\"" + noparte + "\","
                + "\"DESCRIPCION\":\"" + descripcion + "\","
                + "\"CANTIDAD\":\"" + cantidad + "\","
                + "\"PEDIMENTO\":\"" + pedimento + "\","
                + "\"ADUANA\":\"" + aduana + "\","
                + "\"MER\":\"" + mer + "\","
                + "\"PERIODO\":\"" + periodo + "\","
                + "\"Val_dolares\":\"" + valDolares + "\","
                + "\"Val_Comercial\":\"" + valComercial + "\"}";
    }

    @Test
    void confirmacionValidaPorDefectoSICreaProductoSalidaYPartida() throws Exception {
        long carga = crearCarga();
        agregarFila(carga, filaValida("NP-NUEVO"), 2);

        Map<String, String> resultado = confirmar(carga);

        assertEquals("CONFIRMED", resultado.get("Resultado"));
        assertEquals("CONFIRMADA", estadoCarga(carga));
        assertEquals(1, contar(CALE, "dbo.salidas"));
        assertEquals(1, contar(CALE, "dbo.psalidas"));
        assertEquals(0, contar(CALE, "dbo.errorcarga"));
        assertEquals("1", valor(CALE, "SELECT COUNT(*) FROM dbo.productos WHERE CVE_PRODUCTO = 'NP-NUEVO'"));
        assertEquals(0, contar(CALE, "dbo.Constanciatransf"));
        assertEquals("CONSTANCIA_CARGA_CONFIRMADA", valor(APP, "SELECT accion FROM app24.BitacoraEvento"));
        transaccionLimpia();
    }

    @Test
    void settingNoConProductoNuevoNormalizaErrorYRevierte() throws Exception {
        sembrarSetting("NO");
        long carga = crearCarga();
        agregarFila(carga, filaValida("NP-X"), 2);

        Map<String, String> resultado = confirmar(carga);

        assertEquals("BUSINESS_ERRORS", resultado.get("Resultado"));
        assertEquals("CON_ERRORES", estadoCarga(carga));
        assertEquals(0, contar(CALE, "dbo.salidas"));
        assertEquals(0, contar(CALE, "dbo.psalidas"));
        assertEquals(0, contar(CALE, "dbo.errorcarga"));
        assertEquals(0, contar(CALE, "dbo.Constanciatransf"));
        assertEquals("0", valor(CALE, "SELECT COUNT(*) FROM dbo.productos"));
        assertEquals(1, contar(APP, "app24.ErrorCargaConstancia"));
        assertEquals("CONSTANCIA_CARGA_CONFIRMACION_ERROR", valor(APP, "SELECT accion FROM app24.BitacoraEvento"));
        transaccionLimpia();
    }

    @Test
    void settingNoConProductoExistenteConfirma() throws Exception {
        sembrarSetting("NO");
        sembrarProducto("P001");
        long carga = crearCarga();
        agregarFila(carga, filaValida("P001"), 2);

        assertEquals("CONFIRMED", confirmar(carga).get("Resultado"));
        assertEquals("CONFIRMADA", estadoCarga(carga));
        assertEquals(1, contar(CALE, "dbo.salidas"));
        assertEquals("1", valor(CALE, "SELECT COUNT(*) FROM dbo.productos"));
        transaccionLimpia();
    }

    @Test
    void errorStageCompartidoPreexistenteFallaCerradoYPreserva() throws Exception {
        try (Connection cale = conectar(CALE); Statement s = cale.createStatement()) {
            s.execute("INSERT INTO dbo.errorcarga (error, cargakey) VALUES ('ERROR_PEDIMENTO_AJENO', 999)");
        }
        long carga = crearCarga();
        agregarFila(carga, filaValida("NP-NUEVO"), 2);

        SQLException error = assertThrows(SQLException.class, () -> confirmar(carga));

        assertTrue(error.getMessage().contains("LEGACY_ERROR_STAGE_BUSY"), error.getMessage());
        assertEquals(1, contar(CALE, "dbo.errorcarga"));
        assertEquals("ERROR_PEDIMENTO_AJENO", valor(CALE, "SELECT TOP 1 error FROM dbo.errorcarga"));
        assertEquals(0, contar(CALE, "dbo.salidas"));
        assertEquals("PREVISUALIZADA", estadoCarga(carga));
        transaccionLimpia();
    }

    @Test
    void stageOcupadoFallaCerrado() throws Exception {
        try (Connection cale = conectar(CALE); Statement s = cale.createStatement()) {
            s.execute("INSERT INTO dbo.Constanciatransf (NUMERODEFOLIO) VALUES ('AJENA')");
        }
        long carga = crearCarga();
        agregarFila(carga, filaValida("NP-NUEVO"), 2);

        SQLException error = assertThrows(SQLException.class, () -> confirmar(carga));

        assertTrue(error.getMessage().contains("LEGACY_STAGE_BUSY"), error.getMessage());
        assertEquals(1, contar(CALE, "dbo.Constanciatransf"));
        assertEquals(0, contar(CALE, "dbo.salidas"));
        transaccionLimpia();
    }

    @Test
    void cargaInexistenteSeRechaza() throws Exception {
        SQLException error = assertThrows(SQLException.class, () -> confirmar(987654321L));
        assertTrue(error.getMessage().contains("CARGA_NO_ENCONTRADA"), error.getMessage());
        transaccionLimpia();
    }

    @Test
    void cargaSinFilasNoProcesa() throws Exception {
        long carga = crearCarga();
        SQLException error = assertThrows(SQLException.class, () -> confirmar(carga));
        assertTrue(error.getMessage().contains("CARGA_SIN_FILAS"), error.getMessage());
        assertEquals(0, contar(CALE, "dbo.salidas"));
        transaccionLimpia();
    }

    @Test
    void cargaConErroresParserNoProcesa() throws Exception {
        long carga = crearCarga();
        try (Connection app = conectar(APP); Statement s = app.createStatement()) {
            s.execute("INSERT INTO app24.ErrorCargaConstancia (carga_id, hoja, fila, columna, valor_enmascarado, codigo, mensaje) "
                    + "VALUES (" + carga + ", 'A', 2, 'NOPARTE', 'no almacenado', 'NOPARTE_CONSTANCIA_VACIA', 'La clave es obligatoria.')");
        }
        SQLException error = assertThrows(SQLException.class, () -> confirmar(carga));
        assertTrue(error.getMessage().contains("CARGA_CON_ERRORES"), error.getMessage());
        assertEquals(0, contar(CALE, "dbo.salidas"));
        transaccionLimpia();
    }

    @Test
    void reconfirmacionSeRechaza() throws Exception {
        long carga = crearCarga();
        agregarFila(carga, filaValida("NP-NUEVO"), 2);
        assertEquals("CONFIRMED", confirmar(carga).get("Resultado"));
        SQLException error = assertThrows(SQLException.class, () -> confirmar(carga));
        assertTrue(error.getMessage().contains("ALREADY_CONFIRMED"), error.getMessage());
        assertEquals(1, contar(CALE, "dbo.salidas"));
        transaccionLimpia();
    }

    @Test
    void errorLegacyNormalizadoYRevierteSinMutacionAutoritativa() throws Exception {
        sembrarProducto("P001");
        long carga = crearCarga();
        agregarFila(carga, renglon("CONST-BAD", "1", "2026-10-06", "", 1, "P001", "Producto", "5", "PED-1", "ADU", "M", "2026-10-06", "10", "20"), 2);

        Map<String, String> resultado = confirmar(carga);

        assertEquals("BUSINESS_ERRORS", resultado.get("Resultado"));
        assertEquals("CON_ERRORES", estadoCarga(carga));
        assertEquals(0, contar(CALE, "dbo.salidas"));
        assertEquals(0, contar(CALE, "dbo.errorcarga"));
        assertEquals(1, contar(APP, "app24.ErrorCargaConstancia"));
        assertTrue(valor(APP, "SELECT mensaje FROM app24.ErrorCargaConstancia").contains("PROVEEDOR"));
        assertEquals("A", valor(APP, "SELECT hoja FROM app24.ErrorCargaConstancia"));
        assertEquals("2", valor(APP, "SELECT fila FROM app24.ErrorCargaConstancia"));
        transaccionLimpia();
    }

    @Test
    void timeoutControladoSinMutacion() throws Exception {
        long carga = crearCarga();
        agregarFila(carga, filaValida("NP-NUEVO"), 2);
        try (Connection ocupante = conectar(CALE); Statement ocupanteSql = ocupante.createStatement()) {
            ocupanteSql.execute("BEGIN TRANSACTION");
            ocupanteSql.execute("SELECT TOP (1) CONSTANCIAKEY FROM dbo.Constanciatransf WITH (TABLOCKX, HOLDLOCK)");
            SQLException error = assertThrows(SQLException.class, () -> confirmarConTimeout(carga, 1));
            assertTrue(error instanceof java.sql.SQLTimeoutException
                            || "HYT00".equalsIgnoreCase(error.getSQLState())
                            || error.getMessage().toLowerCase().contains("timed out"),
                    "Se esperaba timeout controlado: " + error.getMessage());
            assertEquals(0, contar(CALE, "dbo.salidas"));
        }
        transaccionLimpia();
    }

    @Test
    void escritorSalidasConcurrenteEspera() throws Exception {
        assertWriterBloqueado("INSERT INTO dbo.salidas (SalidaKey, Documento, Fecha, Tipo_operacion, Cve_pedimento) "
                + "SELECT ISNULL(MAX(SalidaKey),0)+1, 'EXT', GETDATE(), 'X', 'DESP' FROM dbo.salidas", "dbo.salidas", "P001");
    }

    @Test
    void escritorPsalidasConcurrenteEspera() throws Exception {
        assertWriterBloqueado("INSERT INTO dbo.psalidas (Psalidakey, Clave) "
                + "SELECT ISNULL(MAX(Psalidakey),0)+1, 'EXT' FROM dbo.psalidas", "dbo.psalidas", "P001");
    }

    @Test
    void escritorDirigidoConcurrenteEspera() throws Exception {
        assertWriterBloqueado("INSERT INTO dbo.dirigido (dirigidokey, documento) "
                + "SELECT ISNULL(MAX(dirigidokey),0)+1, 'EXT' FROM dbo.dirigido", "dbo.dirigido", "P001");
    }

    @Test
    void escritorProductosConcurrenteEspera() throws Exception {
        assertWriterBloqueado("INSERT INTO dbo.productos (PRODUCTOKEY, CVE_PRODUCTO) "
                + "SELECT ISNULL(MAX(PRODUCTOKEY),0)+1, 'EXT' FROM dbo.productos", "dbo.productos", "P001");
    }

    @Test
    void escritorErrorCargaConcurrenteEspera() throws Exception {
        assertWriterBloqueado("INSERT INTO dbo.errorcarga (error, cargakey) VALUES ('EXT', 1)", "dbo.errorcarga", "P001");
    }

    @Test
    void escritorGeneradoresConcurrenteEspera() throws Exception {
        assertWriterBloqueado("INSERT INTO dbo.generadores (tabla, consecutivo) VALUES ('EXTERNO', 42)", "dbo.generadores", "P001");
    }

    @Test
    void migrationIdempotente() throws Exception {
        try (Connection app = conectar(APP)) {
            aplicarArchivo(app, raizRepo().resolve("migrations/19-constancia-staging-confirmation-v1.sql"));
            aplicarArchivo(app, raizRepo().resolve("migrations/19-constancia-staging-confirmation-v1.sql"));
        }
        assertEquals(1, contar(APP, "app24.Actividad WHERE clave = 'CONSTANCIAS_CONFIRMAR'"));
        assertEquals(1, contar(APP, "app24.Actividad WHERE clave = 'CONSTANCIAS_CARGAR'"));
    }
}
