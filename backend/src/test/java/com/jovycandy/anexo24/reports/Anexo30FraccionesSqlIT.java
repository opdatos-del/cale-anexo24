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

/** Verifica el contrato del SP versionado de fracciones A31 contra SQL Server efimero. */
class Anexo30FraccionesSqlIT {
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
            s.execute("CREATE TABLE dbo.A31_DESCARGASF (A31_FRACCIONKEY BIGINT IDENTITY PRIMARY KEY,TIPO VARCHAR(2) NULL,CLAVEPEDIMENTO VARCHAR(5) NULL,EJERCICIO VARCHAR(5) NULL,PERIODO VARCHAR(5) NULL,FRACCION VARCHAR(10) NULL,VALOR NUMERIC(18,4) NULL,AF VARCHAR(5) NULL,ARCHIVO VARCHAR(50) NULL)");
            aplicarArchivo(c, rutaSql());
        }
    }
    @AfterAll static void detener() { if (disponible) SQL.stop(); }

    @BeforeEach void sembrar() throws Exception {
        try (Connection c = conectar(DB); Statement s = c.createStatement()) {
            s.execute("TRUNCATE TABLE dbo.A31_DESCARGASF");
            s.execute("INSERT dbo.A31_DESCARGASF(TIPO,CLAVEPEDIMENTO,EJERCICIO,PERIODO,FRACCION,VALOR,AF,ARCHIVO) VALUES "
                    + "('F4','A1','2026','02','84715002',123.4567,'SI','uno.txt'),"
                    + "('F4','A1','2026','02','84715002',123.4567,'SI','dos.txt'),"
                    + "('A3','B2','2025','11','90211001',99.0001,'NO','tres.csv'),"
                    + "(NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL)");
        }
    }

    @Test void contratoVacioUnaFilaYMultiplesFilas() throws Exception {
        vaciar();
        Resultado vacio = listar(null, 1, 20);
        assertEquals(List.of("A31_FRACCION_KEY", "TIPO", "CLAVE_PEDIMENTO", "EJERCICIO", "PERIODO", "FRACCION", "VALOR", "AF", "ARCHIVO"), vacio.columnas());
        assertEquals(0, vacio.total());
        insertar("F4", "A1", "2026", "01", "84715002", "1.0000", "SI", "uno.txt");
        assertEquals(1, listar(null, 1, 20).filas().size());
        insertar("A3", "B2", "2025", "12", "90211001", "2.0000", "NO", "dos.txt");
        assertEquals(2, listar(null, 1, 20).total());
    }

    @Test void conservaPkFisicaTupleNoUnicoNullsYPrecision() throws Exception {
        Resultado resultado = listar(null, 1, 20);
        assertEquals(4, resultado.total());
        List<Map<String, Object>> duplicados = resultado.filas().stream().filter(f -> "84715002".equals(f.get("FRACCION"))).toList();
        assertEquals(2, duplicados.size());
        assertNotEquals(duplicados.get(0).get("A31_FRACCION_KEY"), duplicados.get(1).get("A31_FRACCION_KEY"));
        assertEquals(new BigDecimal("123.4567"), duplicados.get(0).get("VALOR"));
        assertTrue(resultado.filas().stream().anyMatch(f -> f.get("TIPO") == null && f.get("ARCHIVO") == null));
    }

    @Test void filtraPorCadaColumnaTecnicaYBlankNoFiltra() throws Exception {
        assertEquals(2, listar("F4", 1, 20).total());
        assertEquals(2, listar("A1", 1, 20).total());
        assertEquals(2, listar("2026", 1, 20).total());
        assertEquals(3, listar("02", 1, 20).total());
        assertEquals(2, listar("84715002", 1, 20).total());
        assertEquals(2, listar("SI", 1, 20).total());
        assertEquals(1, listar("uno.txt", 1, 20).total());
        assertEquals(listar(null, 1, 20).total(), listar("   ", 1, 20).total());
    }

    @Test void paginaAntesDelOffsetYOrdenDeterministico() throws Exception {
        Resultado pagina1 = listar(null, 1, 2);
        Resultado pagina2 = listar(null, 2, 2);
        Resultado fuera = listar(null, 9, 2);
        assertEquals(4, pagina1.total());
        assertEquals(2, pagina1.filas().size());
        assertEquals(2, pagina2.filas().size());
        assertTrue(fuera.filas().isEmpty());
        assertEquals("2026", pagina1.filas().get(0).get("EJERCICIO"));
        assertEquals("uno.txt", pagina1.filas().get(0).get("ARCHIVO"));
        assertEquals("dos.txt", pagina1.filas().get(1).get("ARCHIVO"));
    }

    @Test void rechazaPaginacionInvalida() {
        assertThrows(SQLException.class, () -> listar(null, 0, 20));
        assertThrows(SQLException.class, () -> listar(null, 1, 0));
        assertThrows(SQLException.class, () -> listar(null, 1, 101));
    }

    private static Resultado listar(String filtro, int pagina, int tamano) throws SQLException {
        try (Connection c = conectar(DB); CallableStatement cs = c.prepareCall("{call dbo.APP24_Q_ANEXO30_REVISION_FRACCIONES_LISTAR(?,?,?,?)}")) {
            if (filtro == null) cs.setNull(1, Types.VARCHAR); else cs.setString(1, filtro);
            cs.setInt(2, pagina); cs.setInt(3, tamano); cs.registerOutParameter(4, Types.BIGINT); cs.execute();
            List<String> columnas = new ArrayList<>(); List<Map<String, Object>> filas = new ArrayList<>();
            try (ResultSet rs = cs.getResultSet()) { ResultSetMetaData meta = rs.getMetaData(); for (int i=1;i<=meta.getColumnCount();i++) columnas.add(meta.getColumnLabel(i)); while (rs.next()) { Map<String,Object> fila = new LinkedHashMap<>(); for (String columna:columnas) fila.put(columna, rs.getObject(columna)); filas.add(fila); } }
            return new Resultado(columnas, filas, cs.getLong(4));
        }
    }
    private record Resultado(List<String> columnas, List<Map<String, Object>> filas, long total) { }
    private static void vaciar() throws Exception { try (Connection c=conectar(DB); Statement s=c.createStatement()) { s.execute("TRUNCATE TABLE dbo.A31_DESCARGASF"); } }
    private static void insertar(String tipo,String clave,String ejercicio,String periodo,String fraccion,String valor,String af,String archivo) throws Exception { try(Connection c=conectar(DB); Statement s=c.createStatement()){s.execute("INSERT dbo.A31_DESCARGASF(TIPO,CLAVEPEDIMENTO,EJERCICIO,PERIODO,FRACCION,VALOR,AF,ARCHIVO) VALUES('"+tipo+"','"+clave+"','"+ejercicio+"','"+periodo+"','"+fraccion+"',"+valor+",'"+af+"','"+archivo+"')");} }
    private static Connection conectar(String db) throws SQLException { return DriverManager.getConnection("jdbc:sqlserver://"+SQL.getHost()+":"+SQL.getMappedPort(1433)+";databaseName="+db+";encrypt=false;trustServerCertificate=true",SQL.getUsername(),SQL.getPassword()); }
    private static Path rutaSql() { Path p=Path.of("..","infra","sql","procedures","queries","APP24_Q_ANEXO30_REVISION_FRACCIONES_LISTAR.sql"); return Files.exists(p)?p:Path.of("infra","sql","procedures","queries","APP24_Q_ANEXO30_REVISION_FRACCIONES_LISTAR.sql"); }
    private static void aplicarArchivo(Connection c, Path archivo) throws Exception { StringBuilder batch=new StringBuilder(); try(Statement s=c.createStatement()){for(String linea:Files.readString(archivo).split("\r?\n",-1)){if(linea.trim().equalsIgnoreCase("GO")){ejecutarBatch(s,batch,archivo);batch.setLength(0);}else batch.append(linea).append(System.lineSeparator());} ejecutarBatch(s,batch,archivo);} }
    private static void ejecutarBatch(Statement s,StringBuilder batch,Path archivo) throws SQLException { if(!batch.toString().isBlank()) { try{s.execute(batch.toString());}catch(SQLException error){throw new IllegalStateException("Fallo aplicando "+archivo.getFileName()+": "+error.getMessage(),error);} } }
}
