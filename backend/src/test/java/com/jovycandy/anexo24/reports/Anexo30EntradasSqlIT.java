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
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Prueba SQL del reporte read-only Revision Anexo 30 - Entradas (LEGACY-073 parcial)
 * contra un SQL Server efimero.
 *
 * <p>Aplica el SP versionado real ({@code APP24_Q_ANEXO30_REVISION_ENTRADAS_LISTAR})
 * sobre un fixture sintetico de {@code A31_ENTRADAS} con el esquema declarado en el
 * dump del proyecto. Nunca se conecta a LIVE ni ejecuta procesos mutables A31.</p>
 */
class Anexo30EntradasSqlIT {

    private static final String CALE = "CALE_IMMEX";

    static final MSSQLServerContainer<?> SQL =
            new MSSQLServerContainer<>("mcr.microsoft.com/mssql/server:2022-latest").acceptLicense();

    private static boolean disponible;

    @BeforeAll
    static void iniciarContenedor() throws Exception {
        disponible = dockerDisponible();
        if (!disponible && "true".equalsIgnoreCase(System.getenv("CI"))) {
            throw new IllegalStateException("Docker no disponible en CI: los tests SQL de reportes son obligatorios (CI_SQL_GATE_REQUIRED).");
        }
        Assumptions.assumeTrue(disponible, "Docker no disponible localmente; se omite la prueba SQL.");
        SQL.start();
        crearBase();
        aplicarFixtures();
        aplicarSpReal();
    }

    @AfterAll
    static void detenerContenedor() {
        if (disponible) SQL.stop();
    }

    @BeforeEach
    void sembrarFilas() throws Exception {
        try (Connection c = conectar(CALE); Statement s = c.createStatement()) {
            s.execute("DELETE FROM dbo.A31_ENTRADAS");
            s.execute("INSERT INTO dbo.A31_ENTRADAS (Entradaskey, Descarga, Tipooperacion, Pedimentoarmado, PEDIMENTOORIGINAL, Fecha, FECHAORIGINAL, Clavepedimento, Fracccion, Valocomercial, IVAFP21, IVAFP22, SALDO, OPERACION, PARTIDA, ESAF) VALUES " 
                    + "(1, 'F4', 'IM', 'PED-2026-0001', 'PED-2026-0000', '2026-01-15T10:00:00', '2025-12-01T10:00:00', 'F4', '84715002', 1500.00, 240.00, 0.00, 1500.00, 1001, '1', 'ESAF-1')," 
                    + "(2, 'A3', 'IM', 'PED-2026-0002', 'PED-2025-0099', '2026-02-20T10:00:00', '2025-11-15T10:00:00', 'A3', '84715002', 800.00, 128.00, 0.00, 800.00, 1002, '1', 'ESAF-2')," 
                    + "(3, 'F4', 'IM', 'PED-2026-0003', null, '2026-03-10T10:00:00', null, 'F4', '90211001', 250.50, 40.08, 0.00, 250.50, 1003, '2', null)");
        }
    }

    @Test
    void contratoDeColumnasYConteo() throws Exception {
        Resultado resultado = listar(null, 1, 10);

        assertEquals(List.of("ENTRADA_KEY", "DESCARGA", "TIPO_OPERACION", "PEDIMENTO", "PEDIMENTO_ORIGINAL", "FECHA", "FECHA_ORIGINAL", "CLAVE_PEDIMENTO", "FRACCION", "VALOR_COMERCIAL", "IVA_FP21", "IVA_FP22", "SALDO", "OPERACION", "PARTIDA", "ESAF"),
                resultado.columnas());
        assertEquals(3, resultado.total());
        assertEquals(3, resultado.filas().size());
    }

    @Test
    void ordenDeterministicoPorFechaDescendente() throws Exception {
        Resultado resultado = listar(null, 1, 10);
        assertEquals("PED-2026-0003", resultado.filas().get(0).get(3).trim());
        assertEquals("PED-2026-0002", resultado.filas().get(1).get(3).trim());
        assertEquals("PED-2026-0001", resultado.filas().get(2).get(3).trim());
    }

    @Test
    void paginacionNormalYPaginaAlta() throws Exception {
        Resultado pagina1 = listar(null, 1, 2);
        assertEquals(3, pagina1.total());
        assertEquals(2, pagina1.filas().size());
        assertEquals("PED-2026-0003", pagina1.filas().get(0).get(3).trim());

        Resultado pagina2 = listar(null, 2, 2);
        assertEquals(3, pagina2.total());
        assertEquals(1, pagina2.filas().size());
        assertEquals("PED-2026-0001", pagina2.filas().get(0).get(3).trim());

        Resultado pagina3 = listar(null, 3, 2);
        assertEquals(3, pagina3.total());
        assertTrue(pagina3.filas().isEmpty());
    }

    @Test
    void filtroPorPedimentoFraccionYOperacion() throws Exception {
        Resultado porPedimento = listar("PED-2026-0001", 1, 10);
        assertEquals(1, porPedimento.total());
        assertEquals("F4", porPedimento.filas().get(0).get(1).trim());

        Resultado porFraccion = listar("90211001", 1, 10);
        assertEquals(1, porFraccion.total());
        assertEquals("PED-2026-0003", porFraccion.filas().get(0).get(3).trim());

        Resultado porOperacion = listar("1002", 1, 10);
        assertEquals(1, porOperacion.total());
        assertEquals("PED-2026-0002", porOperacion.filas().get(0).get(3).trim());
    }

    @Test
    void paginacionInvalidaEsRechazada() {
        SQLException error = assertThrows(SQLException.class, () -> listar(null, 1, 101));
        assertTrue(error.getMessage().contains("paginacion"), error.getMessage());
    }

    private static Resultado listar(String filtro, int pagina, int tamano) throws SQLException {
        try (Connection c = conectar(CALE);
             CallableStatement cs = c.prepareCall("{call dbo.APP24_Q_ANEXO30_REVISION_ENTRADAS_LISTAR(?, ?, ?, ?)}")) {
            if (filtro == null) cs.setNull(1, Types.VARCHAR);
            else cs.setString(1, filtro);
            cs.setInt(2, pagina);
            cs.setInt(3, tamano);
            cs.registerOutParameter(4, Types.BIGINT);
            cs.execute();

            List<String> columnas = new ArrayList<>();
            List<List<String>> filas = new ArrayList<>();
            try (ResultSet rs = cs.getResultSet()) {
                ResultSetMetaData meta = rs.getMetaData();
                for (int i = 1; i <= meta.getColumnCount(); i++) columnas.add(meta.getColumnLabel(i));
                while (rs.next()) {
                    List<String> fila = new ArrayList<>();
                    for (int i = 1; i <= meta.getColumnCount(); i++) {
                        Object valor = rs.getObject(i);
                        fila.add(valor == null ? null : valor.toString());
                    }
                    filas.add(fila);
                }
            }
            while (cs.getMoreResults() || cs.getUpdateCount() != -1) { /* drena */ }
            return new Resultado(columnas, filas, cs.getLong(4));
        }
    }

    private record Resultado(List<String> columnas, List<List<String>> filas, long total) {
    }

    private static void crearBase() throws Exception {
        try (Connection master = DriverManager.getConnection(url("master"), SQL.getUsername(), SQL.getPassword());
             Statement s = master.createStatement()) {
            s.execute("IF DB_ID('" + CALE + "') IS NULL CREATE DATABASE [" + CALE + "]");
        }
    }

    private static void aplicarFixtures() throws Exception {
        try (Connection cale = conectar(CALE); Statement s = cale.createStatement()) {
            s.execute("CREATE TABLE dbo.A31_ENTRADAS (" 
                    + "Entradaskey bigint NOT NULL PRIMARY KEY," 
                    + "Descarga varchar(5) NULL," 
                    + "Tipooperacion varchar(2) NULL," 
                    + "Pedimentoarmado varchar(30) NULL," 
                    + "Fecha datetime NULL," 
                    + "Fracccion varchar(10) NULL," 
                    + "Valocomercial numeric(18,4) NULL," 
                    + "Clavepedimento varchar(5) NULL," 
                    + "IVAFP21 numeric(18,4) NULL," 
                    + "IVAFP22 numeric(18,4) NULL," 
                    + "SALDO numeric(18,4) NULL," 
                    + "PEDIMENTOORIGINAL varchar(30) NULL," 
                    + "FECHAORIGINAL datetime NULL," 
                    + "ESAF varchar(5) NULL," 
                    + "OPERACION bigint NULL," 
                    + "PARTIDA varchar(10) NULL)");
        }
    }

    private static void aplicarSpReal() throws Exception {
        try (Connection cale = conectar(CALE)) {
            aplicarArchivo(cale, raizRepo().resolve("procedures/queries/APP24_Q_ANEXO30_REVISION_ENTRADAS_LISTAR.sql"));
        }
    }

    private static Path raizRepo() {
        Path desdeBackend = Path.of("..", "infra", "sql");
        return Files.exists(desdeBackend) ? desdeBackend : Path.of("infra", "sql");
    }

    private static boolean dockerDisponible() {
        try {
            return DockerClientFactory.instance().isDockerAvailable();
        } catch (Throwable error) {
            return false;
        }
    }

    private static String url(String db) {
        return "jdbc:sqlserver://" + SQL.getHost() + ":" + SQL.getMappedPort(1433) 
                + ";databaseName=" + db + ";encrypt=false;trustServerCertificate=true";
    }

    private static Connection conectar(String db) throws SQLException {
        return DriverManager.getConnection(url(db), SQL.getUsername(), SQL.getPassword());
    }

    /** Divide por lineas que son exactamente GO (case-insensitive). */
    private static void aplicarArchivo(Connection c, Path archivo) throws Exception {
        String texto = Files.readString(archivo);
        StringBuilder batch = new StringBuilder();
        List<String> batches = new ArrayList<>();
        for (String linea : texto.split("\r?\n", -1)) {
            if (linea.trim().equalsIgnoreCase("GO")) {
                if (!batch.toString().isBlank()) batches.add(batch.toString());
                batch.setLength(0);
            } else {
                batch.append(linea).append(System.lineSeparator());
            }
        }
        if (!batch.toString().isBlank()) batches.add(batch.toString());
        try (Statement s = c.createStatement()) {
            for (int i = 0; i < batches.size(); i++) {
                String b = batches.get(i);
                if (b.isBlank()) continue;
                try {
                    s.execute(b);
                    while (s.getMoreResults() || s.getUpdateCount() != -1) { /* drena */ }
                } catch (SQLException error) {
                    throw new IllegalStateException("Fallo aplicando " + archivo.getFileName() + " batch #" + i + " :: " + error.getMessage(), error);
                }
            }
        }
    }
}
