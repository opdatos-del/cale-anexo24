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

import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Prueba de integración de los scripts de deployment REALES del runtime
 * least privilege ({@code infra/sql/04..07}) sobre un SQL Server efímero.
 *
 * <p>Las bases se llaman {@code CALE_IMMEX} y {@code ANEXO24_DEV} para que los
 * scripts se apliquen tal cual están versionados (sin copias), pero son bases
 * efímeras de Testcontainers: no son arquitectura nueva ni se acercan a LIVE.
 * Sólo contienen objetos sintéticos mínimos para que los GRANT funcionen.</p>
 *
 * <p>Cubre: aplicación e idempotencia de 04/05, guard SQLCMD de 06, firma +
 * certificado espejo + set exacto de permisos, drift de thumbprint, set de
 * cinco permisos incorrecto, batería positiva/negativa del runtime y el
 * verificador read-only 07 con sus negativos.</p>
 */
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class RuntimeIdentityDeploymentScriptsIT {

    private static final String CALE = "CALE_IMMEX";
    private static final String APP = "ANEXO24_DEV";

    private static final String RUNTIME = "anexo24_app";
    private static final String RUNTIME_PASSWORD = "Sp1ke!Deploy#2026";
    private static final String DMK_PASSWORD = "Sp1ke!Dmk#2026Deploy";
    private static final String CERT = "app24_pedimento_cert";
    private static final String CERT_USER = "app24_pedimento_cert_user";
    private static final String CERT_PATH_1 = "/var/opt/mssql/app24_deploy_cert_1.cer";
    private static final String CERT_PATH_2 = "/var/opt/mssql/app24_deploy_cert_2.cer";
    private static final String CERT_PATH_3 = "/var/opt/mssql/app24_deploy_cert_3.cer";
    private static final String CERT_PATH_4 = "/var/opt/mssql/app24_deploy_cert_4.cer";

    private static final String[] CALE_SP = {
            "APP24_Q_ACTIVOS_FIJOS_LISTAR", "APP24_Q_AGENTES_ADUANALES_LISTAR",
            "APP24_Q_ALMACENES_LISTAR", "APP24_Q_ANALISIS_DESCARGAS_LISTAR",
            "APP24_Q_CATEGORIAS_LISTAR", "APP24_Q_CLIENTES_LISTAR",
            "APP24_Q_COMPULSA_LISTAR", "APP24_Q_DATOS_GENERALES_OBTENER",
            "APP24_Q_DIRIGIDOS_LISTAR", "APP24_Q_ENTRADAS_LISTAR",
            "APP24_Q_ESTRUCTURAS_LISTAR", "APP24_Q_F4_LISTAR",
            "APP24_Q_MATERIALES_LISTAR",
            "APP24_Q_MATERIALES_UTILIZADOS_LISTAR", "APP24_Q_OPERACIONES_BLOQUEADAS_LISTAR",
            "APP24_Q_PEDIMENTO_VALIDAR_REGLAS", "APP24_Q_PRODUCTOS_LISTAR",
            "APP24_Q_PROVEEDORES_LISTAR", "APP24_Q_RECTIFICACIONES_LISTAR",
            "APP24_Q_SALIDAS_LISTAR", "APP24_Q_TIPOS_MATERIAL_LISTAR",
            "APP24_Q_UNIDADES_LISTAR", "APP24_Q_VENCIMIENTOS_LISTAR",
            "APP24_C_MATERIAL_CARGA_CONFIRMAR",
            "APP24_C_PRODUCTO_CARGA_CONFIRMAR",
            "APP24_C_CLIENTE_CARGA_CONFIRMAR",
            "APP24_C_PROVEEDOR_CARGA_CONFIRMAR",
    };

    private static final String[] APP_SP = {
            "APP24_Q_USUARIO_POR_CLAVE", "APP24_Q_USUARIO_ACCESO",
            "APP24_Q_USUARIOS_LISTAR", "APP24_Q_USUARIO_OBTENER",
            "APP24_C_USUARIO_CREAR", "APP24_C_USUARIO_ACTUALIZAR_DATOS",
            "APP24_C_USUARIO_CAMBIAR_ESTADO", "APP24_C_USUARIO_CAMBIAR_PERFIL",
            "APP24_C_USUARIO_CAMBIAR_VIGENCIA", "APP24_C_USUARIO_RESTABLECER_PASSWORD",
            "APP24_Q_PERFILES_LISTAR", "APP24_Q_PERFIL_PERMISOS_LISTAR",
            "APP24_C_PERFIL_CREAR", "APP24_C_PERFIL_ACTUALIZAR_NOMBRE",
            "APP24_C_PERFIL_CAMBIAR_ESTADO", "APP24_C_PERFIL_REEMPLAZAR_PERMISOS",
            "APP24_Q_ACTIVIDADES_LISTAR", "APP24_Q_BITACORA_LISTAR",
            "APP24_C_BITACORA_REGISTRAR", "APP24_Q_PEDIMENTO_CARGA_POR_HASH",
            "APP24_Q_PEDIMENTO_CARGA_OBTENER", "APP24_Q_PEDIMENTO_CARGA_ERRORES",
            "APP24_C_PEDIMENTO_CARGA_CREAR", "APP24_Q_FACTURACION_CARGA_POR_HASH",
            "APP24_Q_FACTURACION_CARGA_OBTENER", "APP24_C_FACTURACION_CARGA_CREAR",
            "APP24_Q_FACTURACION_PLANTILLA_ACTIVA", "APP24_Q_CATALOGO_MATERIAL_CARGA_POR_HASH",
            "APP24_Q_CATALOGO_MATERIAL_CARGA_OBTENER", "APP24_Q_CATALOGO_MATERIAL_CARGA_ERRORES",
            "APP24_C_CATALOGO_MATERIAL_CARGA_CREAR", "APP24_Q_CATALOGO_PRODUCTO_CARGA_POR_HASH",
            "APP24_Q_CATALOGO_PRODUCTO_CARGA_OBTENER", "APP24_Q_CATALOGO_PRODUCTO_CARGA_ERRORES",
            "APP24_C_CATALOGO_PRODUCTO_CARGA_CREAR",
            "APP24_Q_CATALOGO_CLIENTE_CARGA_POR_HASH",
            "APP24_Q_CATALOGO_CLIENTE_CARGA_OBTENER",
            "APP24_Q_CATALOGO_CLIENTE_CARGA_ERRORES",
            "APP24_C_CATALOGO_CLIENTE_CARGA_CREAR",
            "APP24_Q_CATALOGO_PROVEEDOR_CARGA_POR_HASH",
            "APP24_Q_CATALOGO_PROVEEDOR_CARGA_OBTENER",
            "APP24_Q_CATALOGO_PROVEEDOR_CARGA_ERRORES",
            "APP24_C_CATALOGO_PROVEEDOR_CARGA_CREAR",
    };

    private static final String CUERPO_COMMAND = """
            CREATE PROCEDURE dbo.APP24_C_PEDIMENTO_CONFIRMAR
            AS
            BEGIN
                SET NOCOUNT ON;

                DECLARE @filas INT, @errores INT;

                SELECT @filas = COUNT(*)
                  FROM ANEXO24_DEV.app24.CargaPedimentoFila
                 WHERE CargaPedimentoKey = 1;

                SELECT @errores = COUNT(*)
                  FROM ANEXO24_DEV.app24.ErrorCargaPedimento
                 WHERE CargaPedimentoKey = 1;

                IF @errores > 0 THROW 51000, N'La carga tiene errores.', 1;
                IF @filas = 0 THROW 51001, N'La carga no tiene partidas.', 1;

                UPDATE ANEXO24_DEV.app24.CargaPedimento
                   SET Estado = N'CONFIRMADA'
                 WHERE CargaPedimentoKey = 1
                   AND Estado = N'PREVISUALIZADA';

                IF @@ROWCOUNT = 0 THROW 51002, N'La carga no es confirmable en su estado actual.', 1;

                EXEC ANEXO24_DEV.app24.APP24_C_BITACORA_REGISTRAR
                     @Evento = N'PEDIMENTO_CONFIRMAR',
                     @Referencia = N'DEPLOY-IT';

                SELECT N'OK' AS Resultado, @filas AS FilasProcesadas;
            END
            """;

    static final MSSQLServerContainer<?> SQL =
            new MSSQLServerContainer<>("mcr.microsoft.com/mssql/server:2022-latest").acceptLicense();

    private static boolean disponible;
    private static Path script04;
    private static Path script05;
    private static Path script06;
    private static Path script07;

    @BeforeAll
    static void iniciarEntorno() throws Exception {
        disponible = dockerDisponible();
        if (!disponible && "true".equalsIgnoreCase(System.getenv("CI"))) {
            throw new IllegalStateException("Docker no disponible en CI: el deployment de scripts es obligatorio "
                    + "(CI_DEPLOYMENT_SCRIPTS_REQUIRED).");
        }
        Assumptions.assumeTrue(disponible, "Docker no disponible localmente; se omiten los scripts de deployment.");
        SQL.start();
        Path raiz = raizRepo();
        script04 = raiz.resolve("04-app-runtime-permissions.sql");
        script05 = raiz.resolve("05-cale-immex-runtime-permissions.sql");
        script06 = raiz.resolve("06-pedimento-cross-db-signing.sql");
        script07 = raiz.resolve("07-runtime-security-verify.sql");
        crearBases();
        aplicarFixtures();
    }

    @AfterAll
    static void detenerEntorno() {
        if (disponible) SQL.stop();
    }

    // ------------------------------------------------------------------ casos

    @Test
    @Order(1)
    void aplicar04y05CreaRolesYPermisosExactosEIdempotente() throws Exception {
        aplicar04();
        aplicar05();

        assertEquals(1, valorAdmin(CALE, "SELECT COUNT(*) FROM sys.database_principals WHERE name = 'cale_immex_runtime' AND type = 'R'"));
        assertEquals(28, valorAdmin(CALE, "SELECT COUNT(*) FROM sys.database_permissions WHERE grantee_principal_id = USER_ID('cale_immex_runtime')"));
        assertEquals(0, valorAdmin(CALE, "SELECT COUNT(*) FROM sys.database_permissions WHERE grantee_principal_id = USER_ID('cale_immex_runtime') AND (permission_name <> 'EXECUTE' OR class_desc <> 'OBJECT_OR_COLUMN' OR state <> 'G')"));
        assertEquals(1, valorAdmin(CALE, "SELECT COUNT(*) FROM sys.database_role_members drm JOIN sys.database_principals r ON r.principal_id = drm.role_principal_id JOIN sys.database_principals m ON m.principal_id = drm.member_principal_id WHERE r.name = 'cale_immex_runtime' AND m.name = '" + RUNTIME + "'"));
        assertEquals(0, valorAdmin(CALE, "SELECT COUNT(*) FROM sys.database_permissions WHERE grantee_principal_id = USER_ID('" + RUNTIME + "') AND NOT (class_desc = 'DATABASE' AND permission_name = 'CONNECT' AND state = 'G')"));
        assertEquals(0, valorAdmin(CALE, "SELECT COUNT(*) FROM sys.database_role_members drm JOIN sys.database_principals r ON r.principal_id = drm.role_principal_id JOIN sys.database_principals m ON m.principal_id = drm.member_principal_id WHERE m.name = '" + RUNTIME + "' AND r.name IN ('db_owner','db_datareader','db_datawriter','db_ddladmin')"));

        assertEquals(1, valorAdmin(APP, "SELECT COUNT(*) FROM sys.database_principals WHERE name = 'app24_runtime' AND type = 'R'"));
        assertEquals(43, valorAdmin(APP, "SELECT COUNT(*) FROM sys.database_permissions WHERE grantee_principal_id = USER_ID('app24_runtime')"));
        assertEquals(0, valorAdmin(APP, "SELECT COUNT(*) FROM sys.database_permissions WHERE grantee_principal_id = USER_ID('app24_runtime') AND (permission_name <> 'EXECUTE' OR class_desc <> 'OBJECT_OR_COLUMN' OR state <> 'G')"));
        assertEquals(1, valorAdmin(APP, "SELECT COUNT(*) FROM sys.database_role_members drm JOIN sys.database_principals r ON r.principal_id = drm.role_principal_id JOIN sys.database_principals m ON m.principal_id = drm.member_principal_id WHERE r.name = 'app24_runtime' AND m.name = '" + RUNTIME + "'"));
        assertEquals(0, valorAdmin(APP, "SELECT COUNT(*) FROM sys.database_permissions WHERE grantee_principal_id = USER_ID('" + RUNTIME + "') AND NOT (class_desc = 'DATABASE' AND permission_name = 'CONNECT' AND state = 'G')"));

        // Idempotencia: segunda aplicación no falla ni altera los conteos.
        aplicar04();
        aplicar05();
        assertEquals(28, valorAdmin(CALE, "SELECT COUNT(*) FROM sys.database_permissions WHERE grantee_principal_id = USER_ID('cale_immex_runtime')"));
        assertEquals(43, valorAdmin(APP, "SELECT COUNT(*) FROM sys.database_permissions WHERE grantee_principal_id = USER_ID('app24_runtime')"));
        System.out.println("[DEPLOY-IT] 04/05 aplicados e idempotentes: CALE=28 APP=43");
    }

    @Test
    @Order(2)
    void variablesSqlcmdNoSustituidasFallanAntesDeDdl() throws Exception {
        SQLException error = assertThrows(SQLException.class, () -> aplicar06(Map.of()));
        assertTrue(error.getMessage().contains("SQLCMD_VARIABLE_GUARD"),
                "El guard debe dispararse antes de cualquier DDL: " + error.getMessage());
        assertEquals(0, valorAdmin(CALE, "SELECT COUNT(*) FROM sys.certificates WHERE name = '" + CERT + "'"));
        assertEquals(0, valorAdmin(APP, "SELECT COUNT(*) FROM sys.certificates WHERE name = '" + CERT + "'"));
        assertEquals(0, valorAdmin(APP, "SELECT COUNT(*) FROM sys.symmetric_keys WHERE name = '##MS_DatabaseMasterKey##'"));
        assertEquals(0, valorAdmin(CALE, "SELECT COUNT(*) FROM sys.crypt_properties WHERE major_id = OBJECT_ID('dbo.APP24_C_PEDIMENTO_CONFIRMAR')"));
        assertEquals(0, valorAdmin(APP, "SELECT COUNT(*) FROM sys.database_principals WHERE name = '" + CERT_USER + "'"));
        System.out.println("[DEPLOY-IT] SQLCMD_VARIABLE_GUARD = PASS (" + error.getMessage() + ")");
    }

    @Test
    @Order(3)
    void aplicar06FirmaEspejaYVerifica() throws Exception {
        aplicar06(Map.of("DmkPassword", DMK_PASSWORD, "CertPublicPath", CERT_PATH_1));

        assertEquals(1, valorAdmin(CALE, "SELECT COUNT(*) FROM sys.certificates WHERE name = '" + CERT + "'"));
        assertEquals(1, valorAdmin(CALE, "SELECT COUNT(*) FROM sys.crypt_properties WHERE class_desc = 'OBJECT_OR_COLUMN' AND major_id = OBJECT_ID('dbo.APP24_C_PEDIMENTO_CONFIRMAR')"));
        assertEquals(1, valorAdmin(APP, "SELECT COUNT(*) FROM sys.certificates WHERE name = '" + CERT + "'"));
        assertEquals(1, valorAdmin(APP, "SELECT COUNT(*) FROM sys.database_principals WHERE name = '" + CERT_USER + "'"));

        String thumbOrigen = textoAdmin("master", "SELECT CONVERT(VARCHAR(64), thumbprint, 2) FROM CALE_IMMEX.sys.certificates WHERE name = '" + CERT + "'");
        String thumbDestino = textoAdmin("master", "SELECT CONVERT(VARCHAR(64), thumbprint, 2) FROM ANEXO24_DEV.sys.certificates WHERE name = '" + CERT + "'");
        assertEquals(thumbOrigen, thumbDestino, "CERTIFICATE_MIRROR_MATCH: los thumbprints deben ser idénticos");

        String permisos = textoAdmin(APP, "SELECT STRING_AGG(p.permission_name COLLATE DATABASE_DEFAULT + '|' + s.name COLLATE DATABASE_DEFAULT + '.' + o.name COLLATE DATABASE_DEFAULT, '; ') "
                + "WITHIN GROUP (ORDER BY p.permission_name COLLATE DATABASE_DEFAULT, s.name COLLATE DATABASE_DEFAULT, o.name COLLATE DATABASE_DEFAULT) "
                + "FROM sys.database_permissions p JOIN sys.objects o ON o.object_id = p.major_id JOIN sys.schemas s ON s.schema_id = o.schema_id "
                + "WHERE p.grantee_principal_id = USER_ID('" + CERT_USER + "')");
        assertEquals("EXECUTE|app24.APP24_C_BITACORA_REGISTRAR; SELECT|app24.CargaPedimento; SELECT|app24.CargaPedimentoFila; "
                + "SELECT|app24.ErrorCargaPedimento; UPDATE|app24.CargaPedimento", permisos,
                "CERT_USER_PERMISSION_SET_EXACT: set exacto de 5 GRANT");

        // El command firmado ejecuta cross-db como runtime.
        resetApp();
        ejecutarRuntimeCommandYValidar();
        assertEquals("CONFIRMADA", textoAdmin(APP, "SELECT Estado FROM app24.CargaPedimento WHERE CargaPedimentoKey = 1"));
        assertEquals(1, valorAdmin(APP, "SELECT COUNT(*) FROM app24.BitacoraEvento"));
        resetApp();
        System.out.println("[DEPLOY-IT] 06 aplicado: firma + espejo + set exacto + command cross-db PASS");
    }

    @Test
    @Order(4)
    void thumbprintDriftFallaSinEfectosDePermisos() throws Exception {
        try (Connection app = conectarAdmin(APP)) {
            ejecutar(app, "IF USER_ID('" + CERT_USER + "') IS NOT NULL DROP USER [" + CERT_USER + "]");
            ejecutar(app, "IF CERT_ID('" + CERT + "') IS NOT NULL DROP CERTIFICATE [" + CERT + "]");
            ejecutar(app, "IF NOT EXISTS (SELECT 1 FROM sys.symmetric_keys WHERE name = '##MS_DatabaseMasterKey##')"
                    + " CREATE MASTER KEY ENCRYPTION BY PASSWORD = N'" + DMK_PASSWORD + "'");
            ejecutar(app, "CREATE CERTIFICATE " + CERT + " WITH SUBJECT = N'drift sintético'");
        }
        // 06 completo con el certificado drift: debe fallar ANTES de crear user/permisos.
        SQLException error = assertThrows(SQLException.class,
                () -> aplicar06(Map.of("DmkPassword", DMK_PASSWORD, "CertPublicPath", CERT_PATH_2)));
        assertTrue(error.getMessage().contains("CERTIFICATE_MIRROR_MATCH"), error.getMessage());
        assertEquals(0, valorAdmin(APP, "SELECT COUNT(*) FROM sys.database_principals WHERE name = '" + CERT_USER + "'"),
                "THUMBPRINT_FAIL_HAS_NO_PERMISSION_SIDE_EFFECTS: no debe crearse el cert-user");
        assertEquals(0, valorAdmin(APP, "SELECT COUNT(*) FROM sys.database_permissions p"
                        + " JOIN sys.database_principals u ON u.principal_id = p.grantee_principal_id"
                        + " WHERE u.name = '" + CERT_USER + "'"),
                "THUMBPRINT_FAIL_HAS_NO_PERMISSION_SIDE_EFFECTS: no debe haber grants");

        // Restauración manual (fail closed: el script no repara solo).
        try (Connection app = conectarAdmin(APP)) {
            ejecutar(app, "IF CERT_ID('" + CERT + "') IS NOT NULL DROP CERTIFICATE [" + CERT + "]");
        }
        aplicar06(Map.of("DmkPassword", DMK_PASSWORD, "CertPublicPath", CERT_PATH_3));
        assertEquals(textoAdmin("master", "SELECT CONVERT(VARCHAR(64), thumbprint, 2) FROM CALE_IMMEX.sys.certificates WHERE name = '" + CERT + "'"),
                textoAdmin("master", "SELECT CONVERT(VARCHAR(64), thumbprint, 2) FROM ANEXO24_DEV.sys.certificates WHERE name = '" + CERT + "'"));
        System.out.println("[DEPLOY-IT] THUMBPRINT_FAIL_HAS_NO_PERMISSION_SIDE_EFFECTS = PASS");
    }

    @Test
    @Order(5)
    void firmaSeRestauraCon06TrasAlter() throws Exception {
        resetApp();
        try (Connection cale = conectarAdmin(CALE)) {
            ejecutar(cale, CUERPO_COMMAND.replace("CREATE PROCEDURE", "ALTER PROCEDURE"));
        }
        assertEquals(0, valorAdmin(CALE, "SELECT COUNT(*) FROM sys.crypt_properties WHERE major_id = OBJECT_ID('dbo.APP24_C_PEDIMENTO_CONFIRMAR')"),
                "ALTER PROCEDURE elimina la firma");
        SQLException trasAlter = assertDenegado("EXEC cross-db sin firma vigente",
                () -> ejecutarComoRuntime(CALE, "EXEC dbo.APP24_C_PEDIMENTO_CONFIRMAR"));
        System.out.println("[DEPLOY-IT] tras ALTER → DENIED: " + trasAlter.getMessage());

        aplicar06(Map.of("DmkPassword", DMK_PASSWORD, "CertPublicPath", CERT_PATH_4));
        assertEquals(1, valorAdmin(CALE, "SELECT COUNT(*) FROM sys.crypt_properties WHERE major_id = OBJECT_ID('dbo.APP24_C_PEDIMENTO_CONFIRMAR')"));
        ejecutarRuntimeCommandYValidar();
        assertEquals("CONFIRMADA", textoAdmin(APP, "SELECT Estado FROM app24.CargaPedimento WHERE CargaPedimentoKey = 1"));
        resetApp();
    }

    @Test
    @Order(6)
    void setDeCincoPermisosIncorrectoFalla() throws Exception {
        try (Connection app = conectarAdmin(APP)) {
            ejecutar(app, "REVOKE SELECT ON OBJECT::app24.ErrorCargaPedimento FROM " + CERT_USER);
            ejecutar(app, "GRANT SELECT ON OBJECT::app24.BitacoraEvento TO " + CERT_USER);
        }
        assertEquals(5, valorAdmin(APP, "SELECT COUNT(*) FROM sys.database_permissions WHERE grantee_principal_id = USER_ID('" + CERT_USER + "') AND class_desc = 'OBJECT_OR_COLUMN'"),
                "El count sigue siendo 5; el set es el que está mal");
        SQLException error = ejecutarBatchesConMarcador(conectarAdmin(APP), script06, "CERT_USER_PERMISSION_SET_EXACT");
        assertNotNull(error, "El set incorrecto debe fallar aunque el count sea 5");
        assertTrue(error.getMessage().contains("CERT_USER_PERMISSION_SET_EXACT"), error.getMessage());

        try (Connection app = conectarAdmin(APP)) {
            ejecutar(app, "GRANT SELECT ON OBJECT::app24.ErrorCargaPedimento TO " + CERT_USER);
            ejecutar(app, "REVOKE SELECT ON OBJECT::app24.BitacoraEvento FROM " + CERT_USER);
        }
        assertNull(ejecutarBatchesConMarcador(conectarAdmin(APP), script06, "CERT_USER_PERMISSION_SET_EXACT"),
                "Tras revertir, la verificación exacta debe volver a pasar");
        System.out.println("[DEPLOY-IT] wrong-five-grants → FAIL (count=5 no era suficiente)");
    }

    @Test
    @Order(7)
    void ejecucionRuntimePositivaYNegativa() throws Exception {
        ejecutarComoRuntime(CALE, "EXEC dbo.APP24_Q_MATERIALES_LISTAR");
        assertDenegado("SELECT directo a tabla ANEXO24_DEV",
                () -> ejecutarComoRuntime(CALE, "SELECT TOP 1 Estado FROM ANEXO24_DEV.app24.CargaPedimento"));
        assertDenegado("INSERT directo",
                () -> ejecutarComoRuntime(CALE, "INSERT INTO ANEXO24_DEV.app24.CargaPedimento (CargaPedimentoKey, Estado) VALUES (99, 'X')"));
        assertDenegado("UPDATE directo",
                () -> ejecutarComoRuntime(CALE, "UPDATE ANEXO24_DEV.app24.CargaPedimento SET Estado = 'X' WHERE CargaPedimentoKey = 1"));
        assertDenegado("DELETE directo",
                () -> ejecutarComoRuntime(CALE, "DELETE FROM ANEXO24_DEV.app24.CargaPedimento WHERE CargaPedimentoKey = 1"));
        assertDenegado("SELECT directo desde conexión ANEXO24_DEV",
                () -> ejecutarComoRuntime(APP, "SELECT TOP 1 Estado FROM app24.CargaPedimento"));
        assertDenegado("CREATE TABLE", () -> ejecutarComoRuntime(CALE, "CREATE TABLE dbo.lp_probe (id INT)"));
        assertDenegado("ALTER TABLE", () -> ejecutarComoRuntime(CALE, "ALTER TABLE dbo.lp_ddl_probe ADD marca INT NULL"));
        assertDenegado("DROP TABLE", () -> ejecutarComoRuntime(CALE, "DROP TABLE dbo.lp_ddl_probe"));
        assertDenegado("SP sin GRANT", () -> ejecutarComoRuntime(CALE, "EXEC dbo.LEGACY_DANGEROUS_SP"));
        assertEquals("PREVISUALIZADA", textoAdmin(APP, "SELECT Estado FROM app24.CargaPedimento WHERE CargaPedimentoKey = 1"));
    }

    @Test
    @Order(8)
    void verificador07DetectaInyecciones() throws Exception {
        aplicar07();

        // (a) grant directo al usuario runtime.
        try (Connection app = conectarAdmin(APP)) {
            ejecutar(app, "GRANT SELECT ON OBJECT::app24.UsuarioApp TO " + RUNTIME);
        }
        assertVerificadorFalla("grant directo al usuario debe fallar 07");
        try (Connection app = conectarAdmin(APP)) {
            ejecutar(app, "REVOKE SELECT ON OBJECT::app24.UsuarioApp FROM " + RUNTIME);
        }

        // (b) membership en rol fijo.
        try (Connection app = conectarAdmin(APP)) {
            ejecutar(app, "ALTER ROLE db_datareader ADD MEMBER " + RUNTIME);
        }
        assertVerificadorFalla("rol fijo debe fallar 07");
        try (Connection app = conectarAdmin(APP)) {
            ejecutar(app, "ALTER ROLE db_datareader DROP MEMBER " + RUNTIME);
        }

        // (c) schema EXECUTE.
        try (Connection app = conectarAdmin(APP)) {
            ejecutar(app, "GRANT EXECUTE ON SCHEMA::app24 TO app24_runtime");
        }
        assertVerificadorFalla("schema EXECUTE debe fallar 07");
        try (Connection app = conectarAdmin(APP)) {
            ejecutar(app, "REVOKE EXECUTE ON SCHEMA::app24 FROM app24_runtime");
        }

        // (d) falta un EXECUTE del contrato.
        try (Connection app = conectarAdmin(APP)) {
            ejecutar(app, "REVOKE EXECUTE ON OBJECT::app24.APP24_Q_USUARIOS_LISTAR FROM app24_runtime");
        }
        assertVerificadorFalla("EXECUTE faltante debe fallar 07");
        try (Connection app = conectarAdmin(APP)) {
            ejecutar(app, "GRANT EXECUTE ON OBJECT::app24.APP24_Q_USUARIOS_LISTAR TO app24_runtime");
        }

        // (e) EXECUTE sobrante fuera del contrato.
        try (Connection app = conectarAdmin(APP)) {
            ejecutar(app, "CREATE PROCEDURE app24.APP24_Q_EXTRA_FUERA_CONTRATO AS BEGIN SET NOCOUNT ON; SELECT 1 AS x; END");
            ejecutar(app, "GRANT EXECUTE ON OBJECT::app24.APP24_Q_EXTRA_FUERA_CONTRATO TO app24_runtime");
        }
        assertVerificadorFalla("EXECUTE sobrante debe fallar 07");
        try (Connection app = conectarAdmin(APP)) {
            ejecutar(app, "REVOKE EXECUTE ON OBJECT::app24.APP24_Q_EXTRA_FUERA_CONTRATO FROM app24_runtime");
            ejecutar(app, "DROP PROCEDURE app24.APP24_Q_EXTRA_FUERA_CONTRATO");
        }

        // (f) grant directo en CALE_IMMEX (sobre tabla: SELECT sobre un SP no es válido).
        try (Connection cale = conectarAdmin(CALE)) {
            ejecutar(cale, "GRANT SELECT ON OBJECT::dbo.lp_ddl_probe TO " + RUNTIME);
        }
        assertVerificadorFalla("grant directo CALE debe fallar 07");
        try (Connection cale = conectarAdmin(CALE)) {
            ejecutar(cale, "REVOKE SELECT ON OBJECT::dbo.lp_ddl_probe FROM " + RUNTIME);
        }

        aplicar07();
        System.out.println("[DEPLOY-IT] 07 PASS + 6 negativos detectados");
    }

    @Test
    @Order(9)
    void verificador07NivelServidor() throws Exception {
        aplicar07();

        try (Connection master = conectarAdmin("master")) {
            ejecutar(master, "ALTER SERVER ROLE sysadmin ADD MEMBER " + RUNTIME);
        }
        assertVerificadorFalla("membership sysadmin debe fallar 07");
        try (Connection master = conectarAdmin("master")) {
            ejecutar(master, "ALTER SERVER ROLE sysadmin DROP MEMBER " + RUNTIME);
        }

        try (Connection master = conectarAdmin("master")) {
            ejecutar(master, "GRANT CONTROL SERVER TO " + RUNTIME);
        }
        assertVerificadorFalla("CONTROL SERVER debe fallar 07");
        try (Connection master = conectarAdmin("master")) {
            ejecutar(master, "REVOKE CONTROL SERVER FROM " + RUNTIME);
        }

        aplicar07();
        System.out.println("[DEPLOY-IT] SERVER_LEVEL_RUNTIME_VERIFY = PASS (2 negativos detectados)");
    }

    // ------------------------------------------------------------- fixtures

    private static void crearBases() throws Exception {
        try (Connection master = conectarAdmin("master"); Statement s = master.createStatement()) {
            s.execute("IF DB_ID('" + CALE + "') IS NULL CREATE DATABASE [" + CALE + "]");
            s.execute("IF DB_ID('" + APP + "') IS NULL CREATE DATABASE [" + APP + "]");
        }
    }

    private static void aplicarFixtures() throws Exception {
        try (Connection master = conectarAdmin("master")) {
            ejecutar(master, "CREATE LOGIN " + RUNTIME + " WITH PASSWORD = N'" + RUNTIME_PASSWORD + "',"
                    + " CHECK_POLICY = OFF, DEFAULT_DATABASE = [" + CALE + "]");
        }
        try (Connection cale = conectarAdmin(CALE)) {
            ejecutar(cale, "CREATE TABLE dbo.lp_ddl_probe (id INT)");
            ejecutar(cale, "CREATE PROCEDURE dbo.LEGACY_DANGEROUS_SP AS BEGIN SET NOCOUNT ON; SELECT 1 AS operacion; END");
            ejecutar(cale, "DECLARE @n SYSNAME;"
                    + " DECLARE cur CURSOR LOCAL FAST_FORWARD FOR SELECT name FROM (VALUES " + listaSql(CALE_SP) + ") v(name);"
                    + " OPEN cur; FETCH NEXT FROM cur INTO @n;"
                    + " WHILE @@FETCH_STATUS = 0 BEGIN"
                    + "   EXEC(N'CREATE PROCEDURE dbo.' + @n + N' AS BEGIN SET NOCOUNT ON; SELECT 1 AS dummy; END');"
                    + "   FETCH NEXT FROM cur INTO @n;"
                    + " END;"
                    + " CLOSE cur; DEALLOCATE cur;");
            ejecutar(cale, CUERPO_COMMAND);
        }
        try (Connection app = conectarAdmin(APP)) {
            ejecutar(app, "CREATE SCHEMA app24 AUTHORIZATION dbo");
            ejecutar(app, "CREATE TABLE app24.CargaPedimento (CargaPedimentoKey INT NOT NULL PRIMARY KEY, Estado VARCHAR(20) NOT NULL)");
            ejecutar(app, "CREATE TABLE app24.CargaPedimentoFila (FilaKey INT NOT NULL PRIMARY KEY, CargaPedimentoKey INT NOT NULL, MaterialKey INT NULL)");
            ejecutar(app, "CREATE TABLE app24.ErrorCargaPedimento (ErrorKey INT NOT NULL PRIMARY KEY, CargaPedimentoKey INT NOT NULL, Mensaje VARCHAR(200) NULL)");
            ejecutar(app, "CREATE TABLE app24.BitacoraEvento (EventoKey INT IDENTITY(1,1) PRIMARY KEY, Evento VARCHAR(100) NOT NULL, Referencia VARCHAR(100) NULL, Fecha DATETIME2 NOT NULL DEFAULT SYSUTCDATETIME())");
            ejecutar(app, "CREATE TABLE app24.UsuarioApp (id INT NOT NULL PRIMARY KEY, clave VARCHAR(50) NULL, nombre VARCHAR(100) NULL, correo VARCHAR(100) NULL, estado VARCHAR(20) NULL, perfil_id INT NULL, vigencia DATE NULL)");
            ejecutar(app, "CREATE TABLE app24.PerfilApp (id INT NOT NULL PRIMARY KEY, nombre VARCHAR(50) NULL)");
            ejecutar(app, "CREATE TABLE app24.PerfilActividad (id INT NOT NULL PRIMARY KEY)");
            ejecutar(app, "CREATE TABLE app24.Actividad (id INT NOT NULL PRIMARY KEY, clave VARCHAR(50) NULL)");
            ejecutar(app, "CREATE PROCEDURE app24.APP24_C_BITACORA_REGISTRAR"
                    + " @Evento VARCHAR(100), @Referencia VARCHAR(100)"
                    + " AS BEGIN SET NOCOUNT ON;"
                    + " INSERT app24.BitacoraEvento (Evento, Referencia) VALUES (@Evento, @Referencia); END");
            ejecutar(app, "DECLARE @n SYSNAME;"
                    + " DECLARE cur CURSOR LOCAL FAST_FORWARD FOR SELECT name FROM (VALUES " + listaSql(APP_SP) + ") v(name)"
                    + " WHERE name <> 'APP24_C_BITACORA_REGISTRAR';"
                    + " OPEN cur; FETCH NEXT FROM cur INTO @n;"
                    + " WHILE @@FETCH_STATUS = 0 BEGIN"
                    + "   EXEC(N'CREATE PROCEDURE app24.' + @n + N' AS BEGIN SET NOCOUNT ON; SELECT 1 AS dummy; END');"
                    + "   FETCH NEXT FROM cur INTO @n;"
                    + " END;"
                    + " CLOSE cur; DEALLOCATE cur;");
            ejecutar(app, "CREATE USER " + RUNTIME + " FOR LOGIN " + RUNTIME);
            resetApp();
        }
    }

    private static void resetApp() throws Exception {
        try (Connection app = conectarAdmin(APP)) {
            ejecutar(app, "DELETE FROM app24.BitacoraEvento");
            ejecutar(app, "DELETE FROM app24.CargaPedimentoFila");
            ejecutar(app, "DELETE FROM app24.CargaPedimento");
            ejecutar(app, "INSERT INTO app24.CargaPedimento (CargaPedimentoKey, Estado) VALUES (1, 'PREVISUALIZADA')");
            ejecutar(app, "INSERT INTO app24.CargaPedimentoFila (FilaKey, CargaPedimentoKey, MaterialKey) VALUES (1, 1, 10), (2, 1, 20)");
        }
    }

    private static String listaSql(String[] nombres) {
        return Arrays.stream(nombres).map(n -> "('" + n + "')").collect(Collectors.joining(", "));
    }

    // ------------------------------------------------------------- scripts

    private static void aplicar04() throws Exception {
        try (Connection app = conectarAdmin(APP)) {
            aplicarArchivo(app, script04, Map.of());
        }
    }

    private static void aplicar05() throws Exception {
        try (Connection cale = conectarAdmin(CALE)) {
            aplicarArchivo(cale, script05, Map.of());
        }
    }

    private static void aplicar06(Map<String, String> variables) throws Exception {
        try (Connection master = conectarAdmin("master")) {
            aplicarArchivo(master, script06, variables);
        }
    }

    private static void aplicar07() throws Exception {
        try (Connection master = conectarAdmin("master")) {
            aplicarArchivo(master, script07, Map.of());
        }
    }

    /** Ejecuta el archivo real sustituyendo variables SQLCMD y omitiendo directivas ':'. */
    static void aplicarArchivo(Connection c, Path archivo, Map<String, String> variables) throws Exception {
        String texto = Files.readString(archivo);
        for (Map.Entry<String, String> variable : variables.entrySet()) {
            texto = texto.replace("$(" + variable.getKey() + ")", variable.getValue());
        }
        List<String> batches = dividirBatches(texto);
        try (Statement s = c.createStatement()) {
            for (int i = 0; i < batches.size(); i++) {
                String batch = batches.get(i);
                try {
                    s.execute(batch);
                    while (s.getMoreResults() || s.getUpdateCount() != -1) { /* drena */ }
                } catch (SQLException error) {
                    throw new SQLException("Fallo batch #" + i + " de " + archivo.getFileName() + ": " + error.getMessage(),
                            error.getSQLState(), error.getErrorCode(), error);
                }
            }
        }
    }

    /** Ejecuta todos los batches del archivo que contienen el marcador; devuelve el primer error o {@code null}. */
    private static SQLException ejecutarBatchesConMarcador(Connection c, Path archivo, String marcador) throws Exception {
        List<String> batches = dividirBatches(Files.readString(archivo));
        boolean encontrado = false;
        try (Statement s = c.createStatement()) {
            for (String batch : batches) {
                if (!batch.contains(marcador)) continue;
                encontrado = true;
                try {
                    s.execute(batch);
                    while (s.getMoreResults() || s.getUpdateCount() != -1) { /* drena */ }
                } catch (SQLException error) {
                    return error;
                }
            }
        }
        if (!encontrado) throw new IllegalStateException("No se encontró el batch con marcador '" + marcador + "' en " + archivo.getFileName());
        return null;
    }

    /** Divide en batches por líneas GO, omitiendo directivas sqlcmd (':...'). */
    private static List<String> dividirBatches(String texto) {
        List<String> batches = new ArrayList<>();
        StringBuilder batch = new StringBuilder();
        for (String linea : texto.split("\r?\n", -1)) {
            String limpia = linea.trim();
            if (limpia.startsWith(":")) continue;
            if (limpia.equalsIgnoreCase("GO")) {
                if (!batch.toString().isBlank()) batches.add(batch.toString());
                batch.setLength(0);
            } else {
                batch.append(linea).append('\n');
            }
        }
        if (!batch.toString().isBlank()) batches.add(batch.toString());
        return batches;
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

    private static Path raizRepo() {
        Path desdeBackend = Path.of("..", "infra", "sql");
        return Files.exists(desdeBackend) ? desdeBackend : Path.of("infra", "sql");
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

    private static void ejecutarRuntimeCommandYValidar() throws SQLException {
        try (Connection c = conectarRuntime(CALE); Statement s = c.createStatement()) {
            boolean hayResultado = s.execute("EXEC dbo.APP24_C_PEDIMENTO_CONFIRMAR");
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

    private static void assertVerificadorFalla(String contexto) {
        SQLException error = assertThrows(SQLException.class, RuntimeIdentityDeploymentScriptsIT::aplicar07, contexto);
        String mensaje = error.getMessage() == null ? "" : error.getMessage();
        assertTrue(mensaje.contains("RUNTIME_SECURITY_VERIFY") || mensaje.contains("SERVER_LEVEL_RUNTIME_VERIFY"),
                contexto + " :: " + mensaje);
    }

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
