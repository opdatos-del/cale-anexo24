package com.jovycandy.anexo24.reports;

import org.junit.jupiter.api.*;
import org.testcontainers.DockerClientFactory;
import org.testcontainers.containers.MSSQLServerContainer;

import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.*;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;

/** SQL IT del contrato read-only de LEGACY-038 y su wrapper tecnico. */
class SaldosSqlIT {
    private static final String DB = "CALE_IMMEX";
    private static final MSSQLServerContainer<?> SQL =
            new MSSQLServerContainer<>("mcr.microsoft.com/mssql/server:2022-latest").acceptLicense();
    private static boolean docker;

    @BeforeAll
    static void start() throws Exception {
        docker = dockerAvailable();
        if (!docker && "true".equalsIgnoreCase(System.getenv("CI"))) {
            throw new IllegalStateException("Docker no disponible en CI: SaldosSqlIT es obligatorio.");
        }
        Assumptions.assumeTrue(docker, "Docker no disponible localmente; se omite la prueba SQL.");
        SQL.start();
        try (Connection c = DriverManager.getConnection(url("master"), SQL.getUsername(), SQL.getPassword());
             Statement s = c.createStatement()) {
            s.execute("CREATE DATABASE [" + DB + "]");
        }
        createFixture();
        applyRealWrapper();
    }

    @AfterAll
    static void stop() { if (docker) SQL.stop(); }

    @BeforeEach
    void seed() throws Exception {
        try (Connection c = connect(); Statement s = c.createStatement()) {
            s.execute("DELETE FROM dbo.Descarga; DELETE FROM dbo.Partidas; DELETE FROM dbo.Importaciones;");
            s.execute("INSERT dbo.Importaciones (Ipedimentokey,Numero_ped,Fecha,Cve_pedimento,Descarga,PedimentoOriginal,PaisOrigen,Factura,COVE) VALUES "
                    + "(1,'PED-1','2025-01-01T00:00:00','A1','SI','ORG-1','MX','FAC-1','COVE-1'),"
                    + "(2,'PED-1','2025-01-01T00:00:00','A1','SI',NULL,NULL,NULL,NULL),"
                    + "(3,'PED-2','2025-06-30T00:00:00','A1','SI','ORG-2','US','FAC-2','COVE-2'),"
                    + "(4,'PED-NO','2025-01-01T00:00:00','A1','SI',NULL,NULL,NULL,NULL),"
                    + "(5,'PED-QTY','2025-01-01T00:00:00','A1','SI',NULL,NULL,NULL,NULL),"
                    + "(6,'PED-NOD','2025-01-01T00:00:00','A1','NO',NULL,NULL,NULL,NULL),"
                    + "(7,'PED-AF','2025-01-01T00:00:00','AF','SI',NULL,NULL,NULL,NULL)");
            s.execute("INSERT dbo.Partidas (Partidakey,Importacionlink,Categoria,Cantidad,Saldo,clave,Fraccion,Unidad,Val_aduanal,Val_dolares,lote,Complemento1,Complemento2,Complemento3,NICO) VALUES "
                    + "(101,1,'CAT',10,6,'MAT-1','01010101','KG',1000,100,'L1','C1',NULL,'C3','N1'),"
                    + "(102,2,'CAT',20,0,'MAT-2','01010102','PZ',2000,200,'L2',NULL,NULL,NULL,'N2'),"
                    + "(103,3,'CAT',30,4,'MAT-3','01010103','KG',3000,300,NULL,NULL,NULL,NULL,NULL),"
                    + "(104,4,'NO',10,5,'MAT-NO','01010104','KG',100,10,NULL,NULL,NULL,NULL,NULL),"
                    + "(105,5,'CAT',0,5,'MAT-QTY','01010105','KG',100,10,NULL,NULL,NULL,NULL,NULL),"
                    + "(106,6,'CAT',10,5,'MAT-NOD','01010106','KG',100,10,NULL,NULL,NULL,NULL,NULL),"
                    + "(107,7,'CAT',10,5,'MAT-AF','01010107','KG',100,10,NULL,NULL,NULL,NULL,NULL)");
            s.execute("INSERT dbo.Descarga (pentradalink,desperdicio,saldodesperdicio) VALUES (102,2,3)");
        }
    }

    @Test
    void legacyExpone37ColumnasYAplicaFiltros() throws Exception {
        Result result = callLegacy(null, null, null);
        assertEquals(37, result.columns.size());
        assertEquals(3, result.rows.size());
        List<String> firstPart = result.rows.stream().filter(row -> "MAT-1".equals(row.get(5))).findFirst().orElseThrow();
        List<String> wastePart = result.rows.stream().filter(row -> "MAT-2".equals(row.get(5))).findFirst().orElseThrow();
        assertTrue(result.rows.stream().anyMatch(row -> "MAT-3".equals(row.get(5))));
        assertEquals("TM-MAT-1", firstPart.get(27));
        assertEquals("2.0000", wastePart.get(23));
        assertEquals("3.0000", wastePart.get(24));
        assertEquals("2025-07-01 10:00:00.0", firstPart.get(16));
    }

    @Test
    void wrapperConservaParidad37ColumnasYValores() throws Exception {
        Result legacy = callLegacy(null, null, null);
        Result wrapper = callWrapper(null, null, null, 1, 20);
        assertEquals(legacy.columns, wrapper.columns);
        assertEquals(sorted(legacy.rows), sorted(wrapper.rows));
    }

    @Test
    void filtrosFechaDocumentoYDocumentoAnulaRango() throws Exception {
        assertEquals(3, callWrapper("2025-01-01", "2025-06-30", null, 1, 20).total);
        assertEquals(1, callWrapper("2026-01-01", "2026-01-02", "PED-2", 1, 20).total);
        assertEquals(0, callWrapper(null, null, "PED-1X", 1, 20).total);
    }

    @Test
    void paginacionEstableNoDuplicaNiOmiteFilas() throws Exception {
        Result first = callWrapper(null, null, null, 1, 2);
        Result second = callWrapper(null, null, null, 2, 2);
        assertEquals(3, first.total);
        assertEquals(3, second.total);
        List<List<String>> rows = new ArrayList<>(first.rows);
        rows.addAll(second.rows);
        assertEquals(3, rows.size());
        assertEquals(3, rows.stream().map(row -> row.get(5)).distinct().count());
        assertTrue(callWrapper(null, null, null, 9, 2).rows.isEmpty());
    }

    @Test
    void runtimeEjecutaWrapperSinPermisoDirectoAlLegacy() throws Exception {
        try (Connection c = connect(); Statement s = c.createStatement()) {
            s.execute("CREATE USER [saldos_runtime_test] WITHOUT LOGIN");
            s.execute("GRANT EXECUTE ON OBJECT::dbo.APP24_Q_SALDOS_LISTAR TO [saldos_runtime_test]");
            s.execute("EXECUTE AS USER = 'saldos_runtime_test'");
            try {
                try (CallableStatement cs = c.prepareCall("{call dbo.APP24_Q_SALDOS_LISTAR(?,?,?,?,?,?)}")) {
                    setDates(cs, null, null, null);
                    cs.setInt(4, 1);
                    cs.setInt(5, 20);
                    cs.registerOutParameter(6, Types.BIGINT);
                    cs.execute();
                    assertEquals(3, cs.getLong(6));
                }
                try (ResultSet rs = s.executeQuery("SELECT HAS_PERMS_BY_NAME('dbo.PR_INFORME_SALDOS','OBJECT','EXECUTE')")) {
                    assertTrue(rs.next());
                    assertEquals(0, rs.getInt(1));
                }
            } finally {
                s.execute("REVERT");
                s.execute("DROP USER [saldos_runtime_test]");
            }
        }
    }

    @Test
    void paginacionInvalidaEsRechazada() throws Exception {
        SQLException error = assertThrows(SQLException.class, () -> callWrapper(null, null, null, 1, 101));
        assertTrue(error.getMessage().toLowerCase(Locale.ROOT).contains("pagin"));
    }

    private static Result callLegacy(String from, String to, String document) throws SQLException {
        try (Connection c = connect(); CallableStatement cs = c.prepareCall("{call dbo.PR_INFORME_SALDOS(?,?,?)}")) {
            setDates(cs, from, to, document);
            cs.execute();
            return read(cs.getResultSet());
        }
    }

    private static Result callWrapper(String from, String to, String document, int page, int size) throws SQLException {
        try (Connection c = connect(); CallableStatement cs = c.prepareCall("{call dbo.APP24_Q_SALDOS_LISTAR(?,?,?,?,?,?)}")) {
            setDates(cs, from, to, document);
            cs.setInt(4, page);
            cs.setInt(5, size);
            cs.registerOutParameter(6, Types.BIGINT);
            cs.execute();
            Result result = read(cs.getResultSet());
            return new Result(result.columns, result.rows, cs.getLong(6));
        }
    }

    private static void setDates(CallableStatement cs, String from, String to, String document) throws SQLException {
        if (from == null) cs.setNull(1, Types.TIMESTAMP);
        else cs.setTimestamp(1, Timestamp.valueOf(from + " 00:00:00"));
        if (to == null) cs.setNull(2, Types.TIMESTAMP);
        else cs.setTimestamp(2, Timestamp.valueOf(to + " 00:00:00"));
        if (document == null) cs.setNull(3, Types.VARCHAR);
        else cs.setString(3, document);
    }

    private static Result read(ResultSet rs) throws SQLException {
        List<String> columns = new ArrayList<>();
        List<List<String>> rows = new ArrayList<>();
        ResultSetMetaData metadata = rs.getMetaData();
        for (int i = 1; i <= metadata.getColumnCount(); i++) columns.add(metadata.getColumnLabel(i));
        while (rs.next()) {
            List<String> row = new ArrayList<>();
            for (int i = 1; i <= metadata.getColumnCount(); i++) {
                Object value = rs.getObject(i);
                row.add(value == null ? null : value.toString());
            }
            rows.add(row);
        }
        return new Result(columns, rows, rows.size());
    }

    private static List<List<String>> sorted(List<List<String>> rows) {
        return rows.stream().sorted(Comparator.comparing(row -> String.valueOf(row.get(0)) + String.valueOf(row.get(5)))).toList();
    }

    private record Result(List<String> columns, List<List<String>> rows, long total) {}

    private static void createFixture() throws Exception {
        try (Connection c = connect(); Statement s = c.createStatement()) {
            s.execute("CREATE TABLE dbo.Importaciones (Ipedimentokey int primary key, Numero_ped char(20), Fecha datetime, Cve_pedimento char(5), Descarga char(2), PedimentoOriginal varchar(19), PaisOrigen varchar(10), Factura char(50), COVE varchar(100))");
            s.execute("CREATE TABLE dbo.Partidas (Partidakey int primary key, Importacionlink int, Categoria char(5), Cantidad numeric(18,4), Saldo numeric(18,4), clave varchar(50), Fraccion varchar(15), Unidad varchar(10), Val_aduanal numeric(18,4), Val_dolares numeric(18,4), lote varchar(2), Complemento1 varchar(50), Complemento2 varchar(50), Complemento3 varchar(50), NICO varchar(10))");
            s.execute("CREATE TABLE dbo.Categorias (categoria char(5), meses int)");
            s.execute("INSERT dbo.Categorias VALUES ('CAT',6)");
            s.execute("CREATE TABLE dbo.Productomaterial (clave varchar(50), producto varchar(50))");
            s.execute("CREATE TABLE dbo.Material (CLAVE varchar(50), TIPOMATERIAL varchar(150))");
            s.execute("INSERT dbo.Material VALUES ('MAT-1','TM-MAT-1'),('MAT-2','TM-MAT-2'),('MAT-3','TM-MAT-3')");
            s.execute("CREATE TABLE dbo.Descarga (pentradalink int, desperdicio numeric(18,4), saldodesperdicio numeric(18,4))");
            s.execute("CREATE FUNCTION dbo.BUSCATIPOM(@CLAVE varchar(50)) RETURNS varchar(150) AS BEGIN RETURN (SELECT TOP 1 TIPOMATERIAL FROM dbo.Material WHERE CLAVE=@CLAVE) END");
            s.execute(legacyProcedure());
        }
    }

    private static String legacyProcedure() {
        return """
            CREATE PROCEDURE dbo.PR_INFORME_SALDOS
                @DESDE datetime = NULL, @HASTA datetime = NULL, @documento varchar(50) = NULL
            AS
            BEGIN
                SET NOCOUNT ON;
                IF ISNULL(@documento, '') = '' SET @documento = NULL;
                ELSE BEGIN SET @DESDE = NULL; SET @HASTA = NULL; END;
                DECLARE @res TABLE (
                    [Documento] char(20), [Fecha de Pago] datetime, [Clave Pedimento] char(5),
                    [Tipo de Operacion] char(25), [tc] float, [Clave] varchar(50), [Descripcion] varchar(250),
                    [Fraccion] varchar(15), [Cant. Importado] numeric(18,4), [Unidad] varchar(10), [Saldo] numeric(18,4),
                    [Valor Aduanal de Saldo] numeric(38,6), [Valor dolares del saldo] numeric(38,6), [Pais origen] varchar(10),
                    [Temporalidad(Meses)] int, [Categoria] char(5), [Fecha de Vencimiento] datetime,
                    [PedimentoOriginal] varchar(19), [Descarga] varchar(2), [lote] varchar(2),
                    [Complemento 1] varchar(50), [Complemento 2] varchar(50), [Complemento 3] varchar(50),
                    [Desperdiciado] numeric(38,4), [Saldodesperdicio] numeric(38,4), [COVE] varchar(100),
                    [Factura] char(50), [Tipo Material] varchar(150), [pu_vad] numeric(38,10), [pu_vdo] numeric(38,10),
                    [val_aduanal] numeric(18,4), [val_dolares] numeric(18,4), [saldo en UMT] float,
                    [unidadt] char(5), [valor en pesos] float, [Saldo en valor pesos] float, [NICO] varchar(10));
                INSERT @res
                SELECT i.Numero_ped, i.Fecha, i.Cve_pedimento, 'IM', 1, p.clave, NULL, p.Fraccion,
                       p.Cantidad, p.Unidad, p.Saldo, p.Val_aduanal / p.Cantidad * p.Saldo,
                       p.Val_dolares / p.Cantidad * p.Saldo, i.PaisOrigen, c.meses, p.Categoria,
                       DATEADD(month, c.meses, i.Fecha), i.PedimentoOriginal, i.Descarga, p.lote,
                       p.Complemento1, p.Complemento2, p.Complemento3,
                       SUM(ISNULL(d.desperdicio, 0)), SUM(ISNULL(d.saldodesperdicio, 0)), i.COVE, i.Factura,
                       dbo.BUSCATIPOM(p.clave), p.Val_aduanal / p.Cantidad, p.Val_dolares / p.Cantidad,
                       p.Val_aduanal, p.Val_dolares, p.Saldo, p.Unidad, p.Val_aduanal,
                       p.Val_aduanal * p.Saldo / IIF(p.Val_aduanal = 0, 1, p.Val_aduanal), p.NICO
                FROM dbo.Importaciones i
                JOIN dbo.Partidas p ON i.Ipedimentokey = p.Importacionlink
                LEFT JOIN dbo.Categorias c ON p.Categoria = c.categoria
                LEFT JOIN dbo.Descarga d ON d.pentradalink = p.Partidakey
                WHERE p.Categoria <> 'NO' AND p.Cantidad > 0
                  AND (ROUND(p.Saldo, 3) > 0 OR ISNULL((SELECT SUM(x.saldodesperdicio) FROM dbo.Descarga x WHERE x.pentradalink = p.Partidakey), 0) > 0)
                  AND i.Descarga = 'SI' AND i.Cve_pedimento <> 'AF'
                  AND i.Fecha BETWEEN COALESCE(@DESDE, i.Fecha) AND COALESCE(@HASTA, i.Fecha)
                  AND i.Numero_ped = COALESCE(@documento, i.Numero_ped)
                GROUP BY i.Numero_ped, i.Fecha, i.Cve_pedimento, p.clave, p.Fraccion, p.Cantidad, p.Unidad,
                         p.Saldo, p.Val_aduanal, p.Val_dolares, i.PaisOrigen, c.meses, p.Categoria,
                         i.PedimentoOriginal, i.Descarga, p.lote, p.Complemento1, p.Complemento2, p.Complemento3,
                         i.COVE, i.Factura, p.NICO;
                SELECT * FROM @res ORDER BY [Fecha de Pago], [Documento];
            END
            """;
    }

    private static void applyRealWrapper() throws Exception {
        Path file = Path.of("..", "infra", "sql", "procedures", "queries", "APP24_Q_SALDOS_LISTAR.sql");
        if (!Files.exists(file)) file = Path.of("infra", "sql", "procedures", "queries", "APP24_Q_SALDOS_LISTAR.sql");
        String text = Files.readString(file);
        try (Connection c = connect(); Statement s = c.createStatement()) {
            for (String batch : text.split("(?im)^\s*GO\s*$")) if (!batch.isBlank()) s.execute(batch);
        }
    }

    private static boolean dockerAvailable() {
        try { return DockerClientFactory.instance().isDockerAvailable(); }
        catch (Throwable error) { return false; }
    }

    private static String url(String db) {
        return "jdbc:sqlserver://" + SQL.getHost() + ":" + SQL.getMappedPort(1433)
                + ";databaseName=" + db + ";encrypt=false;trustServerCertificate=true";
    }

    private static Connection connect() throws SQLException {
        return DriverManager.getConnection(url(DB), SQL.getUsername(), SQL.getPassword());
    }
}
