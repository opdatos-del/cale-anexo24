package com.jovycandy.anexo24.catalogs.imports;

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
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Types;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Contrato app24 del staging: {@code CONFIRMADA} es un estado exclusivamente terminal.
 *
 * <p>Los commands {@code APP24_C_CATALOGO_*_CARGA_CREAR} de cliente, proveedor y agente
 * sólo deben admitir {@code PREVISUALIZADA} o {@code CON_ERRORES}. El estado
 * {@code CONFIRMADA} lo establece exclusivamente el wrapper autoritativo de CALE_IMMEX
 * después de delegar en el SP legacy. Invocarlos con {@code CONFIRMADA} es un bypass
 * del ciclo de confirmación y debe fallar con {@code PARAMETRO_INVALIDO} sin crear
 * ninguna fila.</p>
 *
 * <p>Este arnés sólo necesita ANEXO24_DEV: no toca CALE_IMMEX ni ninguna base LIVE.</p>
 */
class CatalogStagingConfirmadaBypassTest {

    private static final String APP = "ANEXO24_DEV";

    private static final String[] CREAR_SP = {
            "app24.APP24_C_CATALOGO_CLIENTE_CARGA_CREAR",
            "app24.APP24_C_CATALOGO_PROVEEDOR_CARGA_CREAR",
            "app24.APP24_C_CATALOGO_AGENTE_CARGA_CREAR",
    };

    private static final String[] TABLA_CARGA = {
            "app24.CargaCatalogoCliente",
            "app24.CargaCatalogoProveedor",
            "app24.CargaCatalogoAgente",
    };

    @SuppressWarnings("resource") // El contenedor se cierra explícitamente en @AfterAll.
    static final MSSQLServerContainer<?> SQL =
            new MSSQLServerContainer<>("mcr.microsoft.com/mssql/server:2022-latest").acceptLicense();

    private static boolean disponible;

    @BeforeAll
    static void iniciarContenedor() throws Exception {
        disponible = dockerDisponible();
        if (!disponible && "true".equalsIgnoreCase(System.getenv("CI"))) {
            throw new IllegalStateException("Docker no disponible en CI: el contrato de staging es obligatorio "
                    + "(CATALOG_STAGING_STATE_CI_REQUIRED).");
        }
        Assumptions.assumeTrue(disponible, "Docker no disponible localmente; se omite el contrato de staging.");
        SQL.start();
        crearEsquemaApp();
        // Sólo las tres migrations de staging; no se necesita CALE_IMMEX ni wrapper alguno.
        aplicarArchivo(conectar(APP), raizRepo().resolve("migrations/15-cliente-confirmar-state-permission.sql"));
        aplicarArchivo(conectar(APP), raizRepo().resolve("migrations/16-proveedor-confirmar-state-permission.sql"));
        aplicarArchivo(conectar(APP), raizRepo().resolve("migrations/17-agente-staging-confirmation-v1.sql"));
    }

    @AfterAll
    static void detenerContenedor() {
        if (disponible) SQL.stop();
    }

    @BeforeEach
    void limpiarDatos() throws Exception {
        try (Connection app = conectar(APP); Statement s = app.createStatement()) {
            for (String tabla : TABLA_CARGA) {
                s.execute("IF OBJECT_ID('" + tabla + "Fila', 'U') IS NOT NULL DELETE FROM " + tabla + "Fila");
                s.execute("IF OBJECT_ID('" + tabla + "', 'U') IS NOT NULL DELETE FROM " + tabla);
            }
        }
    }

    @Test
    void crearClienteRechazaEstadoConfirmada() throws Exception {
        assertRechazaConfirmada(0, "app24.CargaCatalogoCliente");
    }

    @Test
    void crearProveedorRechazaEstadoConfirmada() throws Exception {
        assertRechazaConfirmada(1, "app24.CargaCatalogoProveedor");
    }

    @Test
    void crearAgenteRechazaEstadoConfirmada() throws Exception {
        assertRechazaConfirmada(2, "app24.CargaCatalogoAgente");
    }

    @Test
    void crearAceptaPrevisualizadaYConErrores() throws Exception {
        for (int i = 0; i < CREAR_SP.length; i++) {
            for (String estado : List.of("PREVISUALIZADA", "CON_ERRORES")) {
                long id = invocarCrear(i, estado, 1, 1);
                assertTrue(id > 0, CREAR_SP[i] + " no devolvió id con estado " + estado);
                assertEquals(estado, estadoDe(i, id), CREAR_SP[i] + " no persistió el estado " + estado);
            }
        }
        for (String tabla : TABLA_CARGA) {
            assertEquals(2, contar("SELECT COUNT(*) FROM " + tabla), tabla + " debería tener 2 cargas válidas.");
        }
    }

    @Test
    void estadoConfirmadaSigueSiendoValidoEnLaTabla() throws Exception {
        // La restricción es de CREACIÓN, no de estado persistente: el wrapper actualiza a CONFIRMADA.
        for (int i = 0; i < CREAR_SP.length; i++) {
            long id = invocarCrear(i, "PREVISUALIZADA", 1, 1);
            try (Connection app = conectar(APP);
                 Statement s = app.createStatement()) {
                s.executeUpdate("UPDATE " + TABLA_CARGA[i] + " SET estado='CONFIRMADA', fecha_confirmacion=SYSUTCDATETIME() WHERE id=" + id);
            }
            assertEquals("CONFIRMADA", estadoDe(i, id), TABLA_CARGA[i] + " no debe rechazar CONFIRMADA al actualizarse.");
        }
    }

    private void assertRechazaConfirmada(int indice, String tabla) throws Exception {
        assertEquals(0, contar("SELECT COUNT(*) FROM " + tabla), "La tabla debe arrancar vacía.");

        SQLException error = assertThrows(SQLException.class,
                () -> invocarCrear(indice, "CONFIRMADA", 1, 1),
                CREAR_SP[indice] + " debió rechazar @Estado='CONFIRMADA'.");

        assertTrue(error.getMessage().contains("PARAMETRO_INVALIDO"),
                "Se esperaba PARAMETRO_INVALIDO pero llegó: " + error.getMessage());
        assertEquals(0, contar("SELECT COUNT(*) FROM " + tabla),
                CREAR_SP[indice] + " creó una carga pese al bypass de CONFIRMADA.");
    }

    private static final AtomicInteger SECUENCIA = new AtomicInteger();

    private static String hashUnico() {
        // Hash único por invocación para no chocar con UQ_*_hash dentro de un mismo test.
        return "b" + String.format("%063d", SECUENCIA.incrementAndGet());
    }

    private long invocarCrear(int indice, String estado, int totalFilas, int filasValidas) throws Exception {
        String filasJson = filasValidas > 0
                ? "[{\"hoja\":\"CATALOGO\",\"fila\":2,\"datos\":{\"Clave\":\"AAA0000001\",\"Nombre\":\"Sintetico\"}}]"
                : "[]";
        try (Connection app = conectar(APP);
             CallableStatement cs = app.prepareCall(
                     "EXEC " + CREAR_SP[indice]
                             + " @Archivo=?, @Hash=?, @UsuarioId=?, @Estado=?, @TotalFilas=?, @FilasValidas=?,"
                             + " @VersionContrato=?, @CorrelationId=?, @FilasJson=?, @ErroresJson=?, @CargaId=?")) {
            cs.setString(1, "bypass.xlsx");
            cs.setString(2, hashUnico());
            cs.setLong(3, 7001L);
            cs.setString(4, estado);
            cs.setInt(5, totalFilas);
            cs.setInt(6, filasValidas);
            cs.setString(7, "CONTRATO-V1");
            cs.setString(8, "bypass-correlation");
            cs.setNString(9, filasJson);
            cs.setNString(10, "[]");
            cs.registerOutParameter(11, Types.BIGINT);
            cs.execute();
            long id = cs.getLong(11);
            while (cs.getMoreResults() || cs.getUpdateCount() != -1) {
                // Drena cualquier resultado residual del command.
            }
            return id;
        }
    }

    private String estadoDe(int indice, long id) throws SQLException {
        try (Connection app = conectar(APP);
             Statement s = app.createStatement();
             var rs = s.executeQuery("SELECT estado FROM " + TABLA_CARGA[indice] + " WHERE id=" + id)) {
            assertTrue(rs.next(), "No se encontró la carga " + id + " en " + TABLA_CARGA[indice] + ".");
            return rs.getString(1);
        }
    }

    private int contar(String sql) throws SQLException {
        try (Connection app = conectar(APP); Statement s = app.createStatement(); var rs = s.executeQuery(sql)) {
            rs.next();
            return rs.getInt(1);
        }
    }

    private static boolean dockerDisponible() {
        try {
            return DockerClientFactory.instance().isDockerAvailable();
        } catch (RuntimeException error) {
            return false;
        }
    }

    private static void crearEsquemaApp() throws SQLException {
        try (Connection master = DriverManager.getConnection(url("master"), SQL.getUsername(), SQL.getPassword());
             Statement s = master.createStatement()) {
            s.execute("CREATE DATABASE [" + APP + "]");
        }
        try (Connection app = conectar(APP); Statement s = app.createStatement()) {
            s.execute("IF SCHEMA_ID('app24') IS NULL EXEC('CREATE SCHEMA app24')");
            s.execute("IF OBJECT_ID('app24.PerfilApp', 'U') IS NULL CREATE TABLE app24.PerfilApp (id INT IDENTITY(1,1) NOT NULL PRIMARY KEY, nombre VARCHAR(100) NOT NULL UNIQUE)");
            s.execute("SET IDENTITY_INSERT app24.PerfilApp ON; INSERT INTO app24.PerfilApp (id, nombre) VALUES (1, 'ADMINISTRADOR'); SET IDENTITY_INSERT app24.PerfilApp OFF");
            s.execute("IF OBJECT_ID('app24.Actividad', 'U') IS NULL CREATE TABLE app24.Actividad (id INT IDENTITY(1,1) NOT NULL PRIMARY KEY, clave VARCHAR(100) NOT NULL UNIQUE, nombre VARCHAR(200) NOT NULL, recurso VARCHAR(100) NOT NULL, accion VARCHAR(100) NOT NULL)");
            s.execute("IF OBJECT_ID('app24.PerfilActividad', 'U') IS NULL CREATE TABLE app24.PerfilActividad (perfil_id INT NOT NULL, actividad_id INT NOT NULL, CONSTRAINT PK_PerfilActividad PRIMARY KEY (perfil_id, actividad_id))");
            s.execute("IF OBJECT_ID('app24.UsuarioApp', 'U') IS NULL CREATE TABLE app24.UsuarioApp (id BIGINT IDENTITY(1,1) NOT NULL PRIMARY KEY)");
            s.execute("SET IDENTITY_INSERT app24.UsuarioApp ON; INSERT INTO app24.UsuarioApp (id) VALUES (7001); SET IDENTITY_INSERT app24.UsuarioApp OFF");
            s.execute("IF OBJECT_ID('app24.BitacoraEvento', 'U') IS NULL CREATE TABLE app24.BitacoraEvento (id BIGINT IDENTITY(1,1) NOT NULL PRIMARY KEY, usuario_id BIGINT NOT NULL, modulo VARCHAR(100) NOT NULL, accion VARCHAR(80) NOT NULL, detalle VARCHAR(500) NOT NULL, correlation_id VARCHAR(100) NOT NULL, resultado VARCHAR(50) NOT NULL, fecha DATETIME2 NOT NULL DEFAULT SYSUTCDATETIME())");
            s.execute("IF OBJECT_ID('app24.APP24_C_BITACORA_REGISTRAR', 'P') IS NULL EXEC('CREATE PROCEDURE app24.APP24_C_BITACORA_REGISTRAR @UsuarioId BIGINT, @Modulo VARCHAR(100), @Accion VARCHAR(80), @Detalle VARCHAR(500), @CorrelacionId VARCHAR(100), @Resultado VARCHAR(50), @EventoId BIGINT OUTPUT AS BEGIN SET NOCOUNT ON; INSERT INTO app24.BitacoraEvento (usuario_id, modulo, accion, detalle, correlation_id, resultado) VALUES (@UsuarioId, @Modulo, @Accion, @Detalle, @CorrelacionId, @Resultado); SET @EventoId = SCOPE_IDENTITY(); END')");
        }
    }

    private static Path raizRepo() {
        Path desdeBackend = Path.of("..", "infra", "sql");
        return Files.exists(desdeBackend) ? desdeBackend : Path.of("infra", "sql");
    }

    private static String url(String db) {
        return "jdbc:sqlserver://" + SQL.getHost() + ":" + SQL.getMappedPort(1433)
                + ";databaseName=" + db + ";encrypt=false;trustServerCertificate=true";
    }

    private static Connection conectar(String db) throws SQLException {
        return DriverManager.getConnection(url(db), SQL.getUsername(), SQL.getPassword());
    }

    private static void aplicarArchivo(Connection conexion, Path archivo) throws Exception {
        String texto = Files.readString(archivo);
        List<String> lotes = List.of(texto.split("(?im)^\\s*GO\\s*$"));
        try (Statement s = conexion.createStatement()) {
            for (int i = 0; i < lotes.size(); i++) {
                if (lotes.get(i).isBlank()) continue;
                try {
                    s.execute(lotes.get(i));
                    while (s.getMoreResults() || s.getUpdateCount() != -1) {
                        // Drena resultados de DDL.
                    }
                } catch (SQLException error) {
                    throw new IllegalStateException("Fallo aplicando " + archivo.getFileName() + " lote " + i,
                            error);
                }
            }
        }
    }
}
