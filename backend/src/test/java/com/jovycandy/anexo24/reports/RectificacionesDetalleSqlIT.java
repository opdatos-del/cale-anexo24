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
 * Verifica el SP versionado del detalle de rectificaciones aplicando el archivo productivo real.
 * dbo.v_rectificaciones se representa con una tabla efímera de 8 columnas (solo fixture Testcontainers).
 *
 * PHYSICAL_ROW_KEY = NONE
 * STABLE_ORDER_FOR_NON_IDENTICAL_ROWS = YES
 * EXACT_DUPLICATE_ROWS = COLLAPSED_BY_LEGACY_VIEW_DISTINCT (el wrapper no agrega DISTINCT)
 */
class RectificacionesDetalleSqlIT {
    private static final String DB = "CALE_IMMEX";
    private static final String STATUS_LEGACY = "CUIDADO AMBOS DESCARGAN";
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
            s.execute("CREATE TABLE dbo.v_rectificaciones ("
                    + "[Pedimento] VARCHAR(30) NULL, [Clave Pedimento] VARCHAR(5) NULL, [Descarga] VARCHAR(5) NULL,"
                    + "[Pedimento Original] VARCHAR(30) NULL, [Existe Pedimento] VARCHAR(5) NULL,"
                    + "[Clave Pedimento Original] VARCHAR(5) NULL, [Descarga Original] VARCHAR(5) NULL, [Status] VARCHAR(30) NULL)");
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
            s.execute("TRUNCATE TABLE dbo.v_rectificaciones");
            // Insertadas fuera de orden a propósito: el orden lo impone el wrapper.
            s.execute("INSERT dbo.v_rectificaciones VALUES "
                    + "('P-0004','C3','D3','P-0005','NO','C9','D3','" + STATUS_LEGACY + "'),"
                    + "('P-0006','D4',NULL,'P-0007',NULL,NULL,NULL,NULL),"
                    + "('P-0003','A1','D1','P-0001','SI','A1','D1','" + STATUS_LEGACY + "'),"
                    + "('P-0002','B2','D2','P-0001','SI','A1','D9','')");
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
        try (Connection c = conectar(DB); Statement s = c.createStatement()) {
            s.execute("INSERT dbo.v_rectificaciones VALUES ('P-1','A1','D1','P-0','SI','A1','D1','')");
        }
        Resultado r = listar(null, 1, 20);
        assertEquals(1, r.total());
        assertEquals(1, r.filas().size());
    }

    @Test
    void variasFilas() throws Exception {
        Resultado r = listar(null, 1, 20);
        assertEquals(4, r.total());
        assertEquals(4, r.filas().size());
    }

    @Test
    void contratoDeOchoColumnas() throws Exception {
        assertEquals(List.of("PEDIMENTO", "CLAVE_PEDIMENTO", "DESCARGA", "PEDIMENTO_ORIGINAL", "EXISTE_PEDIMENTO",
                "CLAVE_PEDIMENTO_ORIGINAL", "DESCARGA_ORIGINAL", "STATUS"), listar(null, 1, 20).columnas());
    }

    @Test
    void preservaNulos() throws Exception {
        Map<String, Object> fila = listar("P-0007", 1, 20).filas().getFirst();
        assertEquals("P-0006", fila.get("PEDIMENTO"));
        for (String columna : List.of("DESCARGA", "EXISTE_PEDIMENTO", "CLAVE_PEDIMENTO_ORIGINAL", "DESCARGA_ORIGINAL", "STATUS")) {
            assertNull(fila.get(columna), columna);
        }
    }

    @Test
    void preservaStatusNormalYLiteralLegacy() throws Exception {
        Resultado r = listar(null, 1, 20);
        assertEquals("", filaPorPedimento(r, "P-0002").get("STATUS"));
        assertEquals(STATUS_LEGACY, filaPorPedimento(r, "P-0003").get("STATUS"));
    }

    @Test
    void filtraPorPedimento() throws Exception {
        Resultado r = listar("P-0004", 1, 20);
        assertEquals(1, r.total());
        assertEquals("P-0004", r.filas().getFirst().get("PEDIMENTO"));
    }

    @Test
    void filtraPorClavePedimento() throws Exception {
        Resultado r = listar("B2", 1, 20);
        assertEquals(1, r.total());
        assertEquals("P-0002", r.filas().getFirst().get("PEDIMENTO"));
    }

    @Test
    void filtraPorPedimentoOriginal() throws Exception {
        Resultado r = listar("P-0001", 1, 20);
        assertEquals(2, r.total());
        assertTrue(r.filas().stream().allMatch(f -> "P-0001".equals(f.get("PEDIMENTO_ORIGINAL"))));
    }

    @Test
    void filtraPorClavePedimentoOriginal() throws Exception {
        Resultado r = listar("C9", 1, 20);
        assertEquals(1, r.total());
        assertEquals("P-0004", r.filas().getFirst().get("PEDIMENTO"));
    }

    @Test
    void filtraPorExistePedimento() throws Exception {
        Resultado r = listar("NO", 1, 20);
        assertEquals(1, r.total());
        assertEquals("NO", r.filas().getFirst().get("EXISTE_PEDIMENTO"));
    }

    @Test
    void filtraPorStatus() throws Exception {
        Resultado r = listar("CUIDADO", 1, 20);
        assertEquals(2, r.total());
        assertTrue(r.filas().stream().allMatch(f -> STATUS_LEGACY.equals(f.get("STATUS"))));
    }

    @Test
    void filtroEnBlancoEquivaleANull() throws Exception {
        assertEquals(listar(null, 1, 20).total(), listar("   ", 1, 20).total());
        assertEquals(4, listar("   ", 1, 20).filas().size());
    }

    @Test
    void paginaUno() throws Exception {
        Resultado r = listar(null, 1, 2);
        assertEquals(List.of("P-0002", "P-0003"), pedimentos(r));
    }

    @Test
    void paginaDos() throws Exception {
        Resultado r = listar(null, 2, 2);
        assertEquals(List.of("P-0004", "P-0006"), pedimentos(r));
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
        // Pedimento Original, Pedimento: P-0001/P-0002, P-0001/P-0003, P-0005/P-0004, P-0007/P-0006
        assertEquals(List.of("P-0002", "P-0003", "P-0004", "P-0006"), pedimentos(listar(null, 1, 20)));
        assertEquals(pedimentos(listar(null, 1, 20)), pedimentos(listar(null, 1, 20)));
    }

    private static Map<String, Object> filaPorPedimento(Resultado r, String pedimento) {
        return r.filas().stream().filter(f -> pedimento.equals(f.get("PEDIMENTO"))).findFirst().orElseThrow();
    }

    private static List<Object> pedimentos(Resultado r) {
        return r.filas().stream().map(f -> f.get("PEDIMENTO")).toList();
    }

    private static void vaciar() throws SQLException {
        try (Connection c = conectar(DB); Statement s = c.createStatement()) {
            s.execute("TRUNCATE TABLE dbo.v_rectificaciones");
        }
    }

    private static Resultado listar(String filtro, int pagina, int tamano) throws SQLException {
        try (Connection c = conectar(DB);
             CallableStatement cs = c.prepareCall("{call dbo.APP24_Q_RECTIFICACIONES_DETALLE_LISTAR(?,?,?,?)}")) {
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
        Path p = Path.of("..", "infra", "sql", "procedures", "queries", "APP24_Q_RECTIFICACIONES_DETALLE_LISTAR.sql");
        return Files.exists(p) ? p : Path.of("infra", "sql", "procedures", "queries", "APP24_Q_RECTIFICACIONES_DETALLE_LISTAR.sql");
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
