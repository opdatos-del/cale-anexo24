package com.jovycandy.anexo24.savedqueries;

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
import java.sql.Types;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** Pruebas de los SP versionados contra ANEXO24_DEV efímera; nunca conecta a LIVE. */
class ConsultaGuardadaSqlIT {
    private static final String APP = "ANEXO24_DEV";
    private static final long OWNER_A = 7001L;
    private static final long OWNER_B = 7002L;
    private static boolean disponible;

    @SuppressWarnings("resource")
    private static final MSSQLServerContainer<?> SQL =
            new MSSQLServerContainer<>("mcr.microsoft.com/mssql/server:2022-latest").acceptLicense();

    @BeforeAll
    static void iniciarFixtureApp24() throws Exception {
        disponible = dockerDisponible();
        if (!disponible && "true".equalsIgnoreCase(System.getenv("CI"))) {
            throw new IllegalStateException("Docker no disponible en CI: consulta guardada SQL IT obligatoria.");
        }
        Assumptions.assumeTrue(disponible, "Docker no disponible localmente; se omite fixture SQL.");
        SQL.start();
        crearBaseYUsuario();
        aplicarMigration();
    }

    @AfterAll
    static void detenerContenedor() {
        if (disponible) SQL.stop();
    }

    @BeforeEach
    void limpiarConsultas() throws Exception {
        try (Connection app = conectar(); Statement statement = app.createStatement()) {
            statement.execute("DELETE FROM app24.ConsultaGuardada");
        }
    }

    @Test
    void creaListaActualizaYEliminaAislandoPorUsuarioYAlcance() throws Exception {
        crear(OWNER_A, "Entradas", "ENTRADAS", "{\"from\":\"2026-10-01\",\"to\":\"2026-10-31\"}");
        crear(OWNER_A, "Reporte", "REPORTES", "{\"type\":\"entradas\"}");
        crear(OWNER_B, "Propia", "ENTRADAS", "{\"from\":\"2026-10-01\",\"to\":\"2026-10-31\"}");

        List<String> ownQueries = listar(OWNER_A, null);
        assertEquals(2, ownQueries.size());
        assertTrue(ownQueries.containsAll(List.of("Entradas", "Reporte")));
        assertEquals(List.of("Entradas"), listar(OWNER_A, "ENTRADAS"));
        assertEquals(List.of("Propia"), listar(OWNER_B, null));

        long id = idPorNombre(OWNER_A, "Entradas");
        actualizar(id, OWNER_A, "Octubre", "ENTRADAS", "{\"from\":\"2026-10-01\",\"to\":\"2026-10-31\"}");
        assertEquals(List.of("Octubre"), listar(OWNER_A, "ENTRADAS"));
        assertSqlError(51202, () -> actualizar(id, OWNER_B, "Robada", "ENTRADAS", "{}"));
        assertSqlError(51202, () -> eliminar(id, OWNER_B));
        eliminar(id, OWNER_A);
        assertEquals(List.of(), listar(OWNER_A, "ENTRADAS"));
    }

    @Test
    void rechazaDuplicadoScopeInvalidoYJsonInvalido() throws Exception {
        crear(OWNER_A, "Mismo nombre", "ENTRADAS", "{}");
        assertSqlError(51203, () -> crear(OWNER_A, "Mismo nombre", "ENTRADAS", "{}"));
        assertSqlError(51201, () -> crear(OWNER_A, "Scope", "NO_VALIDO", "{}"));
        assertSqlError(51201, () -> crear(OWNER_A, "JSON", "ENTRADAS", "not-json"));
        assertEquals(List.of("Mismo nombre"), listar(OWNER_A, null));
    }

    @Test
    void limitaCienConsultasPorPropietario() throws Exception {
        for (int i = 1; i <= 100; i++) crear(OWNER_A, "Consulta " + i, "ENTRADAS", "{}");
        assertSqlError(51204, () -> crear(OWNER_A, "Consulta 101", "ENTRADAS", "{}"));
        assertEquals(100, listar(OWNER_A, null).size());
        crear(OWNER_B, "Consulta 1", "ENTRADAS", "{}");
        assertEquals(1, listar(OWNER_B, null).size());
    }

    private static void crearBaseYUsuario() throws Exception {
        try (Connection master = DriverManager.getConnection(url("master"), SQL.getUsername(), SQL.getPassword());
             Statement statement = master.createStatement()) {
            statement.execute("CREATE DATABASE [" + APP + "]");
        }
        try (Connection app = conectar(); Statement statement = app.createStatement()) {
            statement.execute("CREATE SCHEMA app24");
            statement.execute("CREATE TABLE app24.UsuarioApp (id BIGINT IDENTITY(1,1) NOT NULL PRIMARY KEY)");
            statement.execute("SET IDENTITY_INSERT app24.UsuarioApp ON; INSERT INTO app24.UsuarioApp (id) VALUES (7001), (7002); SET IDENTITY_INSERT app24.UsuarioApp OFF");
        }
    }

    private static void aplicarMigration() throws Exception {
        Path root = Path.of("..", "infra", "sql");
        if (!Files.exists(root)) root = Path.of("infra", "sql");
        Path migration = root.resolve("migrations/20-saved-queries-v1.sql");
        String text = Files.readString(migration);
        try (Connection app = conectar(); Statement statement = app.createStatement()) {
            String[] batches = text.split("(?im)^\s*GO\s*$");
            for (int i = 0; i < batches.length; i++) {
                if (batches[i].isBlank()) continue;
                try {
                    statement.execute(batches[i]);
                    while (statement.getMoreResults() || statement.getUpdateCount() != -1) {
                        // Drena resultados de lotes de DDL y procedures.
                    }
                } catch (SQLException error) {
                    throw new IllegalStateException("Fallo en migration 20, lote " + i, error);
                }
            }
        }
    }

    private static List<String> listar(long owner, String scope) throws SQLException {
        List<String> names = new ArrayList<>();
        try (Connection app = conectar(); CallableStatement call = app.prepareCall("{call app24.APP24_Q_CONSULTAS_GUARDADAS_LISTAR(?, ?)}")) {
            call.setLong(1, owner);
            if (scope == null) call.setNull(2, Types.VARCHAR); else call.setString(2, scope);
            boolean hasRows = call.execute();
            if (hasRows) try (ResultSet rows = call.getResultSet()) {
                while (rows.next()) names.add(rows.getString("nombre"));
            }
        }
        return names;
    }

    private static long idPorNombre(long owner, String name) throws SQLException {
        try (Connection app = conectar(); CallableStatement call = app.prepareCall("{call app24.APP24_Q_CONSULTAS_GUARDADAS_LISTAR(?, ?)}")) {
            call.setLong(1, owner);
            call.setNull(2, Types.VARCHAR);
            if (!call.execute()) throw new AssertionError("El SP no devolvió resultset.");
            try (ResultSet rows = call.getResultSet()) {
                while (rows.next()) if (name.equals(rows.getString("nombre"))) return rows.getLong("id");
            }
        }
        throw new AssertionError("Consulta guardada inexistente en fixture.");
    }

    private static void crear(long owner, String name, String scope, String criteria) throws SQLException {
        try (Connection app = conectar(); CallableStatement call = app.prepareCall("{call app24.APP24_C_CONSULTA_GUARDADA_CREAR(?, ?, ?, ?, ?)}")) {
            call.setLong(1, owner);
            call.setNString(2, name);
            call.setNull(3, Types.NVARCHAR);
            call.setString(4, scope);
            call.setNString(5, criteria);
            call.execute();
        }
    }

    private static void actualizar(long id, long owner, String name, String scope, String criteria) throws SQLException {
        try (Connection app = conectar(); CallableStatement call = app.prepareCall("{call app24.APP24_C_CONSULTA_GUARDADA_ACTUALIZAR(?, ?, ?, ?, ?, ?)}")) {
            call.setLong(1, id);
            call.setLong(2, owner);
            call.setNString(3, name);
            call.setNull(4, Types.NVARCHAR);
            call.setString(5, scope);
            call.setNString(6, criteria);
            call.execute();
        }
    }

    private static void eliminar(long id, long owner) throws SQLException {
        try (Connection app = conectar(); CallableStatement call = app.prepareCall("{call app24.APP24_C_CONSULTA_GUARDADA_ELIMINAR(?, ?)}")) {
            call.setLong(1, id);
            call.setLong(2, owner);
            call.execute();
        }
    }

    private static void assertSqlError(int code, SqlOperation operation) {
        SQLException error = assertThrows(SQLException.class, operation::execute);
        assertTrue(error.getErrorCode() == code || String.valueOf(error.getMessage()).contains(String.valueOf(code)), error.toString());
    }

    private static boolean dockerDisponible() {
        try { return DockerClientFactory.instance().isDockerAvailable(); }
        catch (RuntimeException error) { return false; }
    }

    private static String url(String database) {
        return "jdbc:sqlserver://" + SQL.getHost() + ":" + SQL.getMappedPort(1433)
                + ";databaseName=" + database + ";encrypt=false;trustServerCertificate=true";
    }

    private static Connection conectar() throws SQLException {
        return DriverManager.getConnection(url(APP), SQL.getUsername(), SQL.getPassword());
    }

    @FunctionalInterface
    private interface SqlOperation { void execute() throws SQLException; }
}
