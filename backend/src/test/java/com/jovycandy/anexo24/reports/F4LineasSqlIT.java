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
 * Prueba SQL del reporte read-only F4 (LEGACY-041) contra un SQL Server efímero.
 *
 * <p>Aplica el SP versionado real ({@code APP24_Q_F4_LISTAR}) sobre un fixture
 * sintético de {@code salidas}/{@code psalidas}/{@code dirigido} y las vistas
 * legacy {@code V_F4CTMA}/{@code V_F4DESP}; nunca se conecta a LIVE ni escribe
 * datos empresariales.</p>
 */
class F4LineasSqlIT {

    private static final String CALE = "CALE_IMMEX";

    static final MSSQLServerContainer<?> SQL =
            new MSSQLServerContainer<>("mcr.microsoft.com/mssql/server:2022-latest").acceptLicense();

    private static boolean disponible;

    @BeforeAll
    static void iniciarContenedor() throws Exception {
        disponible = dockerDisponible();
        if (!disponible && "true".equalsIgnoreCase(System.getenv("CI"))) {
            throw new IllegalStateException("Docker no disponible en CI: los tests SQL de reportes son "
                    + "obligatorios (CI_SQL_GATE_REQUIRED).");
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
            s.execute("DELETE FROM dbo.dirigido");
            s.execute("DELETE FROM dbo.psalidas");
            s.execute("DELETE FROM dbo.salidas");
            s.execute("INSERT INTO dbo.salidas (SalidaKey, Documento, Fecha, Cve_pedimento, TipoDescarga) VALUES "
                    + "(1, 'F4-DOC-1', '2026-01-15T10:00:00', 'F4', 'CTMAPAA'), "
                    + "(2, 'F4-DOC-2', '2026-02-20T10:00:00', 'A3', 'DESP'), "
                    + "(3, 'F4-DOC-3', '2026-03-10T10:00:00', 'F4', 'OTRO'), "
                    + "(4, 'F9-DOC-4', '2026-04-01T10:00:00', 'F9', 'CTMAPAA')");
            s.execute("INSERT INTO dbo.psalidas (Psalidakey, Salidalink) VALUES (11, 1), (12, 2), (13, 3), (14, 4)");
            s.execute("INSERT INTO dbo.dirigido (dirigidokey, documento, clave, incorporado, salidakey, psalidakey, saldo) VALUES "
                    + "(101, 'IMP-1', 'MAT-1', 10.5, 1, 11, 3.25), "
                    + "(102, 'IMP-2', 'MAT-2', 4.0, 2, 12, 0.0), "
                    + "(103, 'IMP-3', 'MAT-3', 1.0, 3, 13, 1.0), "
                    + "(104, 'IMP-4', 'MAT-4', 2.0, 4, 14, 2.0)");
        }
    }

    @Test
    void contratoDeColumnasYConteo() throws Exception {
        Resultado resultado = listar(null, 1, 10);

        assertEquals(List.of("TIPO_DESCARGA", "F4", "FECHA", "IMPORTACION", "CLAVE", "INCORPORADO", "SALDO"),
                resultado.columnas());
        assertEquals(2, resultado.total());
        assertEquals(2, resultado.filas().size());
        assertEquals("CTMAPAA", resultado.filas().get(1).get(0));
        assertEquals("DESP", resultado.filas().get(0).get(0));
        assertEquals("IMP-2", resultado.filas().get(0).get(3));
        assertEquals("MAT-2", resultado.filas().get(0).get(4));
    }

    @Test
    void ordenDeterministicoPorFechaDescendente() throws Exception {
        Resultado primera = listar(null, 1, 10);
        Resultado segunda = listar(null, 1, 10);

        assertEquals("F4-DOC-2", primera.filas().get(0).get(1).trim());
        assertEquals("F4-DOC-1", primera.filas().get(1).get(1).trim());
        assertEquals(primera.filas(), segunda.filas());
    }

    @Test
    void paginaNormalYPaginaAlta() throws Exception {
        Resultado pagina1 = listar(null, 1, 1);
        assertEquals(2, pagina1.total());
        assertEquals(1, pagina1.filas().size());
        assertEquals("F4-DOC-2", pagina1.filas().get(0).get(1).trim());

        Resultado pagina2 = listar(null, 2, 1);
        assertEquals(2, pagina2.total());
        assertEquals("F4-DOC-1", pagina2.filas().get(0).get(1).trim());

        Resultado pagina3 = listar(null, 3, 1);
        assertEquals(2, pagina3.total());
        assertTrue(pagina3.filas().isEmpty());
    }

    @Test
    void filtroPorTipoDescargaImportacionYClave() throws Exception {
        Resultado porTipo = listar("CTMAPAA", 1, 10);
        assertEquals(1, porTipo.total());
        assertEquals("F4-DOC-1", porTipo.filas().get(0).get(1).trim());

        Resultado porClave = listar("MAT-2", 1, 10);
        assertEquals(1, porClave.total());
        assertEquals("DESP", porClave.filas().get(0).get(0));

        Resultado porDocumento = listar("F4-DOC", 1, 10);
        assertEquals(2, porDocumento.total());
    }

    @Test
    void paginacionInvalidaEsRechazada() {
        SQLException error = assertThrows(SQLException.class, () -> listar(null, 1, 101));
        assertTrue(error.getMessage().contains("paginacion"), error.getMessage());
    }

    private static Resultado listar(String filtro, int pagina, int tamano) throws SQLException {
        try (Connection c = conectar(CALE);
             CallableStatement cs = c.prepareCall("{call dbo.APP24_Q_F4_LISTAR(?, ?, ?, ?)}")) {
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
        try (Connection cale = conectar(CALE); Statement s = cale.createStatement()) {
            // Reproduce la restricción demostrada en LIVE: CALE_IMMEX sin OPENJSON.
            s.execute("ALTER DATABASE [" + CALE + "] SET COMPATIBILITY_LEVEL = 100");
        }
    }

    private static void aplicarFixtures() throws Exception {
        try (Connection cale = conectar(CALE); Statement s = cale.createStatement()) {
            s.execute("CREATE TABLE dbo.salidas (SalidaKey int NOT NULL PRIMARY KEY, Documento char(60) NULL, "
                    + "Fecha datetime NULL, Cve_pedimento char(5) NULL, TipoDescarga varchar(10) NULL)");
            s.execute("CREATE TABLE dbo.psalidas (Psalidakey int NOT NULL PRIMARY KEY, Salidalink int NULL)");
            s.execute("CREATE TABLE dbo.dirigido (dirigidokey bigint NOT NULL PRIMARY KEY, documento varchar(35) NULL, "
                    + "clave varchar(50) NULL, incorporado float NULL, salidakey int NULL, psalidakey int NULL, saldo numeric(18,4) NULL)");
            s.execute("CREATE VIEW dbo.V_F4CTMA AS "
                    + "SELECT TOP (100) PERCENT dbo.salidas.Documento AS F4, dbo.salidas.Fecha, "
                    + "dbo.dirigido.documento AS IMPORTACION, dbo.dirigido.clave, dbo.dirigido.incorporado, "
                    + "dbo.salidas.TipoDescarga, dbo.dirigido.dirigidokey, dbo.dirigido.salidakey, "
                    + "dbo.dirigido.psalidakey, dbo.dirigido.saldo, dbo.salidas.Documento "
                    + "FROM dbo.salidas INNER JOIN dbo.psalidas ON dbo.salidas.SalidaKey = dbo.psalidas.Salidalink "
                    + "INNER JOIN dbo.dirigido ON dbo.salidas.SalidaKey = dbo.dirigido.salidakey "
                    + "AND dbo.psalidas.Psalidakey = dbo.dirigido.psalidakey "
                    + "WHERE dbo.salidas.Cve_pedimento IN ('F4','A3') AND dbo.salidas.TipoDescarga = 'CTMAPAA'");
            s.execute("CREATE VIEW dbo.V_F4DESP AS "
                    + "SELECT TOP (100) PERCENT dbo.salidas.Documento AS F4, dbo.salidas.Fecha, "
                    + "dbo.dirigido.documento AS IMPORTACION, dbo.dirigido.clave, dbo.dirigido.incorporado, "
                    + "dbo.salidas.TipoDescarga, dbo.dirigido.dirigidokey, dbo.dirigido.salidakey, "
                    + "dbo.dirigido.psalidakey, dbo.dirigido.saldo, dbo.salidas.Documento "
                    + "FROM dbo.salidas INNER JOIN dbo.psalidas ON dbo.salidas.SalidaKey = dbo.psalidas.Salidalink "
                    + "INNER JOIN dbo.dirigido ON dbo.salidas.SalidaKey = dbo.dirigido.salidakey "
                    + "AND dbo.psalidas.Psalidakey = dbo.dirigido.psalidakey "
                    + "WHERE dbo.salidas.Cve_pedimento IN ('F4','A3') AND dbo.salidas.TipoDescarga = 'DESP'");
        }
    }

    private static void aplicarSpReal() throws Exception {
        try (Connection cale = conectar(CALE)) {
            aplicarArchivo(cale, raizRepo().resolve("procedures/queries/APP24_Q_F4_LISTAR.sql"));
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

    /** Divide por líneas que son exactamente {@code GO} (case-insensitive). */
    private static void aplicarArchivo(Connection c, Path archivo) throws Exception {
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
            for (int i = 0; i < batches.size(); i++) {
                String b = batches.get(i);
                if (b.isBlank()) continue;
                try {
                    s.execute(b);
                    while (s.getMoreResults() || s.getUpdateCount() != -1) { /* drena */ }
                } catch (SQLException error) {
                    throw new IllegalStateException("Fallo aplicando " + archivo.getFileName() + " batch #" + i
                            + " :: " + error.getMessage(), error);
                }
            }
        }
    }
}
