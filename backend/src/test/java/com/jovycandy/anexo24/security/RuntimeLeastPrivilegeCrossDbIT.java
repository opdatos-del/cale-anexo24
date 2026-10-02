package com.jovycandy.anexo24.security;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;
import org.testcontainers.DockerClientFactory;
import org.testcontainers.containers.MSSQLServerContainer;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.Locale;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Spike reproducible del mecanismo cross-database para runtime least privilege
 * ({@code CALE_IMMEX → ANEXO24_DEV}) sobre un SQL Server efímero.
 *
 * <p>Simula con bases sintéticas {@code LP_SOURCE} (≈ CALE_IMMEX) y
 * {@code LP_TARGET} (≈ ANEXO24_DEV): el command vive en {@code LP_SOURCE} y debe
 * leer/actualizar staging y registrar bitácora en {@code LP_TARGET} con
 * {@code TRUSTWORTHY OFF} y {@code DB_CHAINING OFF}.</p>
 *
 * <p>Secuencia: baseline sin firma (debe denegar), intento de variantes de
 * module signing (certificado público espejo, certificado con clave privada,
 * certificado de servidor con login), verificación de permisos mínimos del
 * principal de contexto, batería negativa del runtime y ciclo de vida de la
 * firma frente a {@code ALTER PROCEDURE}.</p>
 *
 * <p>No toca LIVE, no usa datos empresariales y no crea identidades fuera del
 * contenedor. Para ejecución local ver {@code docs/03-diseno/runtime-least-privilege.md}
 * (se requiere {@code JAVA_TOOL_OPTIONS=-Dapi.version=1.44} con Docker Desktop actual).</p>
 */
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class RuntimeLeastPrivilegeCrossDbIT {

    private static final String FUENTE = "LP_SOURCE";
    private static final String DESTINO = "LP_TARGET";

    private static final String RUNTIME = "lp_app";
    private static final String RUNTIME_PASSWORD = "Sp1ke!LpRuntime#2026";

    private static final String CERT = "lp_spike_cert";
    private static final String CERT_SERVIDOR = "lp_spike_server_cert";
    private static final String LOGIN_CERT = "lp_spike_cert_login";
    private static final String USUARIO_CERT = "lp_spike_cert_user";
    private static final String USUARIO_LOGIN = "lp_spike_login_user";
    private static final String CERT_PUBLICO = "/var/opt/mssql/lp_spike_cert.cer";
    private static final String CERT_COMPLETO = "/var/opt/mssql/lp_spike_cert_full.cer";
    private static final String CERT_CLAVE = "/var/opt/mssql/lp_spike_cert_full.pvk";
    private static final String CERT_PASSWORD = "Sp1ke!Cert#2026";
    private static final String DMK_PASSWORD = "Sp1ke!Dmk#2026Lp";

    private static final String VARIANTE_CERT_PUBLICO = "CERT_DB_ESPEJO_PUBLICO";
    private static final String VARIANTE_CERT_PRIVADO = "CERT_DB_ESPEJO_CON_CLAVE_PRIVADA";
    private static final String VARIANTE_SERVER_LOGIN = "CERT_SERVER_LOGIN_EN_MASTER";

    private static final String COMMAND = "dbo.APP24_C_SPIKE_CONFIRMAR";

    private static final String CUERPO_COMMAND = """
            CREATE PROCEDURE dbo.APP24_C_SPIKE_CONFIRMAR
            AS
            BEGIN
                SET NOCOUNT ON;

                DECLARE @filas INT, @errores INT;

                SELECT @filas = COUNT(*)
                  FROM LP_TARGET.app24.CargaPedimentoFila
                 WHERE CargaPedimentoKey = 1;

                SELECT @errores = COUNT(*)
                  FROM LP_TARGET.app24.ErrorCargaPedimento
                 WHERE CargaPedimentoKey = 1;

                IF @errores > 0 THROW 51000, N'La carga tiene errores.', 1;

                IF @filas = 0 THROW 51001, N'La carga no tiene partidas.', 1;

                UPDATE LP_TARGET.app24.CargaPedimento
                   SET Estado = N'CONFIRMADA'
                 WHERE CargaPedimentoKey = 1
                   AND Estado = N'PREVISUALIZADA';

                IF @@ROWCOUNT = 0 THROW 51002, N'La carga no es confirmable en su estado actual.', 1;

                EXEC LP_TARGET.app24.APP24_C_BITACORA_REGISTRAR
                     @Evento = N'PEDIMENTO_CONFIRMAR',
                     @Referencia = N'LP-SPIKE-1';

                SELECT N'OK' AS Resultado, @filas AS FilasProcesadas;
            END
            """;

    static final MSSQLServerContainer<?> SQL =
            new MSSQLServerContainer<>("mcr.microsoft.com/mssql/server:2022-latest").acceptLicense();

    private static boolean disponible;
    private static String mecanismoGanador;
    private static String usuarioContextoGanador;

    @BeforeAll
    static void iniciarEntorno() throws Exception {
        disponible = dockerDisponible();
        if (!disponible && "true".equalsIgnoreCase(System.getenv("CI"))) {
            throw new IllegalStateException("Docker no disponible en CI: el spike cross-db es obligatorio "
                    + "(CI_CROSS_DB_SPIKE_REQUIRED).");
        }
        Assumptions.assumeTrue(disponible, "Docker no disponible localmente; se omite el spike cross-db.");
        SQL.start();
        crearBases();
        aplicarFixturesBase();
    }

    @AfterAll
    static void detenerEntorno() {
        if (disponible) SQL.stop();
    }

    // ------------------------------------------------------------------ casos

    @Test
    @Order(1)
    void contextoRuntimeEsLeastPrivilege() throws Exception {
        try (Connection fuente = conectarRuntime(FUENTE);
             Statement s = fuente.createStatement();
             ResultSet rs = s.executeQuery("SELECT SUSER_SNAME(), USER_NAME(), IS_SRVROLEMEMBER('sysadmin')")) {
            assertTrue(rs.next(), "Debe responder el contexto de sesión");
            assertEquals(RUNTIME, rs.getString(1), "SUSER_SNAME debe ser el login runtime");
            assertEquals(RUNTIME, rs.getString(2), "USER_NAME debe ser el user runtime en " + FUENTE);
            assertEquals(0, rs.getInt(3), "El runtime no debe ser sysadmin");
        }
        try (Connection destino = conectarRuntime(DESTINO);
             Statement s = destino.createStatement();
             ResultSet rs = s.executeQuery("SELECT SUSER_SNAME(), USER_NAME()")) {
            assertTrue(rs.next());
            assertEquals(RUNTIME, rs.getString(1));
            assertEquals(RUNTIME, rs.getString(2), "USER_NAME debe ser el user runtime en " + DESTINO);
        }
        for (String base : new String[]{FUENTE, DESTINO}) {
            assertEquals(0, valorAdmin("master",
                    "SELECT is_trustworthy_on FROM sys.databases WHERE name = '" + base + "'"),
                    base + " debe permanecer TRUSTWORTHY OFF");
            assertEquals(0, valorAdmin("master",
                    "SELECT is_db_chaining_on FROM sys.databases WHERE name = '" + base + "'"),
                    base + " debe permanecer DB_CHAINING OFF");
        }
        assertEquals(1, valorAdmin("master",
                "SELECT COUNT(*) FROM sys.server_permissions WHERE grantee_principal_id = SUSER_ID('" + RUNTIME + "')"
                        + " AND permission_name = 'CONNECT SQL' AND state_desc = 'GRANT'"),
                "El runtime debe tener únicamente CONNECT SQL");
        assertEquals(0, valorAdmin("master",
                "SELECT COUNT(*) FROM sys.server_permissions WHERE grantee_principal_id = SUSER_ID('" + RUNTIME + "')"
                        + " AND permission_name <> 'CONNECT SQL'"),
                "El runtime no debe tener otros permisos de servidor");
    }

    @Test
    @Order(2)
    void baselineSinFirmaDenegado() throws Exception {
        assertEquals(0, contarFirma(FUENTE), "El command debe partir sin firma");
        SQLException error = assertDenegado("EXEC cross-db sin firma (baseline)",
                () -> ejecutarComoRuntime(FUENTE, "EXEC " + COMMAND));
        System.out.println("[LP-SPIKE] baseline sin firma → DENIED: " + error.getMessage());
        assertEquals("PREVISUALIZADA",
                textoAdmin(DESTINO, "SELECT Estado FROM app24.CargaPedimento WHERE CargaPedimentoKey = 1"),
                "Sin firma no debe haber escritura parcial");
        assertEquals(0, valorAdmin(DESTINO, "SELECT COUNT(*) FROM app24.BitacoraEvento"),
                "Sin firma no debe registrarse bitácora");
    }

    @Test
    @Order(3)
    void spikeSeleccionaMecanismoCrossDb() throws Exception {
        firmarFuente();
        assertEquals(1, contarFirma(FUENTE), "La firma debe quedar aplicada al command");

        Exception errorPublico = intentarVariante(VARIANTE_CERT_PUBLICO, () -> {
            limpiarDestino();
            limpiarMaster();
            try (Connection destino = conectarAdmin(DESTINO)) {
                ejecutar(destino, "CREATE CERTIFICATE " + CERT + " FROM FILE = N'" + CERT_PUBLICO + "'");
                ejecutar(destino, "CREATE USER " + USUARIO_CERT + " FROM CERTIFICATE " + CERT);
                aplicarPermisosMinimos(destino, USUARIO_CERT);
            }
        });
        if (errorPublico == null) {
            mecanismoGanador = VARIANTE_CERT_PUBLICO;
            usuarioContextoGanador = USUARIO_CERT;
        } else {
            Exception errorPrivado = intentarVariante(VARIANTE_CERT_PRIVADO, () -> {
                limpiarDestino();
                limpiarMaster();
                try (Connection fuente = conectarAdmin(FUENTE)) {
                    ejecutar(fuente, "BACKUP CERTIFICATE " + CERT + " TO FILE = N'" + CERT_COMPLETO + "'"
                            + " WITH PRIVATE KEY (FILE = N'" + CERT_CLAVE + "',"
                            + " ENCRYPTION BY PASSWORD = N'" + CERT_PASSWORD + "')");
                }
                try (Connection destino = conectarAdmin(DESTINO)) {
                    crearMasterKeySiFalta(destino);
                    ejecutar(destino, "CREATE CERTIFICATE " + CERT + " FROM FILE = N'" + CERT_COMPLETO + "'"
                            + " WITH PRIVATE KEY (FILE = N'" + CERT_CLAVE + "',"
                            + " DECRYPTION BY PASSWORD = N'" + CERT_PASSWORD + "')");
                    ejecutar(destino, "CREATE USER " + USUARIO_CERT + " FROM CERTIFICATE " + CERT);
                    aplicarPermisosMinimos(destino, USUARIO_CERT);
                }
            });
            if (errorPrivado == null) {
                mecanismoGanador = VARIANTE_CERT_PRIVADO;
                usuarioContextoGanador = USUARIO_CERT;
            } else {
                Exception errorLogin = intentarVariante(VARIANTE_SERVER_LOGIN, () -> {
                    limpiarDestino();
                    limpiarMaster();
                    try (Connection master = conectarAdmin("master")) {
                        crearMasterKeySiFalta(master);
                        ejecutar(master, "CREATE CERTIFICATE " + CERT_SERVIDOR + " FROM FILE = N'" + CERT_PUBLICO + "'");
                        ejecutar(master, "CREATE LOGIN " + LOGIN_CERT + " FROM CERTIFICATE " + CERT_SERVIDOR);
                    }
                    try (Connection destino = conectarAdmin(DESTINO)) {
                        ejecutar(destino, "CREATE USER " + USUARIO_LOGIN + " FOR LOGIN " + LOGIN_CERT);
                        aplicarPermisosMinimos(destino, USUARIO_LOGIN);
                    }
                });
                if (errorLogin == null) {
                    mecanismoGanador = VARIANTE_SERVER_LOGIN;
                    usuarioContextoGanador = USUARIO_LOGIN;
                } else {
                    assertNotNull(null, "Ninguna variante cross-db funcionó. "
                            + "publico=[" + errorPublico.getMessage() + "] "
                            + "privado=[" + errorPrivado.getMessage() + "] "
                            + "login=[" + errorLogin.getMessage() + "]");
                }
            }
        }

        assertNotNull(mecanismoGanador, "Debe existir un mecanismo cross-db verificable");
        System.out.println("[LP-SPIKE] MECANISMO GANADOR = " + mecanismoGanador
                + " (principal de contexto = " + usuarioContextoGanador + ")");
        assertEquals("CONFIRMADA",
                textoAdmin(DESTINO, "SELECT Estado FROM app24.CargaPedimento WHERE CargaPedimentoKey = 1"),
                "El flujo firmado debe confirmar la carga sintética");
        assertEquals(1, valorAdmin(DESTINO, "SELECT COUNT(*) FROM app24.BitacoraEvento"),
                "El flujo firmado debe registrar la bitácora vía ownership chain intra-DB");
        resetTarget();
    }

    @Test
    @Order(4)
    void principalDeContextoTieneSoloPermisosMinimos() throws Exception {
        assertNotNull(usuarioContextoGanador, "El mecanismo ganador debe estar definido");
        // consultarPermisos devuelve las filas ordenadas por su texto (state|permission|objeto).
        String obtenidos = consultarPermisos(DESTINO, usuarioContextoGanador);
        String esperados = "GRANT|EXECUTE|app24.APP24_C_BITACORA_REGISTRAR; "
                + "GRANT|SELECT|app24.CargaPedimento; "
                + "GRANT|SELECT|app24.CargaPedimentoFila; "
                + "GRANT|SELECT|app24.ErrorCargaPedimento; "
                + "GRANT|UPDATE|app24.CargaPedimento";
        assertEquals(esperados, obtenidos,
                "El principal de contexto debe tener exactamente 5 GRANT por objeto, sin INSERT/DELETE/DDL");
        String detalleRuntime = permisosDeBase(DESTINO, RUNTIME);
        assertEquals("DATABASE|GRANT|CONNECT", detalleRuntime,
                "El runtime solo debe tener CONNECT por base en " + DESTINO + "; sin permisos de objeto");
        if (VARIANTE_SERVER_LOGIN.equals(mecanismoGanador)) {
            assertEquals(0, valorAdmin("master",
                    "SELECT COUNT(*) FROM sys.server_permissions WHERE grantee_principal_id = SUSER_ID('" + LOGIN_CERT + "')"
                            + " AND permission_name <> 'CONNECT SQL'"),
                    "El login derivado del certificado no debe tener más permiso de servidor que CONNECT SQL");
        }
        // BitacoraEvento intencionalmente ausente: su INSERT lo resuelve la cadena de propiedad intra-DB.
        assertEquals(0, valorAdmin(DESTINO,
                "SELECT COUNT(*) FROM sys.database_permissions p JOIN sys.objects o ON o.object_id = p.major_id "
                        + "WHERE p.grantee_principal_id = USER_ID('" + usuarioContextoGanador + "') "
                        + "AND o.name = 'BitacoraEvento'"),
                "El principal de contexto no debe tener DML directo sobre BitacoraEvento");
    }

    @Test
    @Order(5)
    void runtimeDirectoPermaneceDenegado() throws Exception {
        assertDenegado("SELECT directo a CargaPedimento",
                () -> ejecutarComoRuntime(FUENTE, "SELECT TOP 1 Estado FROM LP_TARGET.app24.CargaPedimento"));
        assertDenegado("INSERT directo a CargaPedimento",
                () -> ejecutarComoRuntime(FUENTE, "INSERT INTO LP_TARGET.app24.CargaPedimento (CargaPedimentoKey, Estado) VALUES (999, 'X')"));
        assertDenegado("UPDATE directo a CargaPedimento",
                () -> ejecutarComoRuntime(FUENTE, "UPDATE LP_TARGET.app24.CargaPedimento SET Estado = 'X' WHERE CargaPedimentoKey = 1"));
        assertDenegado("DELETE directo a CargaPedimento",
                () -> ejecutarComoRuntime(FUENTE, "DELETE FROM LP_TARGET.app24.CargaPedimento WHERE CargaPedimentoKey = 1"));
        assertDenegado("SELECT directo a CargaPedimentoFila",
                () -> ejecutarComoRuntime(FUENTE, "SELECT TOP 1 FilaKey FROM LP_TARGET.app24.CargaPedimentoFila"));
        assertDenegado("SELECT directo a ErrorCargaPedimento",
                () -> ejecutarComoRuntime(FUENTE, "SELECT TOP 1 ErrorKey FROM LP_TARGET.app24.ErrorCargaPedimento"));
        assertDenegado("SELECT directo a BitacoraEvento",
                () -> ejecutarComoRuntime(FUENTE, "SELECT TOP 1 EventoKey FROM LP_TARGET.app24.BitacoraEvento"));
        assertDenegado("EXEC directo a bitácora sin firma de por medio",
                () -> ejecutarComoRuntime(FUENTE, "EXEC LP_TARGET.app24.APP24_C_BITACORA_REGISTRAR @Evento = 'X', @Referencia = 'Y'"));
        assertDenegado("SELECT directo desde conexión a " + DESTINO,
                () -> ejecutarComoRuntime(DESTINO, "SELECT TOP 1 Estado FROM app24.CargaPedimento"));
        assertEquals("PREVISUALIZADA",
                textoAdmin(DESTINO, "SELECT Estado FROM app24.CargaPedimento WHERE CargaPedimentoKey = 1"),
                "Las pruebas negativas no deben escribir");
        assertEquals(0, valorAdmin(DESTINO, "SELECT COUNT(*) FROM app24.BitacoraEvento"),
                "Las pruebas negativas no deben registrar bitácora");
    }

    @Test
    @Order(6)
    void ddlPermanenceDenegado() throws Exception {
        assertDenegado("CREATE TABLE", () -> ejecutarComoRuntime(FUENTE, "CREATE TABLE dbo.lp_probe_runtime (id INT)"));
        assertDenegado("ALTER TABLE", () -> ejecutarComoRuntime(FUENTE, "ALTER TABLE dbo.lp_ddl_probe ADD marca INT NULL"));
        assertDenegado("DROP TABLE", () -> ejecutarComoRuntime(FUENTE, "DROP TABLE dbo.lp_ddl_probe"));
        assertDenegado("CREATE PROCEDURE", () -> ejecutarComoRuntime(FUENTE, "CREATE PROCEDURE dbo.lp_proc_runtime AS SELECT 1 AS x"));
    }

    @Test
    @Order(7)
    void spNoAutorizadoDenegado() throws Exception {
        assertDenegado("EXEC SP sin GRANT",
                () -> ejecutarComoRuntime(FUENTE, "EXEC dbo.LEGACY_DANGEROUS_SP"));
    }

    @Test
    @Order(8)
    void firmaSeInvalidaConAlterYSeRestauraConRefirma() throws Exception {
        resetTarget();
        assertEquals(1, contarFirma(FUENTE), "La firma debe existir antes del ciclo");

        try (Connection fuente = conectarAdmin(FUENTE)) {
            ejecutar(fuente, CUERPO_COMMAND.replace("CREATE PROCEDURE", "ALTER PROCEDURE"));
        }
        assertEquals(0, contarFirma(FUENTE), "ALTER PROCEDURE debe eliminar la firma");
        SQLException trasAlter = assertDenegado("EXEC firmado tras ALTER",
                () -> ejecutarComoRuntime(FUENTE, "EXEC " + COMMAND));
        System.out.println("[LP-SPIKE] tras ALTER sin refirmar → DENIED: " + trasAlter.getMessage());
        assertEquals("PREVISUALIZADA",
                textoAdmin(DESTINO, "SELECT Estado FROM app24.CargaPedimento WHERE CargaPedimentoKey = 1"),
                "Sin firma vigente no debe haber escritura parcial");

        try (Connection fuente = conectarAdmin(FUENTE)) {
            ejecutar(fuente, "ADD SIGNATURE TO " + COMMAND + " BY CERTIFICATE " + CERT);
        }
        assertEquals(1, contarFirma(FUENTE), "La refirma debe restaurar la firma");
        ejecutarRuntimeCommandYValidar();
        assertEquals("CONFIRMADA",
                textoAdmin(DESTINO, "SELECT Estado FROM app24.CargaPedimento WHERE CargaPedimentoKey = 1"),
                "Tras refirmar, el flujo cross-db debe volver a funcionar");
        resetTarget();
    }

    // ------------------------------------------------------------- fixtures

    private static void crearBases() throws Exception {
        try (Connection master = conectarAdmin("master"); Statement s = master.createStatement()) {
            s.execute("IF DB_ID('" + FUENTE + "') IS NULL CREATE DATABASE [" + FUENTE + "]");
            s.execute("IF DB_ID('" + DESTINO + "') IS NULL CREATE DATABASE [" + DESTINO + "]");
        }
    }

    private static void aplicarFixturesBase() throws Exception {
        try (Connection master = conectarAdmin("master")) {
            ejecutar(master, "CREATE LOGIN " + RUNTIME + " WITH PASSWORD = N'" + RUNTIME_PASSWORD + "',"
                    + " CHECK_POLICY = OFF, DEFAULT_DATABASE = [" + FUENTE + "]");
        }
        try (Connection fuente = conectarAdmin(FUENTE)) {
            ejecutar(fuente, CUERPO_COMMAND);
            ejecutar(fuente, "CREATE TABLE dbo.lp_ddl_probe (id INT)");
            ejecutar(fuente, "CREATE PROCEDURE dbo.LEGACY_DANGEROUS_SP AS BEGIN SET NOCOUNT ON; SELECT 1 AS operacion; END");
            ejecutar(fuente, "CREATE USER " + RUNTIME + " FOR LOGIN " + RUNTIME);
            ejecutar(fuente, "GRANT EXECUTE ON OBJECT::" + COMMAND + " TO " + RUNTIME);
        }
        try (Connection destino = conectarAdmin(DESTINO)) {
            ejecutar(destino, "CREATE SCHEMA app24 AUTHORIZATION dbo");
            ejecutar(destino, "CREATE TABLE app24.CargaPedimento (CargaPedimentoKey INT NOT NULL PRIMARY KEY, Estado VARCHAR(20) NOT NULL)");
            ejecutar(destino, "CREATE TABLE app24.CargaPedimentoFila (FilaKey INT NOT NULL PRIMARY KEY, CargaPedimentoKey INT NOT NULL, MaterialKey INT NULL)");
            ejecutar(destino, "CREATE TABLE app24.ErrorCargaPedimento (ErrorKey INT NOT NULL PRIMARY KEY, CargaPedimentoKey INT NOT NULL, Mensaje VARCHAR(200) NULL)");
            ejecutar(destino, "CREATE TABLE app24.BitacoraEvento (EventoKey INT IDENTITY(1,1) PRIMARY KEY, Evento VARCHAR(100) NOT NULL, Referencia VARCHAR(100) NULL, Fecha DATETIME2 NOT NULL DEFAULT SYSUTCDATETIME())");
            ejecutar(destino, "CREATE PROCEDURE app24.APP24_C_BITACORA_REGISTRAR"
                    + " @Evento VARCHAR(100), @Referencia VARCHAR(100)"
                    + " AS BEGIN SET NOCOUNT ON;"
                    + " INSERT app24.BitacoraEvento (Evento, Referencia) VALUES (@Evento, @Referencia); END");
            ejecutar(destino, "CREATE USER " + RUNTIME + " FOR LOGIN " + RUNTIME);
            resetTarget();
        }
    }

    private static void firmarFuente() throws Exception {
        try (Connection fuente = conectarAdmin(FUENTE)) {
            crearMasterKeySiFalta(fuente);
            ejecutar(fuente, "CREATE CERTIFICATE " + CERT + " WITH SUBJECT = N'LP spike cross-db signing'");
            ejecutar(fuente, "BACKUP CERTIFICATE " + CERT + " TO FILE = N'" + CERT_PUBLICO + "'");
            ejecutar(fuente, "ADD SIGNATURE TO " + COMMAND + " BY CERTIFICATE " + CERT);
        }
    }

    private static void aplicarPermisosMinimos(Connection destino, String principal) throws Exception {
        ejecutar(destino, "GRANT SELECT, UPDATE ON OBJECT::app24.CargaPedimento TO [" + principal + "]");
        ejecutar(destino, "GRANT SELECT ON OBJECT::app24.CargaPedimentoFila TO [" + principal + "]");
        ejecutar(destino, "GRANT SELECT ON OBJECT::app24.ErrorCargaPedimento TO [" + principal + "]");
        ejecutar(destino, "GRANT EXECUTE ON OBJECT::app24.APP24_C_BITACORA_REGISTRAR TO [" + principal + "]");
    }

    private static void limpiarDestino() throws Exception {
        try (Connection destino = conectarAdmin(DESTINO)) {
            ejecutar(destino, "IF USER_ID(N'" + USUARIO_CERT + "') IS NOT NULL DROP USER [" + USUARIO_CERT + "]");
            ejecutar(destino, "IF USER_ID(N'" + USUARIO_LOGIN + "') IS NOT NULL DROP USER [" + USUARIO_LOGIN + "]");
            ejecutar(destino, "IF CERT_ID(N'" + CERT + "') IS NOT NULL DROP CERTIFICATE [" + CERT + "]");
        }
    }

    private static void limpiarMaster() throws Exception {
        try (Connection master = conectarAdmin("master")) {
            ejecutar(master, "IF SUSER_ID(N'" + LOGIN_CERT + "') IS NOT NULL DROP LOGIN [" + LOGIN_CERT + "]");
            ejecutar(master, "IF CERT_ID(N'" + CERT_SERVIDOR + "') IS NOT NULL DROP CERTIFICATE [" + CERT_SERVIDOR + "]");
        }
    }

    private static void resetTarget() throws Exception {
        try (Connection destino = conectarAdmin(DESTINO)) {
            ejecutar(destino, "DELETE FROM app24.BitacoraEvento");
            ejecutar(destino, "DELETE FROM app24.CargaPedimentoFila");
            ejecutar(destino, "DELETE FROM app24.CargaPedimento");
            ejecutar(destino, "INSERT INTO app24.CargaPedimento (CargaPedimentoKey, Estado) VALUES (1, 'PREVISUALIZADA')");
            ejecutar(destino, "INSERT INTO app24.CargaPedimentoFila (FilaKey, CargaPedimentoKey, MaterialKey) VALUES (1, 1, 10), (2, 1, 20)");
        }
    }

    // ------------------------------------------------------------- variantes

    /**
     * Intenta una variante: reinicia datos, configura el mecanismo, ejecuta el
     * command como runtime y valida el efecto completo. Devuelve {@code null}
     * si la variante funciona, o el error capturado si falla.
     */
    private static Exception intentarVariante(String nombre, AccionSql configurar) {
        try {
            resetTarget();
            configurar.ejecutar();
            ejecutarRuntimeCommandYValidar();
            return null;
        } catch (Exception error) {
            System.out.println("[LP-SPIKE] variante " + nombre + " → ERROR: " + error.getMessage());
            return error;
        }
    }

    /** Ejecuta el command como runtime y valida su fila de resultado. */
    private static void ejecutarRuntimeCommandYValidar() throws SQLException {
        try (Connection c = conectarRuntime(FUENTE); Statement s = c.createStatement()) {
            boolean hayResultado = s.execute("EXEC " + COMMAND);
            while (true) {
                if (hayResultado) {
                    try (ResultSet rs = s.getResultSet()) {
                        assertTrue(rs.next(), "El command debe devolver su fila de resultado");
                        assertEquals("OK", rs.getString("Resultado"), "El command debe informar Resultado=OK");
                        return;
                    }
                }
                hayResultado = s.getMoreResults();
                if (!hayResultado && s.getUpdateCount() == -1) return;
            }
        }
    }

    // ------------------------------------------------------------- helpers SQL

    private static boolean dockerDisponible() {
        try {
            DockerClientFactory.instance().client();
            return true;
        } catch (Throwable noDisponible) {
            return false;
        }
    }

    private static String url(String db) {
        return "jdbc:sqlserver://" + SQL.getHost() + ":" + SQL.getMappedPort(1433)
                + ";databaseName=" + db + ";encrypt=false;trustServerCertificate=true";
    }

    private static Connection conectarAdmin(String db) throws SQLException {
        return DriverManager.getConnection(url(db), SQL.getUsername(), SQL.getPassword());
    }

    private static Connection conectarRuntime(String db) throws SQLException {
        return DriverManager.getConnection(url(db), RUNTIME, RUNTIME_PASSWORD);
    }

    private static void ejecutar(Connection c, String sql) throws SQLException {
        try (Statement s = c.createStatement()) {
            s.execute(sql);
            while (s.getMoreResults() || s.getUpdateCount() != -1) { /* drena */ }
        }
    }

    private static void ejecutarComoRuntime(String db, String sql) throws SQLException {
        try (Connection c = conectarRuntime(db)) {
            ejecutar(c, sql);
        }
    }

    private static void crearMasterKeySiFalta(Connection c) throws SQLException {
        ejecutar(c, "IF NOT EXISTS (SELECT 1 FROM sys.symmetric_keys WHERE name = '##MS_DatabaseMasterKey##')"
                + " CREATE MASTER KEY ENCRYPTION BY PASSWORD = N'" + DMK_PASSWORD + "'");
    }

    private static int contarFirma(String db) {
        return valorAdmin(db, "SELECT COUNT(*) FROM sys.crypt_properties WHERE class_desc = 'OBJECT_OR_COLUMN' "
                + "AND major_id = OBJECT_ID('" + COMMAND + "')");
    }

    private static String consultarPermisos(String db, String principal) {
        String sql = "SELECT p.state_desc + '|' + p.permission_name + '|' + OBJECT_SCHEMA_NAME(p.major_id) + '.' + OBJECT_NAME(p.major_id) "
                + "FROM sys.database_permissions p WHERE p.grantee_principal_id = USER_ID(N'" + principal + "') "
                + "AND p.class_desc = 'OBJECT_OR_COLUMN' ORDER BY 1";
        StringBuilder resultado = new StringBuilder();
        try (Connection c = conectarAdmin(db); Statement s = c.createStatement(); ResultSet rs = s.executeQuery(sql)) {
            while (rs.next()) {
                if (resultado.length() > 0) resultado.append("; ");
                resultado.append(rs.getString(1));
            }
        } catch (SQLException error) {
            throw new IllegalStateException("No se pudieron consultar permisos de " + principal + ": " + error.getMessage(), error);
        }
        return resultado.toString();
    }

    /** Lista compacta de TODOS los permisos de base (cualquier clase) de un principal. */
    private static String permisosDeBase(String db, String principal) {
        String sql = "SELECT p.class_desc + '|' + p.state_desc + '|' + p.permission_name "
                + "FROM sys.database_permissions p WHERE p.grantee_principal_id = USER_ID(N'" + principal + "') ORDER BY 1";
        StringBuilder resultado = new StringBuilder();
        try (Connection c = conectarAdmin(db); Statement s = c.createStatement(); ResultSet rs = s.executeQuery(sql)) {
            while (rs.next()) {
                if (resultado.length() > 0) resultado.append("; ");
                resultado.append(rs.getString(1));
            }
        } catch (SQLException error) {
            throw new IllegalStateException("No se pudieron consultar permisos de " + principal + ": " + error.getMessage(), error);
        }
        return resultado.toString();
    }

    private static int valorAdmin(String db, String sql) {
        try (Connection c = conectarAdmin(db); Statement s = c.createStatement(); ResultSet rs = s.executeQuery(sql)) {
            return rs.next() ? rs.getInt(1) : -1;
        } catch (SQLException error) {
            throw new IllegalStateException("Fallo consulta [" + sql + "]: " + error.getMessage(), error);
        }
    }

    private static String textoAdmin(String db, String sql) {
        try (Connection c = conectarAdmin(db); Statement s = c.createStatement(); ResultSet rs = s.executeQuery(sql)) {
            return rs.next() ? rs.getString(1) : null;
        } catch (SQLException error) {
            throw new IllegalStateException("Fallo consulta [" + sql + "]: " + error.getMessage(), error);
        }
    }

    /**
     * Ejecuta una acción que debe fallar por permisos y valida que el error sea
     * de denegación (no un fallo funcional encubierto).
     */
    private static SQLException assertDenegado(String contexto, AccionSql accion) {
        SQLException error = assertThrows(SQLException.class, accion::ejecutar, contexto);
        String mensaje = error.getMessage() == null ? "" : error.getMessage().toLowerCase(Locale.ROOT);
        boolean denegado = mensaje.contains("denied")
                || mensaje.contains("permission")
                || mensaje.contains("do not have permissions")
                || mensaje.contains("denegado")
                || mensaje.contains("permiso");
        assertTrue(denegado, contexto + " → se esperaba DENIED, se obtuvo: [" + error.getErrorCode() + "] " + error.getMessage());
        return error;
    }

    @FunctionalInterface
    private interface AccionSql {
        void ejecutar() throws Exception;
    }
}
