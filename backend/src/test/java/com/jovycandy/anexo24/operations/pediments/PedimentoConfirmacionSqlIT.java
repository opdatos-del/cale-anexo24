package com.jovycandy.anexo24.operations.pediments;

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
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Prueba de integración SQL del command autoritativo contra un SQL Server efímero.
 *
 * <p>Aplica el DDL versionado real (migrations 09/11/12, bitácora y
 * {@code APP24_C_PEDIMENTO_CONFIRMAR}); nunca se conecta a LIVE ni escribe datos
 * empresariales.</p>
 */
class PedimentoConfirmacionSqlIT {

    private static final String CALE = "CALE_IMMEX";
    private static final String APP = "ANEXO24_DEV";

    static final MSSQLServerContainer<?> SQL =
            new MSSQLServerContainer<>("mcr.microsoft.com/mssql/server:2022-latest").acceptLicense();

    private static boolean disponible;
    private static long usuarioId;

    @BeforeAll
    static void iniciarContenedor() throws Exception {
        disponible = dockerDisponible();
        if (!disponible && "true".equalsIgnoreCase(System.getenv("CI"))) {
            throw new IllegalStateException("Docker no disponible en CI: los tests SQL de confirmación son "
                    + "obligatorios (CI_SQL_GATE_REQUIRED).");
        }
        Assumptions.assumeTrue(disponible, "Docker no disponible localmente; se omite la prueba SQL.");
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
        try (Connection c = conectar(CALE); Statement s = c.createStatement()) {
            for (String tabla : List.of("DIRIGIDO", "PSALIDAS", "SALIDAS", "PARTIDAS", "IMPORTACIONES", "MATERIAL", "PRODUCTOS"))
                s.execute("DELETE FROM dbo." + tabla);
        }
        try (Connection c = conectar(APP); Statement s = c.createStatement()) {
            s.execute("DELETE FROM app24.ErrorCargaPedimento");
            s.execute("DELETE FROM app24.CargaPedimentoFila");
            s.execute("DELETE FROM app24.CargaPedimento");
            s.execute("DELETE FROM app24.BitacoraEvento");
        }
    }

    // ------------------------------------------------------------------ casos

    @Test
    void confirmaImportacion() throws Exception {
        crearMaterial("MAT-1");
        long carga = crearCarga("LEGACY-STAGE-DERIVED-V2");
        agregarFila(carga, 1, "H", importRow("IMP-0001", "MAT-1", "1"));
        agregarFila(carga, 2, "H", importRow("IMP-0001", "MAT-1", "2"));

        Map<String, String> r = confirmar(carga);

        assertEquals("CONFIRMED", r.get("Resultado"));
        assertEquals("CONFIRMADA", r.get("Estado"));
        assertNotNull(r.get("FechaConfirmacion"));
        assertEquals(1, contar(CALE, "dbo.IMPORTACIONES"));
        assertEquals(2, contar(CALE, "dbo.PARTIDAS"));
        assertEquals("CONFIRMADA", estadoCarga(carga));
        assertEquals(1, contar(APP, "app24.BitacoraEvento"));
        assertEquals("PEDIMENTO_CONFIRMADO", valor(APP, "SELECT TOP 1 accion FROM app24.BitacoraEvento"));
        transaccionLimpia();
    }

    @Test
    void confirmaExportacionConDirigidoCondicional() throws Exception {
        crearProducto("PROD-1");
        long carga = crearCarga("LEGACY-STAGE-DERIVED-V2");
        agregarFila(carga, 1, "H", exportRow("EXP-0001", "PROD-1", "1", null));
        agregarFila(carga, 2, "H", exportRow("EXP-0001", "PROD-1", "2", "D-01"));

        Map<String, String> r = confirmar(carga);

        assertEquals("CONFIRMED", r.get("Resultado"));
        assertEquals(1, contar(CALE, "dbo.SALIDAS"));
        assertEquals(2, contar(CALE, "dbo.PSALIDAS"));
        // Sólo la línea con DescargaDirigida no vacía genera DIRIGIDO.
        assertEquals(1, contar(CALE, "dbo.DIRIGIDO"));
        assertEquals("D-01", valor(CALE, "SELECT TOP 1 DOCUMENTO FROM dbo.DIRIGIDO"));
        transaccionLimpia();
    }

    @Test
    void confirmaCargaMixta() throws Exception {
        crearMaterial("MAT-1");
        crearProducto("PROD-1");
        long carga = crearCarga("LEGACY-STAGE-DERIVED-V2");
        agregarFila(carga, 1, "H", importRow("MIX-IMP-1", "MAT-1", "1"));
        agregarFila(carga, 2, "H", exportRow("MIX-EXP-1", "PROD-1", "1", null));

        Map<String, String> r = confirmar(carga);

        assertEquals("CONFIRMED", r.get("Resultado"));
        assertNull(r.get("TipoOperacion")); // carga mixta
        assertEquals(1, contar(CALE, "dbo.IMPORTACIONES"));
        assertEquals(1, contar(CALE, "dbo.PARTIDAS"));
        assertEquals(1, contar(CALE, "dbo.SALIDAS"));
        assertEquals(1, contar(CALE, "dbo.PSALIDAS"));
        assertEquals(1, contar(APP, "app24.BitacoraEvento"));
        transaccionLimpia();
    }

    @Test
    void agrupaMultiplesDocumentos() throws Exception {
        crearMaterial("MAT-1");
        long carga = crearCarga("LEGACY-STAGE-DERIVED-V2");
        agregarFila(carga, 1, "H", importRow("DOC-A", "MAT-1", "1"));
        agregarFila(carga, 2, "H", importRow("DOC-B", "MAT-1", "1"));

        Map<String, String> r = confirmar(carga);

        assertEquals("CONFIRMED", r.get("Resultado"));
        assertEquals(2, contar(CALE, "dbo.IMPORTACIONES"));
        assertEquals(2, contar(CALE, "dbo.PARTIDAS"));
        assertEquals(2, valorInt(CALE, "SELECT COUNT(DISTINCT IMPORTACIONLINK) FROM dbo.PARTIDAS"));
        transaccionLimpia();
    }

    @Test
    void rollbackAtomicoAnteFalloForzado() throws Exception {
        crearMaterial("MAT-1");
        long carga = crearCarga("LEGACY-STAGE-DERIVED-V2");
        agregarFila(carga, 1, "H", importRow("RB-0001", "MAT-1", "1"));
        agregarFila(carga, 2, "H", importRow("RB-0001", "ROLLBACK-X", "2"));

        SQLException error = assertThrows(SQLException.class, () -> confirmar(carga));

        assertTrue(String.valueOf(error.getMessage()).contains("ROLLBACK_TEST_FORZADO"));
        assertEquals(0, contar(CALE, "dbo.IMPORTACIONES"));
        assertEquals(0, contar(CALE, "dbo.PARTIDAS"));
        assertEquals("PREVISUALIZADA", estadoCarga(carga));
        assertNull(valor(APP, "SELECT TOP 1 fecha_confirmacion FROM app24.CargaPedimento"));
        assertEquals(0, contar(APP, "app24.BitacoraEvento"));
        transaccionLimpia();
    }

    @Test
    void alreadyConfirmedEsIdempotente() throws Exception {
        crearMaterial("MAT-1");
        long carga = crearCarga("LEGACY-STAGE-DERIVED-V2");
        agregarFila(carga, 1, "H", importRow("AC-0001", "MAT-1", "1"));
        assertEquals("CONFIRMED", confirmar(carga).get("Resultado"));

        Map<String, String> segunda = confirmar(carga);

        assertEquals("ALREADY_CONFIRMED", segunda.get("Resultado"));
        assertEquals("CONFIRMADA", segunda.get("Estado"));
        assertEquals(1, contar(CALE, "dbo.IMPORTACIONES"));
        assertEquals(1, contar(CALE, "dbo.PARTIDAS"));
        assertEquals(1, contar(APP, "app24.BitacoraEvento"));
        transaccionLimpia();
    }

    @Test
    void duplicateOtraCargaNoEscribe() throws Exception {
        crearMaterial("MAT-1");
        long cargaA = crearCarga("LEGACY-STAGE-DERIVED-V2");
        agregarFila(cargaA, 1, "H", importRow("DUP-0001", "MAT-1", "1"));
        assertEquals("CONFIRMED", confirmar(cargaA).get("Resultado"));

        long cargaB = crearCarga("LEGACY-STAGE-DERIVED-V2");
        agregarFila(cargaB, 1, "H", importRow("DUP-0001", "MAT-1", "1"));

        SQLException error = assertThrows(SQLException.class, () -> confirmar(cargaB));

        assertTrue(String.valueOf(error.getMessage()).contains("DUPLICATE_OPERATION"));
        assertEquals(1, contar(CALE, "dbo.IMPORTACIONES"));
        assertEquals(1, contar(CALE, "dbo.PARTIDAS"));
        assertEquals("PREVISUALIZADA", estadoCarga(cargaB));
        assertEquals(1, contar(APP, "app24.BitacoraEvento"));
        transaccionLimpia();
    }

    @Test
    void sameLoadConcurrente() throws Exception {
        crearMaterial("MAT-1");
        long carga = crearCarga("LEGACY-STAGE-DERIVED-V2");
        agregarFila(carga, 1, "H", importRow("CONC-0001", "MAT-1", "1"));

        ExecutorService executor = Executors.newFixedThreadPool(2);
        CountDownLatch inicio = new CountDownLatch(1);
        try {
            List<Future<String>> futuros = new ArrayList<>();
            for (int i = 0; i < 2; i++) {
                futuros.add(executor.submit(() -> {
                    inicio.await(5, TimeUnit.SECONDS);
                    return confirmar(carga).get("Resultado");
                }));
            }
            inicio.countDown();
            List<String> resultados = new ArrayList<>();
            for (Future<String> f : futuros) resultados.add(f.get(30, TimeUnit.SECONDS));

            assertEquals(1, resultados.stream().filter("CONFIRMED"::equals).count());
            assertEquals(1, resultados.stream().filter("ALREADY_CONFIRMED"::equals).count());
            assertEquals(1, contar(CALE, "dbo.IMPORTACIONES"));
            assertEquals(1, contar(CALE, "dbo.PARTIDAS"));
            transaccionLimpia();
        } finally {
            executor.shutdownNow();
        }
    }

    @Test
    void materialInexistenteNoEscribe() throws Exception {
        long carga = crearCarga("LEGACY-STAGE-DERIVED-V2");
        agregarFila(carga, 1, "H", importRow("MAT-X-0001", "MAT-NO-EXISTE", "1"));

        SQLException error = assertThrows(SQLException.class, () -> confirmar(carga));

        assertTrue(String.valueOf(error.getMessage()).contains("PED-003"));
        assertEquals(0, contar(CALE, "dbo.IMPORTACIONES"));
        assertEquals("PREVISUALIZADA", estadoCarga(carga));
        transaccionLimpia();
    }

    @Test
    void productoInexistenteNoEscribe() throws Exception {
        long carga = crearCarga("LEGACY-STAGE-DERIVED-V2");
        agregarFila(carga, 1, "H", exportRow("PROD-X-0001", "PROD-NO-EXISTE", "1", null));

        SQLException error = assertThrows(SQLException.class, () -> confirmar(carga));

        assertTrue(String.valueOf(error.getMessage()).contains("PED-004"));
        assertEquals(0, contar(CALE, "dbo.SALIDAS"));
        assertEquals("PREVISUALIZADA", estadoCarga(carga));
        transaccionLimpia();
    }

    @Test
    void stagingV1NoConfirmable() throws Exception {
        crearMaterial("MAT-1");
        long carga = crearCarga("LEGACY-STAGE-DERIVED-V1");
        agregarFila(carga, 1, "H", importRow("V1-0001", "MAT-1", "1"));

        SQLException error = assertThrows(SQLException.class, () -> confirmar(carga));

        assertTrue(String.valueOf(error.getMessage()).contains("STAGING_VERSION_NOT_CONFIRMABLE"));
        assertEquals(0, contar(CALE, "dbo.IMPORTACIONES"));
        transaccionLimpia();
    }

    @Test
    void cargaVaciaNoConfirmable() throws Exception {
        long carga = crearCarga("LEGACY-STAGE-DERIVED-V2");

        SQLException error = assertThrows(SQLException.class, () -> confirmar(carga));

        assertTrue(String.valueOf(error.getMessage()).contains("CARGA_SIN_FILAS"));
        transaccionLimpia();
    }

    @Test
    void cargaConErroresNoConfirmable() throws Exception {
        crearMaterial("MAT-1");
        long carga = crearCarga("LEGACY-STAGE-DERIVED-V2");
        agregarFila(carga, 1, "H", importRow("ERR-0001", "MAT-1", "1"));
        agregarError(carga);

        SQLException error = assertThrows(SQLException.class, () -> confirmar(carga));

        assertTrue(String.valueOf(error.getMessage()).contains("CARGA_CON_ERRORES"));
        assertEquals(0, contar(CALE, "dbo.IMPORTACIONES"));
        transaccionLimpia();
    }

    @Test
    void fiscalNullCeroYDecimalSePreservan() throws Exception {
        crearMaterial("MAT-1");
        long carga = crearCarga("LEGACY-STAGE-DERIVED-V2");
        Map<String, String> fila = importRow("FIS-0001", "MAT-1", "1");
        fila.remove("IGIE");   // propiedad ausente -> null
        fila.put("IVA", "0");  // cero explícito
        fila.put("DTA", "3.5"); // decimal
        fila.put("PREV", null); // null explícito
        agregarFila(carga, 1, "H", fila);

        Map<String, String> r = confirmar(carga);

        assertEquals("CONFIRMED", r.get("Resultado"));
        // PARTIDAS: MONTOIGI desde IGIE (null preservado), MONTOIVA desde IVA (cero).
        assertNull(valor(CALE, "SELECT TOP 1 MONTOIGI FROM dbo.PARTIDAS"));
        assertEquals("0", valor(CALE, "SELECT TOP 1 MONTOIVA FROM dbo.PARTIDAS"));
        // IMPORTACIONES: agregados legacy (SUM/MAX con ISNULL) => 0 para ausentes.
        assertEquals("0", valor(CALE, "SELECT TOP 1 ADVALOREM FROM dbo.IMPORTACIONES"));
        assertEquals("0", valor(CALE, "SELECT TOP 1 IVA FROM dbo.IMPORTACIONES"));
        assertEquals("3.5", valor(CALE, "SELECT TOP 1 DTA FROM dbo.IMPORTACIONES"));
        assertEquals("0", valor(CALE, "SELECT TOP 1 PREVALIDACION FROM dbo.IMPORTACIONES"));
        assertEquals("MAT-1", valor(CALE, "SELECT TOP 1 CLAVE FROM dbo.PARTIDAS"));
        transaccionLimpia();
    }

    @Test
    void migracionesOnceYUnoDosSonIdempotentes() throws Exception {
        try (Connection app = conectar(APP)) {
            aplicarArchivo(app, raizRepo().resolve("migrations/11-pedimentos-confirmar-permission.sql"));
            aplicarArchivo(app, raizRepo().resolve("migrations/12-pedimento-confirmada-state.sql"));
        }
        assertEquals(1, valorInt(APP, "SELECT COUNT(*) FROM app24.Actividad WHERE clave = 'PEDIMENTOS_CONFIRMAR'"));
        assertEquals(0, valorInt(APP, "SELECT COUNT(*) FROM app24.PerfilActividad pa "
                + "JOIN app24.Actividad a ON a.id = pa.actividad_id WHERE a.clave = 'PEDIMENTOS_CONFIRMAR'"));
        // La migration 12 permite CONFIRMADA y conserva los estados previos válidos.
        long carga = crearCarga("LEGACY-STAGE-DERIVED-V2");
        assertEquals("PREVISUALIZADA", estadoCarga(carga));
        ejecutar(APP, "UPDATE app24.CargaPedimento SET estado = 'CONFIRMADA' WHERE id = " + carga);
        assertEquals("CONFIRMADA", estadoCarga(carga));
    }

    // ------------------------------------------------------------- helpers SQL

    private static boolean dockerDisponible() {
        try {
            DockerClientFactory.instance().client();
            return true;
        } catch (Throwable noDisponible) {
            return false;
        }
    }

    private static void crearBases() throws Exception {
        try (Connection master = DriverManager.getConnection(url("master"), SQL.getUsername(), SQL.getPassword());
             Statement s = master.createStatement()) {
            s.execute("IF DB_ID('" + CALE + "') IS NULL CREATE DATABASE [" + CALE + "]");
            s.execute("IF DB_ID('" + APP + "') IS NULL CREATE DATABASE [" + APP + "]");
        }
        try (Connection cale = conectar(CALE); Statement s = cale.createStatement()) {
            // Reproduce la restricción demostrada en LIVE: CALE_IMMEX sin OPENJSON.
            s.execute("ALTER DATABASE [" + CALE + "] SET COMPATIBILITY_LEVEL = 100");
        }
    }

    private static void aplicarFixtures() throws Exception {
        try (Connection cale = conectar(CALE)) {
            aplicarArchivo(cale, raizFixtures().resolve("01-create-cale-immex-fixture.sql"));
        }
        try (Connection app = conectar(APP)) {
            aplicarArchivo(app, raizRepo().resolve("02-app-schema.sql"));
            aplicarArchivo(app, raizRepo().resolve("migrations/09-pedimentos-staging-v1.sql"));
            aplicarArchivo(app, raizRepo().resolve("migrations/11-pedimentos-confirmar-permission.sql"));
            aplicarArchivo(app, raizRepo().resolve("migrations/12-pedimento-confirmada-state.sql"));
            aplicarArchivo(app, raizRepo().resolve("procedures/commands/APP24_C_BITACORA_REGISTRAR.sql"));
            sembrarUsuario(app);
        }
        try (Connection cale = conectar(CALE)) {
            // DDL versionado REAL del command (no una copia simplificada).
            aplicarArchivo(cale, raizRepo().resolve("procedures/commands/APP24_C_PEDIMENTO_CONFIRMAR.sql"));
        }
    }

    private static void sembrarUsuario(Connection app) throws Exception {
        try (Statement s = app.createStatement()) {
            s.execute("IF NOT EXISTS (SELECT 1 FROM app24.PerfilApp WHERE nombre = 'ADMINISTRADOR') "
                    + "INSERT INTO app24.PerfilApp (nombre, estado) VALUES ('ADMINISTRADOR', 'ACTIVO')");
            s.execute("IF NOT EXISTS (SELECT 1 FROM app24.UsuarioApp WHERE clave = 'test.sql') "
                    + "INSERT INTO app24.UsuarioApp (clave, nombre, correo, password_hash, estado, perfil_id) "
                    + "SELECT 'test.sql', 'Test SQL', 'test.sql@local', 'x', 'ACTIVO', id FROM app24.PerfilApp WHERE nombre = 'ADMINISTRADOR'");
        }
        try (Statement s = app.createStatement();
             ResultSet rs = s.executeQuery("SELECT TOP 1 id FROM app24.UsuarioApp WHERE clave = 'test.sql'")) {
            rs.next();
            usuarioId = rs.getLong(1);
        }
    }

    private static Path raizRepo() {
        Path desdeBackend = Path.of("..", "infra", "sql");
        return Files.exists(desdeBackend) ? desdeBackend : Path.of("infra", "sql");
    }

    private static Path raizFixtures() {
        Path desdeBackend = Path.of("src", "test", "resources", "sql", "pediments");
        return Files.exists(desdeBackend) ? desdeBackend : Path.of("backend", "src", "test", "resources", "sql", "pediments");
    }

    private static String url(String db) {
        return "jdbc:sqlserver://" + SQL.getHost() + ":" + SQL.getMappedPort(1433)
                + ";databaseName=" + db + ";encrypt=false;trustServerCertificate=true";
    }

    private static Connection conectar(String db) throws SQLException {
        return DriverManager.getConnection(url(db), SQL.getUsername(), SQL.getPassword());
    }

    /** Divide por líneas que son exactamente {@code GO} (case-insensitive). */
    static void aplicarArchivo(Connection c, Path archivo) throws Exception {
        String texto = Files.readString(archivo);
        StringBuilder batch = new StringBuilder();
        List<String> batches = new ArrayList<>();
        for (String linea : texto.split("\r?\n", -1)) {
            if (linea.trim().equalsIgnoreCase("GO")) {
                if (!batch.toString().isBlank()) batches.add(batch.toString());
                batch.setLength(0);
            } else {
                batch.append(linea).append('\n');
            }
        }
        if (!batch.toString().isBlank()) batches.add(batch.toString());
        try (Statement s = c.createStatement()) {
            for (String b : batches) {
                if (b.isBlank()) continue;
                s.execute(b);
                while (s.getMoreResults() || s.getUpdateCount() != -1) { /* drena */ }
            }
        }
    }

    private long crearCarga(String version) throws Exception {
        return insertarCargaConEstado("PREVISUALIZADA", version);
    }

    private long insertarCargaConEstado(String estado) throws Exception {
        return insertarCargaConEstado(estado, "LEGACY-STAGE-DERIVED-V2");
    }

    private long insertarCargaConEstado(String estado, String version) throws Exception {
        try (Connection app = conectar(APP);
             var ps = app.prepareStatement("INSERT INTO app24.CargaPedimento "
                     + "(archivo, hash, usuario_id, estado, total_filas, filas_validas, filas_invalidas, version_plantilla, correlation_id) "
                     + "VALUES ('it.xlsx', ?, ?, ?, 0, 0, 0, ?, 'it')", Statement.RETURN_GENERATED_KEYS)) {
            ps.setString(1, java.util.UUID.randomUUID().toString().replace("-", "") + java.util.UUID.randomUUID().toString().replace("-", "").substring(0, 32));
            ps.setLong(2, usuarioId);
            ps.setString(3, estado);
            ps.setString(4, version);
            ps.executeUpdate();
            try (ResultSet rs = ps.getGeneratedKeys()) {
                rs.next();
                return rs.getLong(1);
            }
        }
    }

    private void agregarFila(long cargaId, int fila, String hoja, Map<String, String> datos) throws Exception {
        try (Connection app = conectar(APP);
             var ps = app.prepareStatement("INSERT INTO app24.CargaPedimentoFila (carga_id, hoja, fila, datos_json) VALUES (?, ?, ?, ?)")) {
            ps.setLong(1, cargaId);
            ps.setString(2, hoja);
            ps.setInt(3, fila);
            ps.setNString(4, json(datos));
            ps.executeUpdate();
        }
    }

    private void agregarError(long cargaId) throws Exception {
        try (Connection app = conectar(APP);
             var ps = app.prepareStatement("INSERT INTO app24.ErrorCargaPedimento (carga_id, hoja, fila, columna, valor_enmascarado, codigo, mensaje) "
                     + "VALUES (?, 'H', 1, 'Clave', 'no almacenado', 'PED-003', 'El material no existe en el catalogo.')")) {
            ps.setLong(1, cargaId);
            ps.executeUpdate();
        }
    }

    private void crearMaterial(String clave) throws Exception {
        ejecutar(CALE, "INSERT INTO dbo.MATERIAL (MATERIALKEY, CLAVE, UNIDAD, DESCRIPCION) VALUES (1, '" + clave + "', 'KG', 'Sintetico')");
    }

    private void crearProducto(String clave) throws Exception {
        ejecutar(CALE, "INSERT INTO dbo.PRODUCTOS (PRODUCTOKEY, CVE_PRODUCTO, UNIDAD) VALUES (1, '" + clave + "', 'KG')");
    }

    private Map<String, String> confirmar(long cargaId) throws Exception {
        try (Connection c = conectar(CALE);
             CallableStatement cs = c.prepareCall("{call dbo.APP24_C_PEDIMENTO_CONFIRMAR(?, ?, ?)}")) {
            cs.setLong(1, cargaId);
            cs.setLong(2, usuarioId);
            cs.setString(3, "it-corr");
            boolean hay = cs.execute();
            if (!hay) throw new IllegalStateException("El command no devolvió result set");
            try (ResultSet rs = cs.getResultSet()) {
                rs.next();
                Map<String, String> r = new LinkedHashMap<>();
                r.put("CargaId", rs.getString("CargaId"));
                r.put("Estado", rs.getString("Estado"));
                r.put("TipoOperacion", rs.getString("TipoOperacion"));
                r.put("OperacionesProcesadas", rs.getString("OperacionesProcesadas"));
                r.put("PartidasProcesadas", rs.getString("PartidasProcesadas"));
                r.put("FechaConfirmacion", rs.getString("FechaConfirmacion"));
                r.put("Resultado", rs.getString("Resultado"));
                assertFalse(cs.getMoreResults(), "El command no debe devolver result sets adicionales");
                return r;
            }
        }
    }

    private String estadoCarga(long cargaId) throws Exception {
        return valor(APP, "SELECT estado FROM app24.CargaPedimento WHERE id = " + cargaId);
    }

    private void transaccionLimpia() throws Exception {
        try (Connection c = conectar(CALE); Statement s = c.createStatement();
             ResultSet rs = s.executeQuery("SELECT @@TRANCOUNT AS t, XACT_STATE() AS x")) {
            rs.next();
            assertEquals(0, rs.getInt("t"), "@@TRANCOUNT debe ser 0 tras la ejecución");
            assertEquals(0, rs.getInt("x"), "XACT_STATE() debe ser 0 tras la ejecución");
        }
    }

    private void ejecutar(String db, String sql) throws Exception {
        try (Connection c = conectar(db); Statement s = c.createStatement()) {
            s.execute(sql);
        }
    }

    private int contar(String db, String tabla) throws Exception {
        return valorInt(db, "SELECT COUNT(*) FROM " + tabla);
    }

    private int valorInt(String db, String sql) throws Exception {
        return Integer.parseInt(valor(db, sql));
    }

    private String valor(String db, String sql) throws Exception {
        try (Connection c = conectar(db); Statement s = c.createStatement(); ResultSet rs = s.executeQuery(sql)) {
            return rs.next() ? rs.getString(1) : null;
        }
    }

    // --------------------------------------------------------- fixture de filas

    private static Map<String, String> importRow(String documento, String clave, String sec) {
        Map<String, String> d = new LinkedHashMap<>();
        d.put("Aduana", "190");
        d.put("Patente", "3302");
        d.put("NumeroPedimento", documento);
        d.put("ClavePedimento", "A1");
        d.put("TipoOperacion", "1");
        d.put("TipoPedimento", "1");
        d.put("FechaPago", "2026-05-28");
        d.put("ClaveCP", "PROV1");
        d.put("Sec", sec);
        d.put("Clave", clave);
        d.put("Descripcion", "Material sintetico");
        d.put("Fraccion", "17019999");
        d.put("CantidadComercial", "12.5");
        d.put("UnidadComercial", "KG");
        d.put("CantidadTarifa", "10");
        d.put("UnidadTarifa", "KG");
        d.put("ValorDolares", "100");
        d.put("ValorAduanal", "90");
        d.put("ValorComercial", "110");
        d.put("ValorME", "1800");
        d.put("PaisOD", "US");
        d.put("PaisCV", "US");
        d.put("Factura", "F-1");
        d.put("FechaFactura", "2026-05-20");
        d.put("FECHAENTRADA", "2026-05-28");
        d.put("lote", "L1");
        d.put("CATEGORIA", "I");
        d.put("IGIE", "1.25");
        d.put("IVA", "0.5");
        d.put("DTA", "3.5");
        d.put("PREV", "0.75");
        d.put("TIPOTASAIGIE", "GENERAL");
        return d;
    }

    private static Map<String, String> exportRow(String documento, String clave, String sec, String descargaDirigida) {
        Map<String, String> d = new LinkedHashMap<>();
        d.put("Aduana", "190");
        d.put("Patente", "3302");
        d.put("NumeroPedimento", documento);
        d.put("ClavePedimento", "A1");
        d.put("TipoOperacion", "2");
        d.put("TipoPedimento", "1");
        d.put("FechaPago", "2026-05-28");
        d.put("ClaveCP", "CLI1");
        d.put("Sec", sec);
        d.put("Clave", clave);
        d.put("Descripcion", "Producto sintetico");
        d.put("Fraccion", "17019999");
        d.put("CantidadComercial", "7.5");
        d.put("UnidadComercial", "KG");
        d.put("CantidadTarifa", "7");
        d.put("UnidadTarifa", "KG");
        d.put("ValorDolares", "50");
        d.put("ValorAduanal", "45");
        d.put("ValorComercial", "55");
        d.put("ValorME", "900");
        d.put("PaisOD", "US");
        d.put("PaisCV", "US");
        d.put("Factura", "F-2");
        d.put("FechaFactura", "2026-05-20");
        d.put("lote", "L2");
        d.put("IGIE", "0.5");
        d.put("IVA", "0.25");
        d.put("DTA", "1.5");
        d.put("PREV", "0.25");
        if (descargaDirigida != null) d.put("DescargaDirigida", descargaDirigida);
        return d;
    }

    private static String json(Map<String, String> datos) {
        StringBuilder sb = new StringBuilder("{");
        boolean primero = true;
        for (Map.Entry<String, String> e : datos.entrySet()) {
            if (!primero) sb.append(',');
            primero = false;
            sb.append('"').append(esc(e.getKey())).append("\":");
            if (e.getValue() == null) sb.append("null");
            else sb.append('"').append(esc(e.getValue())).append('"');
        }
        return sb.append('}').toString();
    }

    private static String esc(String v) {
        return v.replace("\\", "\\\\").replace("\"", "\\\"");
    }
}
