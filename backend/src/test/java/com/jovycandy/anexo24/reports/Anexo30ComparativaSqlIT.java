package com.jovycandy.anexo24.reports;

import org.junit.jupiter.api.*;
import org.testcontainers.DockerClientFactory;
import org.testcontainers.containers.MSSQLServerContainer;

import java.math.BigDecimal;
import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.*;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Verifica el SP de comparativa A31/A24 sin ejecutar snapshots.
 * STABLE_ORDER_FOR_NON_IDENTICAL_ROWS = YES
 * EXACT_DUPLICATE_RELATIVE_ORDER = NOT_GUARANTEED
 */
class Anexo30ComparativaSqlIT {
    private static final String DB = "CALE_IMMEX";
    static final MSSQLServerContainer<?> SQL = new MSSQLServerContainer<>("mcr.microsoft.com/mssql/server:2022-latest").acceptLicense();
    private static boolean disponible;

    @BeforeAll static void iniciar() throws Exception {
        disponible = DockerClientFactory.instance().isDockerAvailable();
        if (!disponible && "true".equalsIgnoreCase(System.getenv("CI"))) {
            throw new IllegalStateException("Docker no disponible en CI: los tests SQL de reportes son obligatorios (CI_SQL_GATE_REQUIRED).");
        }
        Assumptions.assumeTrue(disponible, "Docker no disponible localmente; se omite la prueba SQL.");
        SQL.start();
        try (Connection c = conectar("master"); Statement s = c.createStatement()) { s.execute("CREATE DATABASE " + DB); }
        try (Connection c = conectar(DB); Statement s = c.createStatement()) {
            s.execute("CREATE TABLE dbo.A31_COMPARATIVADESCARGA (" +
                "CLAVEPEDIMENTO VARCHAR(5) NULL," +
                "EJERCICIO VARCHAR(5) NULL," +
                "PERIODO VARCHAR(5) NULL," +
                "FRACCION VARCHAR(10) NULL," +
                "[VALOR A31] NUMERIC(18,4) NULL," +
                "[VALOR A24] NUMERIC(18,4) NULL," +
                "DIFERENCIA NUMERIC(18,4) NULL," +
                "IVA21TOTAL NUMERIC(18,4) NULL," +
                "IVA22TOTAL NUMERIC(18,4) NULL," +
                "VALORTOTAL NUMERIC(18,4) NULL," +
                "[IVA DESCARGADO A31] NUMERIC(18,4) NULL," +
                "[IVA DESCARGADO A24] NUMERIC(18,4) NULL)");
            aplicarArchivo(c, rutaSql());
        }
    }

    @AfterAll static void detener() { if (disponible) SQL.stop(); }

    @BeforeEach void sembrar() throws Exception {
        try (Connection c = conectar(DB); Statement s = c.createStatement()) {
            s.execute("TRUNCATE TABLE dbo.A31_COMPARATIVADESCARGA");
            s.execute("INSERT dbo.A31_COMPARATIVADESCARGA " +
                "(CLAVEPEDIMENTO,EJERCICIO,PERIODO,FRACCION,[VALOR A31],[VALOR A24],DIFERENCIA,IVA21TOTAL,IVA22TOTAL,VALORTOTAL,[IVA DESCARGADO A31],[IVA DESCARGADO A24]) VALUES " +
                "('AA','2026','01','90000001',100.0000,90.0000,10.0000,5.0000,3.0000,98.0000,4.5000,2.5000)," +
                "('BB','2025','12','80000002',200.0000,180.0000,20.0000,10.0000,6.0000,196.0000,9.0000,5.0000)," +
                "('AA','2026','01','90000001',150.0000,140.0000,10.0000,7.0000,4.0000,151.0000,6.5000,3.5000)," +
                "('AA','2026','01','90000001',100.0000,90.0000,10.0000,5.0000,3.0000,98.0000,4.5000,2.5000)," +
                "(NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL)," +
                "('CC','2024','06','70000003',300.0000,280.0000,20.0000,15.0000,8.0000,303.0000,14.0000,7.0000)");
        }
    }
    /** Caso 1: vacio. */
    @Test void vaciaSinRegistros() throws Exception {
        try (Connection c = conectar(DB); Statement s = c.createStatement()) { s.execute("TRUNCATE TABLE dbo.A31_COMPARATIVADESCARGA"); }
        Resultado r = listar(null, 1, 20);
        assertEquals(0, r.total()); assertTrue(r.filas().isEmpty());
    }

    /** Caso 2: una fila. */
    @Test void unaFila() throws Exception {
        try (Connection c = conectar(DB); Statement s = c.createStatement()) {
            s.execute("TRUNCATE TABLE dbo.A31_COMPARATIVADESCARGA");
            s.execute("INSERT dbo.A31_COMPARATIVADESCARGA (CLAVEPEDIMENTO,EJERCICIO,PERIODO,FRACCION,[VALOR A31],[VALOR A24],DIFERENCIA,IVA21TOTAL,IVA22TOTAL,VALORTOTAL,[IVA DESCARGADO A31],[IVA DESCARGADO A24]) VALUES ('X1','2026','01','12345678',10.0000,9.0000,1.0000,0.5000,0.3000,9.8000,0.4500,0.2500)");
        }
        Resultado r = listar(null, 1, 20);
        assertEquals(1, r.total()); assertEquals(1, r.filas().size());
    }
    /** Caso 3: multiples filas. */
    @Test void multipleFilas() throws Exception {
        Resultado r = listar(null, 1, 20);
        assertEquals(6, r.total()); assertEquals(6, r.filas().size());
    }

    /** Caso 4: contrato exacto 12 columnas. */
    @Test void contratoExacto12Columnas() throws Exception {
        Resultado r = listar(null, 1, 20);
        assertEquals(List.of("CLAVE_PEDIMENTO","EJERCICIO","PERIODO","FRACCION","VALOR_A31","VALOR_A24","DIFERENCIA","IVA21_TOTAL","IVA22_TOTAL","VALOR_TOTAL","IVA_DESCARGADO_A31","IVA_DESCARGADO_A24"), r.columnas());
    }

    /** Caso 5: tupla natural duplicada preservada. */
    @Test void preservaTuplaNaturalDuplicada() throws Exception {
        Resultado r = listar("90000001", 1, 20);
        assertEquals(3, r.total());
        long v150 = r.filas().stream().filter(f -> new BigDecimal("150.0000").compareTo((BigDecimal)f.get("VALOR_A31")) == 0).count();
        assertEquals(1, v150, "Fila valorA31=150 debe preservarse junto a duplicados de clave natural");
    }

    /** Caso 6: filas fisicamente identicas preservadas. */
    @Test void preservaFilaFisicaDuplicadaExacta() throws Exception {
        Resultado r = listar("90000001", 1, 20);
        long v100 = r.filas().stream().filter(f -> new BigDecimal("100.0000").compareTo((BigDecimal)f.get("VALOR_A31")) == 0).count();
        assertEquals(2, v100, "Las dos filas fisicamente identicas deben preservarse");
    }
    /** Caso 7: NULLs preservados en todas las columnas. */
    @Test void preservaNulos() throws Exception {
        Resultado r = listar(null, 1, 20);
        Map<String, Object> filaNula = r.filas().stream()
            .filter(f -> f.get("CLAVE_PEDIMENTO") == null && f.get("EJERCICIO") == null)
            .findFirst().orElseThrow(() -> new AssertionError("Fila nula no encontrada"));
        for (String col : r.columnas()) assertNull(filaNula.get(col), "Columna " + col + " debe ser NULL");
    }

    /** Caso 8: precision NUMERIC(18,4) preservada. */
    @Test void preservaPrecisionNumeric18_4() throws Exception {
        Resultado r = listar("80000002", 1, 20);
        assertEquals(1, r.total());
        assertEquals(new BigDecimal("200.0000"), r.filas().getFirst().get("VALOR_A31"));
        assertEquals(new BigDecimal("9.0000"), r.filas().getFirst().get("IVA_DESCARGADO_A31"));
        assertEquals(new BigDecimal("5.0000"), r.filas().getFirst().get("IVA_DESCARGADO_A24"));
    }

    /** Caso 9: filtro por CLAVEPEDIMENTO. */
    @Test void filtraPorClavePedimento() throws Exception {
        Resultado r = listar("BB", 1, 20);
        assertEquals(1, r.total()); assertEquals("BB", r.filas().getFirst().get("CLAVE_PEDIMENTO"));
    }

    /** Caso 10: filtro por EJERCICIO. */
    @Test void filtraPorEjercicio() throws Exception {
        Resultado r = listar("2024", 1, 20);
        assertEquals(1, r.total()); assertEquals("2024", r.filas().getFirst().get("EJERCICIO"));
    }
    /** Caso 11: filtro por PERIODO. */
    @Test void filtraPorPeriodo() throws Exception {
        Resultado r = listar("12", 1, 20);
        assertEquals(1, r.total()); assertEquals("12", r.filas().getFirst().get("PERIODO"));
    }

    /** Caso 12: filtro por FRACCION. */
    @Test void filtraPorFraccion() throws Exception {
        Resultado r = listar("70000003", 1, 20);
        assertEquals(1, r.total()); assertEquals("70000003", r.filas().getFirst().get("FRACCION"));
    }

    /** Caso 13: filtro blank equivale a NULL. */
    @Test void filtroBlankEquivaleANull() throws Exception {
        assertEquals(listar(null, 1, 20).total(), listar("   ", 1, 20).total());
    }

    /** Caso 14: pagina 1. */
    @Test void paginaUno() throws Exception {
        Resultado r = listar(null, 1, 2);
        assertEquals(6, r.total()); assertEquals(2, r.filas().size());
    }

    /** Caso 15: pagina 2. */
    @Test void paginaDos() throws Exception {
        Resultado p1 = listar(null, 1, 2);
        Resultado p2 = listar(null, 2, 2);
        assertEquals(6, p2.total()); assertEquals(2, p2.filas().size());
        assertFalse(p1.filas().equals(p2.filas()));
    }
    /** Caso 16: pagina alta (vacia). */
    @Test void paginaAlta() throws Exception {
        Resultado r = listar(null, 999, 20);
        assertEquals(6, r.total()); assertTrue(r.filas().isEmpty());
    }

    /** Caso 17: pagina < 1 lanza error. */
    @Test void paginaMenorQueUnoLanzaError() {
        assertThrows(SQLException.class, () -> listar(null, 0, 20));
    }

    /** Caso 18: tamano < 1 lanza error. */
    @Test void tamanoMenorQueUnoLanzaError() {
        assertThrows(SQLException.class, () -> listar(null, 1, 0));
    }

    /** Caso 19: tamano > 100 lanza error. */
    @Test void tamanoMayorQueCienLanzaError() {
        assertThrows(SQLException.class, () -> listar(null, 1, 101));
    }

    /** Caso 20: @Total calculado antes del paginado. */
    @Test void totalAntesDelPaginado() throws Exception {
        Resultado r = listar(null, 1, 2);
        assertEquals(6, r.total(), "Total debe reflejar el conteo real, no solo la pagina");
        assertEquals(2, r.filas().size());
    }

    /** Caso 21: orden determinista para filas no identicas. */
    @Test void ordenDeterministaFilasNoIdenticas() throws Exception {
        Resultado r = listar(null, 1, 20);
        assertNull(r.filas().getLast().get("EJERCICIO"), "NULL ejercicio debe ir al final");
        assertEquals("2026", r.filas().getFirst().get("EJERCICIO"), "2026 debe ser el primer ejercicio");
        int idxBB = -1, idxCC = -1;
        for (int i = 0; i < r.filas().size(); i++) {
            Object clv = r.filas().get(i).get("CLAVE_PEDIMENTO");
            if ("BB".equals(clv)) idxBB = i;
            if ("CC".equals(clv)) idxCC = i;
        }
        assertTrue(idxBB < idxCC, "BB(2025) debe preceder a CC(2024)");
    }
    private static Resultado listar(String filtro, int pagina, int tamano) throws SQLException {
        try (Connection c = conectar(DB); CallableStatement cs = c.prepareCall("{call dbo.APP24_Q_ANEXO30_REVISION_COMPARATIVA_LISTAR(?,?,?,?)}")) {
            if (filtro == null) cs.setNull(1, Types.VARCHAR); else cs.setString(1, filtro);
            cs.setInt(2, pagina); cs.setInt(3, tamano); cs.registerOutParameter(4, Types.BIGINT); cs.execute();
            List<String> columnas = new ArrayList<>(); List<Map<String, Object>> filas = new ArrayList<>();
            try (ResultSet rs = cs.getResultSet()) {
                ResultSetMetaData meta = rs.getMetaData();
                for (int i = 1; i <= meta.getColumnCount(); i++) columnas.add(meta.getColumnLabel(i));
                while (rs.next()) { Map<String, Object> fila = new LinkedHashMap<>(); for (String col : columnas) fila.put(col, rs.getObject(col)); filas.add(fila); }
            }
            return new Resultado(columnas, filas, cs.getLong(4));
        }
    }
    private record Resultado(List<String> columnas, List<Map<String, Object>> filas, long total) {}
    private static Connection conectar(String db) throws SQLException { return DriverManager.getConnection("jdbc:sqlserver://" + SQL.getHost() + ":" + SQL.getMappedPort(1433) + ";databaseName=" + db + ";encrypt=false;trustServerCertificate=true", SQL.getUsername(), SQL.getPassword()); }
    private static Path rutaSql() { Path p = Path.of("..","infra","sql","procedures","queries","APP24_Q_ANEXO30_REVISION_COMPARATIVA_LISTAR.sql"); return Files.exists(p) ? p : Path.of("infra","sql","procedures","queries","APP24_Q_ANEXO30_REVISION_COMPARATIVA_LISTAR.sql"); }
    private static void aplicarArchivo(Connection c, Path archivo) throws Exception { StringBuilder batch = new StringBuilder(); try (Statement s = c.createStatement()) { for (String linea : Files.readString(archivo).split("\r?\n",-1)) { if (linea.trim().equalsIgnoreCase("GO")) { ejecutarBatch(s,batch,archivo); batch.setLength(0); } else batch.append(linea).append(System.lineSeparator()); } ejecutarBatch(s,batch,archivo); } }
    private static void ejecutarBatch(Statement s, StringBuilder batch, Path archivo) throws SQLException { if (!batch.toString().isBlank()) try { s.execute(batch.toString()); } catch (SQLException error) { throw new IllegalStateException("Fallo aplicando " + archivo.getFileName() + ": " + error.getMessage(), error); } }
}
