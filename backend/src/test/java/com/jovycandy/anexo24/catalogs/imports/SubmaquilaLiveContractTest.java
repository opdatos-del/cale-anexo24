package com.jovycandy.anexo24.catalogs.imports;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.testcontainers.DockerClientFactory;
import org.testcontainers.containers.MSSQLServerContainer;

import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Reproduce, de forma ejecutable y sin tocar LIVE, que el contrato LIVE de
 * {@code dbo.CARGA_SUBMAQUILA} no es ejecutable contra el schema LIVE.
 *
 * <p>La fixture {@code CARGA_SUBMAQUILA.legacy.sql} es copia textual fiel del
 * {@code OBJECT_DEFINITION} LIVE 2026-10-05; el schema se toma de la metadata LIVE
 * ({@code 01-submaquila-live-schema.sql}). El SP escribe {@code 'SUBMAQUILA'} en
 * {@code SALIDAS.TipoOperacion}, que en LIVE es {@code INT}: la ejecución debe fallar.
 * Esto convierte la discrepancia auditada en una prueba reproducible (no sólo
 * documentación) y fija la clasificación {@code SP_EXISTING_UNSAFE}.</p>
 */
class SubmaquilaLiveContractTest {

    private static final String CALE = "CALE_IMMEX";

    @SuppressWarnings("resource") // El contenedor se cierra explícitamente en @AfterAll.
    static final MSSQLServerContainer<?> SQL =
            new MSSQLServerContainer<>("mcr.microsoft.com/mssql/server:2022-latest").acceptLicense();

    private static boolean disponible;

    @BeforeAll
    static void iniciarContenedor() throws Exception {
        disponible = dockerDisponible();
        if (!disponible && "true".equalsIgnoreCase(System.getenv("CI"))) {
            throw new IllegalStateException("Docker no disponible en CI: el contrato LIVE de submaquila es obligatorio "
                    + "(SUBMAQUILA_LIVE_CONTRACT_CI_REQUIRED).");
        }
        Assumptions.assumeTrue(disponible, "Docker no disponible localmente; se omite el arnés SQL.");
        SQL.start();
        try (Connection master = DriverManager.getConnection(url("master"), SQL.getUsername(), SQL.getPassword());
             Statement s = master.createStatement()) {
            s.execute("CREATE DATABASE [" + CALE + "]");
            s.execute("ALTER DATABASE [" + CALE + "] SET COMPATIBILITY_LEVEL = 100");
        }
        try (Connection cale = conectar(CALE)) {
            aplicarArchivo(cale, raizFixtures().resolve("01-submaquila-live-schema.sql"));
            aplicarArchivo(cale, raizFixtures().resolve("CARGA_SUBMAQUILA.legacy.sql"));
        }
    }

    @AfterAll
    static void detenerContenedor() {
        if (disponible) SQL.stop();
    }

    @Test
    void legacyLiveContractNoEsEjecutableConSchemaLive() throws Exception {
        try (Connection cale = conectar(CALE); Statement s = cale.createStatement()) {
            s.execute("INSERT INTO dbo.TMPSUBMAQUILA (FOLIO, FECHA, SUBMAQUILADOR, CLAVE, CANTIDAD, UNIDAD, DESCRIPCION, LINEA) "
                    + "VALUES ('F-LIVE', '2026-10-05', 'SUBMAQ Uno', 'P001', 10.5, 'PIEZ', 'Renglon', 1)");
        }

        SQLException error = assertThrows(SQLException.class, () -> {
            try (Connection cale = conectar(CALE); Statement s = cale.createStatement()) {
                s.execute("EXEC dbo.CARGA_SUBMAQUILA");
            }
        });

        String mensaje = error.getMessage().toLowerCase();
        assertTrue(mensaje.contains("convirt") || mensaje.contains("convert") || mensaje.contains("varchar"),
                "Se esperaba el error de conversion de 'SUBMAQUILA' a SALIDAS.TipoOperacion INT: " + error.getMessage());
        try (Connection cale = conectar(CALE); Statement s = cale.createStatement()) {
            assertEquals(0, scalar(s, "SELECT COUNT(*) FROM dbo.SALIDAS"),
                    "El contrato LIVE no debe crear salidas.");
            assertEquals(0, scalar(s, "SELECT COUNT(*) FROM dbo.PSALIDAS"),
                    "El contrato LIVE no debe crear partidas.");
        }
    }

    @Test
    void fixtureEsCopiaFielDelContratoLiveYNoUnaVersionIdealizada() throws Exception {
        String definicion = Files.readString(raizFixtures().resolve("CARGA_SUBMAQUILA.legacy.sql"));

        assertTrue(definicion.contains("LEGACY_EXECUTION_SAFE = NO"));
        assertTrue(definicion.contains("(SALIDAKEY,TipoOperacion,Documento,FECHA,Cve_cliente,CVE_PEDIMENTO)"),
                "La fixture debe usar las columnas LIVE (TipoOperacion/Cve_cliente).");
        assertTrue(definicion.contains("(PSALIDAKEY,FACTURA,FECHA,FRACCION,Descripcion,CANTIDAD,Unidad,Clave,Salidalink)"),
                "La fixture debe usar FACTURA/FECHA en PSALIDAS.");
        assertFalse(definicion.contains("Tipo_operacion"),
                "La fixture NO debe contener la version idealizada (Tipo_operacion/Transfiere).");
        assertFalse(definicion.contains("[partida]"),
                "El SP LIVE no asigna PSALIDAS.partida; la fixture no debe inventarlo.");
    }

    private static boolean dockerDisponible() {
        try {
            DockerClientFactory.instance().client();
            return true;
        } catch (Throwable noDisponible) {
            return false;
        }
    }

    private static Path raizFixtures() {
        Path desdeBackend = Path.of("src", "test", "resources", "sql", "submaquilas");
        return Files.exists(desdeBackend)
                ? desdeBackend
                : Path.of("backend", "src", "test", "resources", "sql", "submaquilas");
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
        List<String> lotes = List.of(texto.split("(?im)^\s*GO\s*$"));
        try (Statement s = conexion.createStatement()) {
            for (int i = 0; i < lotes.size(); i++) {
                if (lotes.get(i).isBlank()) continue;
                try {
                    s.execute(lotes.get(i));
                    while (s.getMoreResults() || s.getUpdateCount() != -1) {
                        // Drena resultados de DDL.
                    }
                } catch (SQLException error) {
                    throw new IllegalStateException("Fallo aplicando " + archivo.getFileName() + " lote " + i, error);
                }
            }
        }
    }

    private static int scalar(Statement s, String sql) throws SQLException {
        try (ResultSet rs = s.executeQuery(sql)) {
            rs.next();
            return rs.getInt(1);
        }
    }
}
