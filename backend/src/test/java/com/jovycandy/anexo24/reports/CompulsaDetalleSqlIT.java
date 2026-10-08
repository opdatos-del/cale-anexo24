package com.jovycandy.anexo24.reports;

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
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Types;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * FIXTURE_PARITY_SCOPE = VIEW_PROJECTION_CONTRACT_ONLY
 * REAL_DBSQL_VIEW_DEFINITION = CONFIRMED_BY_CONTROLLER
 * REAL_LEGACY_VIEW_RUNTIME_PARITY = NOT_EXECUTED
 */
class CompulsaDetalleSqlIT {
    private static final String DB = "CALE_IMMEX";
    private static final String FALTA_GLOSA = "FALTA CARGAR GLOSA";
    private static final String FALTA_A24 = "FALTA CAPTURAR A24";
    static final MSSQLServerContainer<?> SQL = new MSSQLServerContainer<>("mcr.microsoft.com/mssql/server:2022-latest").acceptLicense();
    private static boolean disponible;

    @BeforeAll
    static void iniciar() throws Exception {
        disponible = DockerClientFactory.instance().isDockerAvailable();
        if (!disponible && "true".equalsIgnoreCase(System.getenv("CI"))) {
            throw new IllegalStateException("Docker no disponible en CI: los tests SQL de reportes son obligatorios (CI_SQL_GATE_REQUIRED).");
        }
        Assumptions.assumeTrue(disponible, "Docker no disponible localmente; se omite la prueba SQL.");
        SQL.start();
        try (Connection c = conectar("master"); Statement s = c.createStatement()) {
            s.execute("CREATE DATABASE " + DB);
        }
        try (Connection c = conectar(DB); Statement s = c.createStatement()) {
            s.execute("CREATE TABLE dbo.v_compulsa ("
                    + "[PedimentoGlosa] VARCHAR(100) NULL, [SEC GLOSA] VARCHAR(100) NULL, [PedimentoA24] VARCHAR(100) NULL, [SEC A24] VARCHAR(100) NULL,"
                    + "[Clave Pedimento Glosa] VARCHAR(100) NULL, [Clave Pedimento A24] VARCHAR(100) NULL, [STATUS CLAVE PEDIMENTO] VARCHAR(100) NULL,"
                    + "[FechaGlosa] VARCHAR(100) NULL, [FechaA24] VARCHAR(100) NULL, [STATUS FECHAS] VARCHAR(100) NULL,"
                    + "[Fraccion Glosa] VARCHAR(100) NULL, [Fraccion A24] VARCHAR(100) NULL, [STATUS FRACCION] VARCHAR(100) NULL,"
                    + "[Pais OD Glosa] VARCHAR(100) NULL, [Pais OD A24] VARCHAR(100) NULL, [STATUS PAIS OD] VARCHAR(100) NULL,"
                    + "[Pais CV Glosa] VARCHAR(100) NULL, [Pais CV A24] VARCHAR(100) NULL, [STATUS PAIS CV] VARCHAR(100) NULL,"
                    + "[Valor Aduana Glosa] VARCHAR(100) NULL, [Valor Aduana A24] VARCHAR(100) NULL, [STATUS VALOR ADUANAL] VARCHAR(100) NULL,"
                    + "[Valor Comercial Glosa] VARCHAR(100) NULL, [Valor Comercial A24] VARCHAR(100) NULL, [STATUS VALOR COMERCIAL] VARCHAR(100) NULL,"
                    + "[Cantidad UMC Glosa] VARCHAR(100) NULL, [Cantidad UMC A24] VARCHAR(100) NULL, [STATUS CANTIDAD COMERCIAL] VARCHAR(100) NULL,"
                    + "[Cantidad UMT Glosa] VARCHAR(100) NULL, [Cantidad UMT A24] VARCHAR(100) NULL, [STATUS CANTIDAD TARIFA] VARCHAR(100) NULL,"
                    + "[toper] VARCHAR(100) NULL, [tipoped] VARCHAR(100) NULL)");
            aplicarArchivo(c, rutaSql());
        }
    }

    @AfterAll
    static void detener() {
        if (disponible) SQL.stop();
    }

    @BeforeEach
    void sembrar() throws Exception {
        try (Connection c = conectar(DB); Statement s = c.createStatement()) {
            s.execute("TRUNCATE TABLE dbo.v_compulsa");
            insertar(c, "G-001", "1", "A-001", "1", "CL-G1", "CL-A1", "OK", "2026-01-01", "2026-01-02", "OK",
                    "84715001", "84715001", "OK", "MX", "MX", "OK", "US", "US", "OK", "100", "100", "OK",
                    "120", "120", "OK", "10", "10", "OK", "10", "10", "OK", "IMP", "IM");
            insertar(c, "G-002", "2", "A-002", "2", "CL-G2", "CL-A2", "DIFERENCIA EN CLAVE PEDIMENTO", "2026-02-01", "2026-02-02", "DIFERENCIA EN FECHAS",
                    "84715002", "84715003", "DIFERENCIA EN FRACCION", "PAIS-X", "PAIS-Y", "DIFERENCIA EN PAIS OD", "CV-X", "CV-Y", "DIFERENCIA EN PAIS CV", "200", "201", "DIFERENCIA VALOR ADUANA",
                    "220", "221", "DIFERENCIA VALOR COMERCIAL", "20", "21", "DIFERENCIA CANTIDAD COMERCIAL", "30", "31", "DIFERENCIA CANTIDAD TARIFA", "EXP", "EX");
            insertar(c, FALTA_GLOSA, null, "A-003", "3", null, "CL-A3", "OK", "2026-03-01", "2026-03-01", "OK",
                    null, "84715004", "OK", null, "MX", "OK", null, "US", "OK", null, "300", "OK",
                    null, "320", "OK", null, "30", "OK", null, "40", "OK", null, "IM");
            insertar(c, "G-004", "4", FALTA_A24, null, "CL-G4", null, "OK", "2026-04-01", null, "OK",
                    "84715005", null, "OK", "MX", null, "OK", "US", null, "OK", "400", null, "OK",
                    "420", null, "OK", "40", null, "OK", "50", null, "OK", "IMP", null);
        }
    }

    private static void insertar(Connection c, String... valores) throws SQLException {
        assertEquals(33, valores.length);
        String marcadores = String.join(",", java.util.Collections.nCopies(33, "?"));
        try (PreparedStatement ps = c.prepareStatement("INSERT dbo.v_compulsa VALUES (" + marcadores + ")")) {
            for (int i = 0; i < valores.length; i++) ps.setString(i + 1, valores[i]);
            ps.executeUpdate();
        }
    }

    @Test
    void vacio() throws Exception {
        vaciar();
        Resultado r = listar(null, 1, 20);
        assertEquals(0, r.total());
        assertTrue(r.filas().isEmpty());
    }

    @Test
    void unaFila() throws Exception {
        vaciar();
        try (Connection c = conectar(DB)) {
            insertar(c, "G-ONE", "1", "A-ONE", "1", "CG", "CA", "OK", "2026-01-01", "2026-01-01", "OK",
                    "8471", "8471", "OK", "MX", "MX", "OK", "US", "US", "OK", "1", "1", "OK",
                    "1", "1", "OK", "1", "1", "OK", "1", "1", "OK", "IMP", "IM");
        }
        assertEquals(1, listar(null, 1, 20).total());
    }

    @Test
    void variasFilas() throws Exception {
        Resultado r = listar(null, 1, 20);
        assertEquals(4, r.total());
        assertEquals(4, r.filas().size());
    }

    @Test
    void contratoDeTreintaYTresColumnas() throws Exception {
        assertEquals(List.of("PEDIMENTO_GLOSA", "SEC_GLOSA", "PEDIMENTO_A24", "SEC_A24", "CLAVE_PEDIMENTO_GLOSA", "CLAVE_PEDIMENTO_A24", "STATUS_CLAVE_PEDIMENTO", "FECHA_GLOSA", "FECHA_A24", "STATUS_FECHAS", "FRACCION_GLOSA", "FRACCION_A24", "STATUS_FRACCION", "PAIS_OD_GLOSA", "PAIS_OD_A24", "STATUS_PAIS_OD", "PAIS_CV_GLOSA", "PAIS_CV_A24", "STATUS_PAIS_CV", "VALOR_ADUANA_GLOSA", "VALOR_ADUANA_A24", "STATUS_VALOR_ADUANA", "VALOR_COMERCIAL_GLOSA", "VALOR_COMERCIAL_A24", "STATUS_VALOR_COMERCIAL", "CANTIDAD_UMC_GLOSA", "CANTIDAD_UMC_A24", "STATUS_CANTIDAD_COMERCIAL", "CANTIDAD_UMT_GLOSA", "CANTIDAD_UMT_A24", "STATUS_CANTIDAD_TARIFA", "TIPO_OPERACION_GLOSA", "TIPO_PEDIMENTO_GLOSA"), listar(null, 1, 20).columnas());
    }

    @Test
    void filaCompletaConEstadosOk() throws Exception {
        Map<String, Object> fila = filaPorPedimento(listar(null, 1, 20), "G-001");
        for (String columna : List.of("STATUS_CLAVE_PEDIMENTO", "STATUS_FECHAS", "STATUS_FRACCION", "STATUS_PAIS_OD", "STATUS_PAIS_CV", "STATUS_VALOR_ADUANA", "STATUS_VALOR_COMERCIAL", "STATUS_CANTIDAD_COMERCIAL", "STATUS_CANTIDAD_TARIFA")) {
            assertEquals("OK", fila.get(columna), columna);
        }
    }

    @Test
    void preservaDiferenciasLegacyLiterales() throws Exception {
        Map<String, Object> fila = filaPorPedimento(listar(null, 1, 20), "G-002");
        assertEquals("DIFERENCIA EN CLAVE PEDIMENTO", fila.get("STATUS_CLAVE_PEDIMENTO"));
        assertEquals("DIFERENCIA EN FECHAS", fila.get("STATUS_FECHAS"));
        assertEquals("DIFERENCIA EN FRACCION", fila.get("STATUS_FRACCION"));
        assertEquals("DIFERENCIA EN PAIS OD", fila.get("STATUS_PAIS_OD"));
        assertEquals("DIFERENCIA EN PAIS CV", fila.get("STATUS_PAIS_CV"));
        assertEquals("DIFERENCIA VALOR ADUANA", fila.get("STATUS_VALOR_ADUANA"));
        assertEquals("DIFERENCIA VALOR COMERCIAL", fila.get("STATUS_VALOR_COMERCIAL"));
        assertEquals("DIFERENCIA CANTIDAD COMERCIAL", fila.get("STATUS_CANTIDAD_COMERCIAL"));
        assertEquals("DIFERENCIA CANTIDAD TARIFA", fila.get("STATUS_CANTIDAD_TARIFA"));
    }

    @Test
    void preservaEstadosDeAusenciaDelFullOuterJoin() throws Exception {
        Resultado r = listar(null, 1, 20);
        assertEquals(FALTA_GLOSA, filaPorPedimento(r, FALTA_GLOSA).get("PEDIMENTO_GLOSA"));
        assertEquals(FALTA_A24, filaPorPedimento(r, "G-004").get("PEDIMENTO_A24"));
    }

    @Test
    void preservaNulosProyectables() throws Exception {
        Map<String, Object> fila = filaPorPedimento(listar(null, 1, 20), FALTA_GLOSA);
        assertNull(fila.get("SEC_GLOSA"));
        assertNull(fila.get("CLAVE_PEDIMENTO_GLOSA"));
        assertNull(fila.get("FRACCION_GLOSA"));
    }

    @Test
    void filtraPorPedimentoGlosa() throws Exception {
        assertEquals(1, listar("G-001", 1, 20).total());
    }

    @Test
    void filtraPorPedimentoA24() throws Exception {
        assertEquals(1, listar("A-003", 1, 20).total());
    }

    @Test
    void filtraPorClave() throws Exception {
        assertEquals(1, listar("CL-G1", 1, 20).total());
    }

    @Test
    void filtraPorFraccion() throws Exception {
        assertEquals(1, listar("84715004", 1, 20).total());
    }

    @Test
    void filtraPorPais() throws Exception {
        assertEquals(1, listar("PAIS-X", 1, 20).total());
    }

    @Test
    void filtraPorStatus() throws Exception {
        assertEquals(1, listar("DIFERENCIA EN FECHAS", 1, 20).total());
    }

    @Test
    void filtroEnBlancoEquivaleANull() throws Exception {
        assertEquals(listar(null, 1, 20).total(), listar("   ", 1, 20).total());
    }

    @Test
    void paginaUno() throws Exception {
        assertEquals(List.of("G-001", "G-002"), pedimentos(listar(null, 1, 2)));
    }

    @Test
    void paginaDos() throws Exception {
        assertEquals(List.of(FALTA_GLOSA, "G-004"), pedimentos(listar(null, 2, 2)));
    }

    @Test
    void paginaAltaVaciaConTotal() throws Exception {
        Resultado r = listar(null, 99, 2);
        assertTrue(r.filas().isEmpty());
        assertEquals(4, r.total());
    }

    @Test
    void paginaInvalida() {
        assertThrows(SQLException.class, () -> listar(null, 0, 20));
    }

    @Test
    void tamanoInvalido() {
        assertThrows(SQLException.class, () -> listar(null, 1, 0));
        assertThrows(SQLException.class, () -> listar(null, 1, 101));
    }

    @Test
    void totalSeCalculaAntesDePaginar() throws Exception {
        Resultado r = listar(null, 1, 1);
        assertEquals(1, r.filas().size());
        assertEquals(4, r.total());
    }

    @Test
    void ordenEstableParaFilasNoIdenticas() throws Exception {
        List<Object> esperado = List.of("G-001", "G-002", FALTA_GLOSA, "G-004");
        assertEquals(esperado, pedimentos(listar(null, 1, 20)));
        assertEquals(pedimentos(listar(null, 1, 20)), pedimentos(listar(null, 1, 20)));
    }

    @Test
    void duplicadosExactosSePreservanPorElWrapper() throws Exception {
        try (Connection c = conectar(DB)) {
            insertar(c, "G-001", "1", "A-001", "1", "CL-G1", "CL-A1", "OK", "2026-01-01", "2026-01-02", "OK",
                    "84715001", "84715001", "OK", "MX", "MX", "OK", "US", "US", "OK", "100", "100", "OK",
                    "120", "120", "OK", "10", "10", "OK", "10", "10", "OK", "IMP", "IM");
        }
        Resultado r = listar("G-001", 1, 20);
        assertEquals(2, r.total());
        assertEquals(2, r.filas().size());
    }

    private static Map<String, Object> filaPorPedimento(Resultado r, String pedimento) {
        return r.filas().stream().filter(f -> pedimento.equals(f.get("PEDIMENTO_GLOSA"))).findFirst().orElseThrow();
    }

    private static List<Object> pedimentos(Resultado r) {
        return r.filas().stream().map(f -> f.get("PEDIMENTO_GLOSA")).toList();
    }

    private static void vaciar() throws SQLException {
        try (Connection c = conectar(DB); Statement s = c.createStatement()) {
            s.execute("TRUNCATE TABLE dbo.v_compulsa");
        }
    }

    private static Resultado listar(String filtro, int pagina, int tamano) throws SQLException {
        try (Connection c = conectar(DB);
             CallableStatement cs = c.prepareCall("{call dbo.APP24_Q_COMPULSA_DETALLE_LISTAR(?,?,?,?)}")) {
            if (filtro == null) cs.setNull(1, Types.VARCHAR); else cs.setString(1, filtro);
            cs.setInt(2, pagina);
            cs.setInt(3, tamano);
            cs.registerOutParameter(4, Types.BIGINT);
            cs.execute();
            List<String> columnas = new ArrayList<>();
            List<Map<String, Object>> filas = new ArrayList<>();
            try (ResultSet rs = cs.getResultSet()) {
                ResultSetMetaData meta = rs.getMetaData();
                for (int i = 1; i <= meta.getColumnCount(); i++) columnas.add(meta.getColumnLabel(i));
                while (rs.next()) {
                    Map<String, Object> fila = new LinkedHashMap<>();
                    for (String columna : columnas) fila.put(columna, rs.getObject(columna));
                    filas.add(fila);
                }
            }
            return new Resultado(columnas, filas, cs.getLong(4));
        }
    }

    private record Resultado(List<String> columnas, List<Map<String, Object>> filas, long total) {}

    private static Connection conectar(String db) throws SQLException {
        return DriverManager.getConnection("jdbc:sqlserver://" + SQL.getHost() + ":" + SQL.getMappedPort(1433)
                + ";databaseName=" + db + ";encrypt=false;trustServerCertificate=true", SQL.getUsername(), SQL.getPassword());
    }

    private static Path rutaSql() {
        Path p = Path.of("..", "infra", "sql", "procedures", "queries", "APP24_Q_COMPULSA_DETALLE_LISTAR.sql");
        return Files.exists(p) ? p : Path.of("infra", "sql", "procedures", "queries", "APP24_Q_COMPULSA_DETALLE_LISTAR.sql");
    }

    private static void aplicarArchivo(Connection c, Path archivo) throws Exception {
        StringBuilder batch = new StringBuilder();
        try (Statement s = c.createStatement()) {
            for (String linea : Files.readString(archivo).split("\r?\n", -1)) {
                if (linea.trim().equalsIgnoreCase("GO")) {
                    ejecutarBatch(s, batch, archivo);
                    batch.setLength(0);
                } else {
                    batch.append(linea).append(System.lineSeparator());
                }
            }
            ejecutarBatch(s, batch, archivo);
        }
    }

    private static void ejecutarBatch(Statement s, StringBuilder batch, Path archivo) throws SQLException {
        if (batch.toString().isBlank()) return;
        try {
            s.execute(batch.toString());
        } catch (SQLException error) {
            throw new IllegalStateException("Fallo aplicando " + archivo.getFileName() + ": " + error.getMessage(), error);
        }
    }
}
