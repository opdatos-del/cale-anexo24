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
import java.sql.*;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/** Prueba el contrato read-only del inventario inicial con fixture sintético. */
class Anexo30InventarioInicialSqlIT {
    private static final String DB = "CALE_IMMEX";
    private static final MSSQLServerContainer<?> SQL = new MSSQLServerContainer<>("mcr.microsoft.com/mssql/server:2022-latest").acceptLicense();
    private static boolean disponible;

    @BeforeAll static void iniciar() throws Exception {
        disponible = dockerDisponible();
        if (!disponible && "true".equalsIgnoreCase(System.getenv("CI"))) throw new IllegalStateException("Docker no disponible en CI.");
        Assumptions.assumeTrue(disponible, "Docker no disponible localmente; se omite la prueba SQL.");
        SQL.start(); crearBase(); aplicarSp();
    }
    @AfterAll static void detener() { if (disponible) SQL.stop(); }

    @BeforeEach void sembrar() throws Exception {
        try (Connection c = conectar(); Statement s = c.createStatement()) {
            s.execute("DELETE FROM dbo.INVENTARIOINICIAL");
            s.execute("INSERT dbo.INVENTARIOINICIAL ([Patente],[Número de pedimento],[Clave sección aduanera],[Fecha de selección del pedimento],[Fracción arancelaria o subpartida],[Valor comercial histórico],[Identificador de Activo fijo]) VALUES "
                    + "('1234','0000001','240','2026-01-10', '84715002',100.25,'NO'),"
                    + "('1234','0000002','240','2026-02-10', '84715002',200.50,'SI'),"
                    + "('5678','0000003','241','2026-03-10', '90211001',300.75,'NO')");
        }
    }

    @Test void devuelveProyeccionConteoYOrden() throws Exception {
        Resultado result = listar(null, 1, 20);
        assertEquals(List.of("PATENTE", "NUMERO_PEDIMENTO", "CLAVE_SECCION_ADUANERA", "FECHA_SELECCION", "FRACCION", "VALOR_COMERCIAL_HISTORICO", "IDENTIFICADOR_ACTIVO_FIJO"), result.columnas());
        assertEquals(3, result.total());
        assertEquals("0000003", result.filas().get(0).get(1).trim());
    }

    @Test void aplicaFiltroYPaginacion() throws Exception {
        Resultado filtro = listar("90211001", 1, 20);
        assertEquals(1, filtro.total());
        assertEquals("0000003", filtro.filas().get(0).get(1).trim());
        Resultado pagina = listar(null, 2, 2);
        assertEquals(3, pagina.total());
        assertEquals(1, pagina.filas().size());
    }

    @Test void rechazaTamanoInvalido() {
        SQLException error = assertThrows(SQLException.class, () -> listar(null, 1, 101));
        assertTrue(error.getMessage().toLowerCase().contains("paginacion"), error.getMessage());
    }

    private static Resultado listar(String filtro, int pagina, int tamano) throws SQLException {
        try (Connection c = conectar(); CallableStatement cs = c.prepareCall("{call dbo.APP24_Q_ANEXO30_INVENTARIO_INICIAL_LISTAR(?,?,?,?)}")) {
            if (filtro == null) cs.setNull(1, Types.VARCHAR); else cs.setString(1, filtro);
            cs.setInt(2, pagina); cs.setInt(3, tamano); cs.registerOutParameter(4, Types.BIGINT); cs.execute();
            List<String> columnas = new ArrayList<>(); List<List<String>> filas = new ArrayList<>();
            try (ResultSet rs = cs.getResultSet()) {
                ResultSetMetaData meta = rs.getMetaData();
                for (int i = 1; i <= meta.getColumnCount(); i++) columnas.add(meta.getColumnLabel(i));
                while (rs.next()) { List<String> fila = new ArrayList<>(); for (int i = 1; i <= meta.getColumnCount(); i++) fila.add(rs.getObject(i) == null ? null : rs.getObject(i).toString()); filas.add(fila); }
            }
            return new Resultado(columnas, filas, cs.getLong(4));
        }
    }
    private record Resultado(List<String> columnas, List<List<String>> filas, long total) {}
    private static void crearBase() throws Exception { try (Connection c=DriverManager.getConnection(url("master"),SQL.getUsername(),SQL.getPassword()); Statement s=c.createStatement()) { s.execute("IF DB_ID('"+DB+"') IS NULL CREATE DATABASE ["+DB+"]"); } }
    private static void aplicarSp() throws Exception { try (Connection c=conectar()) { String text=Files.readString(raiz().resolve("procedures/queries/APP24_Q_ANEXO30_INVENTARIO_INICIAL_LISTAR.sql")); try(Statement s=c.createStatement()){ for(String batch:text.split("(?im)^\s*GO\s*$")) if(!batch.isBlank()) s.execute(batch); } } }
    private static Path raiz() { Path p=Path.of("..","infra","sql"); return Files.exists(p)?p:Path.of("infra","sql"); }
    private static boolean dockerDisponible(){ try{return DockerClientFactory.instance().isDockerAvailable();}catch(Throwable e){return false;} }
    private static String url(String db){return "jdbc:sqlserver://"+SQL.getHost()+":"+SQL.getMappedPort(1433)+";databaseName="+db+";encrypt=false;trustServerCertificate=true";}
    private static Connection conectar() throws SQLException { return DriverManager.getConnection(url(DB),SQL.getUsername(),SQL.getPassword()); }
}
