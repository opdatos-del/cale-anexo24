-- ============================================
-- ANEXO24 - Esquema complementario de aplicacion
-- BD: ANEXO24_DEV (aislada de CALEX_IMMEX / Modulo C)
-- Tablas: configuración, seguridad y trazabilidad propias (docs/03-diseno/modelo-datos.md)
-- ============================================

IF NOT EXISTS (SELECT 1 FROM sys.schemas WHERE name = 'app24')
    EXEC('CREATE SCHEMA app24');
GO

-- Reinicio seguro para desarrollo: elimina dependencias antes de tablas padre.
IF OBJECT_ID('app24.ErrorCarga', 'U') IS NOT NULL DROP TABLE app24.ErrorCarga;
IF OBJECT_ID('app24.CargaFacturacion', 'U') IS NOT NULL DROP TABLE app24.CargaFacturacion;
IF OBJECT_ID('app24.BitacoraEvento', 'U') IS NOT NULL DROP TABLE app24.BitacoraEvento;
IF OBJECT_ID('app24.UsuarioApp', 'U') IS NOT NULL DROP TABLE app24.UsuarioApp;
IF OBJECT_ID('app24.PerfilActividad', 'U') IS NOT NULL DROP TABLE app24.PerfilActividad;
IF OBJECT_ID('app24.ConfiguracionPlantilla', 'U') IS NOT NULL DROP TABLE app24.ConfiguracionPlantilla;
IF OBJECT_ID('app24.Actividad', 'U') IS NOT NULL DROP TABLE app24.Actividad;
IF OBJECT_ID('app24.PerfilApp', 'U') IS NOT NULL DROP TABLE app24.PerfilApp;
GO

-- ------------------------------------------------------------------
-- Seguridad
-- ------------------------------------------------------------------

IF OBJECT_ID('app24.PerfilApp', 'U') IS NOT NULL
    DROP TABLE app24.PerfilApp;
GO
CREATE TABLE app24.PerfilApp (
    id        BIGINT IDENTITY(1,1) NOT NULL,
    nombre    VARCHAR(80)  NOT NULL,
    estado    VARCHAR(20)  NOT NULL DEFAULT 'ACTIVO',
    CONSTRAINT PK_PerfilApp PRIMARY KEY (id),
    CONSTRAINT UQ_PerfilApp_nombre UNIQUE (nombre)
);
GO

IF OBJECT_ID('app24.Actividad', 'U') IS NOT NULL
    DROP TABLE app24.Actividad;
GO
CREATE TABLE app24.Actividad (
    id      BIGINT IDENTITY(1,1) NOT NULL,
    clave   VARCHAR(60)  NOT NULL,
    nombre  VARCHAR(120) NOT NULL,
    recurso VARCHAR(120) NOT NULL,
    accion  VARCHAR(40)  NOT NULL,
    CONSTRAINT PK_Actividad PRIMARY KEY (id),
    CONSTRAINT UQ_Actividad_clave UNIQUE (clave)
);
GO

IF OBJECT_ID('app24.PerfilActividad', 'U') IS NOT NULL
    DROP TABLE app24.PerfilActividad;
GO
CREATE TABLE app24.PerfilActividad (
    perfil_id    BIGINT NOT NULL,
    actividad_id BIGINT NOT NULL,
    CONSTRAINT PK_PerfilActividad PRIMARY KEY (perfil_id, actividad_id),
    CONSTRAINT FK_PerfilActividad_Perfil  FOREIGN KEY (perfil_id)    REFERENCES app24.PerfilApp (id),
    CONSTRAINT FK_PerfilActividad_Actividad FOREIGN KEY (actividad_id) REFERENCES app24.Actividad (id)
);
GO

IF OBJECT_ID('app24.UsuarioApp', 'U') IS NOT NULL
    DROP TABLE app24.UsuarioApp;
GO
CREATE TABLE app24.UsuarioApp (
    id            BIGINT IDENTITY(1,1) NOT NULL,
    clave         VARCHAR(30)  NOT NULL,
    nombre        VARCHAR(120) NOT NULL,
    correo        VARCHAR(150) NOT NULL,
    password_hash VARCHAR(100) NOT NULL,
    estado        VARCHAR(20)  NOT NULL DEFAULT 'ACTIVO',
    vigencia      DATE         NULL,
    perfil_id     BIGINT       NOT NULL,
    CONSTRAINT PK_UsuarioApp PRIMARY KEY (id),
    CONSTRAINT UQ_UsuarioApp_clave UNIQUE (clave),
    CONSTRAINT UQ_UsuarioApp_correo UNIQUE (correo),
    CONSTRAINT FK_UsuarioApp_Perfil FOREIGN KEY (perfil_id) REFERENCES app24.PerfilApp (id)
);
GO

-- ------------------------------------------------------------------
-- Bitácora (inmutable, sin secretos)
-- ------------------------------------------------------------------

IF OBJECT_ID('app24.BitacoraEvento', 'U') IS NOT NULL
    DROP TABLE app24.BitacoraEvento;
GO
CREATE TABLE app24.BitacoraEvento (
    id             BIGINT IDENTITY(1,1) NOT NULL,
    usuario_id     BIGINT       NULL,
    fecha          DATETIME2(3) NOT NULL CONSTRAINT DF_BitacoraEvento_fecha DEFAULT SYSUTCDATETIME(),
    modulo         VARCHAR(80)  NOT NULL,
    accion         VARCHAR(40)  NOT NULL,
    detalle        VARCHAR(500) NULL,
    correlacion_id VARCHAR(40)  NULL,
    resultado      VARCHAR(20)  NOT NULL,
    CONSTRAINT PK_BitacoraEvento PRIMARY KEY (id),
    CONSTRAINT FK_BitacoraEvento_Usuario FOREIGN KEY (usuario_id) REFERENCES app24.UsuarioApp (id)
);
GO

CREATE NONCLUSTERED INDEX IX_BitacoraEvento_fecha ON app24.BitacoraEvento (fecha DESC);
GO
CREATE NONCLUSTERED INDEX IX_BitacoraEvento_correlacion ON app24.BitacoraEvento (correlacion_id);
GO

-- ------------------------------------------------------------------
-- Carga de facturación
-- ------------------------------------------------------------------

IF OBJECT_ID('app24.CargaFacturacion', 'U') IS NOT NULL
    DROP TABLE app24.CargaFacturacion;
GO
CREATE TABLE app24.CargaFacturacion (
    id                 BIGINT IDENTITY(1,1) NOT NULL,
    archivo            VARCHAR(255) NOT NULL,
    hash               VARCHAR(64)  NOT NULL,
    fecha              DATETIME2(3) NOT NULL CONSTRAINT DF_CargaFacturacion_fecha DEFAULT SYSUTCDATETIME(),
    usuario_id         BIGINT       NOT NULL,
    estado             VARCHAR(20)  NOT NULL,
    total_registros    INT          NOT NULL DEFAULT 0,
    registros_validos  INT          NOT NULL DEFAULT 0,
    registros_invalidos INT         NOT NULL DEFAULT 0,
    CONSTRAINT PK_CargaFacturacion PRIMARY KEY (id),
    CONSTRAINT UQ_CargaFacturacion_hash UNIQUE (hash),
    CONSTRAINT FK_CargaFacturacion_Usuario FOREIGN KEY (usuario_id) REFERENCES app24.UsuarioApp (id)
);
GO

IF OBJECT_ID('app24.ErrorCarga', 'U') IS NOT NULL
    DROP TABLE app24.ErrorCarga;
GO
CREATE TABLE app24.ErrorCarga (
    id        BIGINT IDENTITY(1,1) NOT NULL,
    carga_id  BIGINT       NOT NULL,
    hoja      VARCHAR(80)  NULL,
    fila      INT          NULL,
    columna   VARCHAR(80)  NULL,
    valor     VARCHAR(500) NULL,
    regla     VARCHAR(120) NULL,
    mensaje   VARCHAR(500) NOT NULL,
    CONSTRAINT PK_ErrorCarga PRIMARY KEY (id),
    CONSTRAINT FK_ErrorCarga_Carga FOREIGN KEY (carga_id) REFERENCES app24.CargaFacturacion (id)
);
GO

CREATE NONCLUSTERED INDEX IX_ErrorCarga_carga ON app24.ErrorCarga (carga_id);
GO

-- ------------------------------------------------------------------
-- Plantilla de carga versionada
-- ------------------------------------------------------------------

IF OBJECT_ID('app24.ConfiguracionPlantilla', 'U') IS NOT NULL
    DROP TABLE app24.ConfiguracionPlantilla;
GO
CREATE TABLE app24.ConfiguracionPlantilla (
    id            BIGINT IDENTITY(1,1) NOT NULL,
    nombre        VARCHAR(120) NOT NULL,
    version       VARCHAR(20)  NOT NULL,
    extension     VARCHAR(10)  NOT NULL,
    columnas_json NVARCHAR(MAX) NOT NULL,
    activa        BIT          NOT NULL DEFAULT 0,
    CONSTRAINT PK_ConfiguracionPlantilla PRIMARY KEY (id),
    CONSTRAINT UQ_ConfiguracionPlantilla_nombre_version UNIQUE (nombre, version)
);
GO

PRINT '=== ESQUEMA app24 CREADO ===';
GO