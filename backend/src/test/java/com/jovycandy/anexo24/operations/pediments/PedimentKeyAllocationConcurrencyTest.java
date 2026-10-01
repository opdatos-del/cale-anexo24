package com.jovycandy.anexo24.operations.pediments;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.testcontainers.DockerClientFactory;
import org.testcontainers.containers.MSSQLServerContainer;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

/**
 * Prueba aislada del problema de generación de claves {@code MAX(key)+1} y de la
 * estrategia propuesta de bloqueo de tabla. Usa un contenedor SQL Server efímero
 * con una tabla sintética; nunca escribe en {@code CALE_IMMEX}.
 */
class PedimentKeyAllocationConcurrencyTest {

    static final MSSQLServerContainer<?> SQL =
            new MSSQLServerContainer<>("mcr.microsoft.com/mssql/server:2022-latest").acceptLicense();

    @BeforeAll
    static void iniciarContenedor() {
        boolean disponible = dockerDisponible();
        // En CI el test es obligatorio: no se permite verde-con-skip.
        if (!disponible && "true".equalsIgnoreCase(System.getenv("CI"))) {
            throw new IllegalStateException("Docker no disponible en CI: el test de concurrencia es obligatorio "
                    + "(CONCURRENCY_TEST_CI_REQUIRED).");
        }
        Assumptions.assumeTrue(disponible, "Docker no disponible localmente; se omite la prueba de contenedor.");
        SQL.start();
    }

    private static boolean dockerDisponible() {
        try {
            DockerClientFactory.instance().client();
            return true;
        } catch (Throwable noDisponible) {
            return false;
        }
    }

    @AfterAll
    static void detenerContenedor() {
        SQL.stop();
    }

    @BeforeEach
    void prepararTabla() throws Exception {
        try (Connection c = conectar(); Statement s = c.createStatement()) {
            s.execute("IF OBJECT_ID('dbo.OperacionesSinteticas','U') IS NOT NULL DROP TABLE dbo.OperacionesSinteticas");
            s.execute("CREATE TABLE dbo.OperacionesSinteticas (OperacionKey INT NOT NULL "
                    + "CONSTRAINT PK_OperacionesSinteticas PRIMARY KEY, Documento VARCHAR(50) NOT NULL)");
        }
    }

    @Test
    void maxPlusOneSinBloqueoPuedeColisionar() throws Exception {
        try (Connection sesionNueva = conectar(); Connection sesionLegacy = conectar()) {
            sesionNueva.setAutoCommit(false);
            sesionLegacy.setAutoCommit(false);

            // Ambas sesiones leen el MAX antes de que ninguna inserte.
            int claveNueva = maxMasUno(sesionNueva, false);
            int claveLegacy = maxMasUno(sesionLegacy, false);
            assertEquals(claveNueva, claveLegacy);

            insertar(sesionNueva, claveNueva, "NUEVA");
            sesionNueva.commit();

            // La sesión legacy intenta usar la misma clave calculada: colisión de PK.
            assertThrows(SQLException.class, () -> insertar(sesionLegacy, claveLegacy, "LEGACY"));
            sesionLegacy.rollback();
        }
    }

    @Test
    void bloqueoDeTablaCoordinaConSesionLegacySinConocerElBloqueo() throws Exception {
        ExecutorService executor = Executors.newSingleThreadExecutor();
        CountDownLatch legacyLista = new CountDownLatch(1);
        try (Connection sesionNueva = conectar(); Connection sesionLegacy = conectar()) {
            sesionNueva.setAutoCommit(false);
            sesionLegacy.setAutoCommit(false);

            // NEW: toma TABLOCKX + HOLDLOCK, calcula MAX+1 y retiene la transacción.
            int claveNueva = maxMasUno(sesionNueva, true);

            Future<Integer> legacy = executor.submit(() -> {
                legacyLista.countDown();
                // LEGACY: MAX+1 plano, sin conocer el bloqueo de la sesión nueva.
                int clave = maxMasUno(sesionLegacy, false);
                insertar(sesionLegacy, clave, "LEGACY");
                sesionLegacy.commit();
                return clave;
            });

            legacyLista.await(5, TimeUnit.SECONDS);
            // Mientras NEW retiene el lock, LEGACY permanece bloqueada.
            assertThrows(TimeoutException.class, () -> legacy.get(2, TimeUnit.SECONDS));

            // NEW inserta y confirma, liberando el lock.
            insertar(sesionNueva, claveNueva, "NUEVA");
            sesionNueva.commit();

            int claveLegacy = legacy.get(15, TimeUnit.SECONDS);
            assertNotEquals(claveNueva, claveLegacy);
            assertEquals(2, contarFilas());
        } finally {
            executor.shutdownNow();
        }
    }

    private Connection conectar() throws SQLException {
        return DriverManager.getConnection(SQL.getJdbcUrl(), SQL.getUsername(), SQL.getPassword());
    }

    private int maxMasUno(Connection c, boolean bloqueoTabla) throws SQLException {
        String sql = "SELECT ISNULL(MAX(OperacionKey), 0) + 1 FROM dbo.OperacionesSinteticas"
                + (bloqueoTabla ? " WITH (TABLOCKX, HOLDLOCK)" : "");
        try (Statement s = c.createStatement(); ResultSet rs = s.executeQuery(sql)) {
            rs.next();
            return rs.getInt(1);
        }
    }

    private void insertar(Connection c, int clave, String documento) throws SQLException {
        try (Statement s = c.createStatement()) {
            s.executeUpdate("INSERT INTO dbo.OperacionesSinteticas (OperacionKey, Documento) VALUES ("
                    + clave + ", '" + documento + "')");
        }
    }

    private int contarFilas() throws SQLException {
        try (Connection c = conectar(); Statement s = c.createStatement();
             ResultSet rs = s.executeQuery("SELECT COUNT(*) FROM dbo.OperacionesSinteticas")) {
            rs.next();
            return rs.getInt(1);
        }
    }
}
