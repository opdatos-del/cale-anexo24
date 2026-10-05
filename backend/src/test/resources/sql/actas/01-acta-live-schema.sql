-- Schema fiel a la metadata LIVE de CALE_IMMEX (2026-10-05) para el contrato
-- dbo.CARGAACTAS: dbo.ACTA, dbo.SALIDAS, dbo.PSALIDAS, dbo.DIRIGIDO,
-- dbo.GENERADORES y los catalogos dbo.MATERIAL / dbo.PRODUCTOS.
USE [CALE_IMMEX];
GO

IF OBJECT_ID('dbo.ACTA', 'U') IS NULL
CREATE TABLE dbo.ACTA (
    actakey         BIGINT IDENTITY(1,1) NOT NULL,
    Folio           VARCHAR(50) NULL,
    fecha           DATETIME NULL,
    clave           VARCHAR(50) NULL,
    linea           INT NULL,
    cantidad        FLOAT NULL,
    umc             VARCHAR(50) NULL,
    descargadirigida VARCHAR(50) NULL,
    VALORCOMERCIAL  NUMERIC(18,10) NULL,
    CONSTRAINT PK_ACTA PRIMARY KEY CLUSTERED (actakey)
);
GO

IF OBJECT_ID('dbo.SALIDAS', 'U') IS NULL
CREATE TABLE dbo.SALIDAS (
    SalidaKey       NUMERIC(18,0) NOT NULL,
    Documento       CHAR(60) NULL,
    Fecha           DATETIME NULL,
    Tipo_operacion  CHAR(25) NULL,
    Cve_pedimento   CHAR(5)  NULL,
    Cve_cliente     CHAR(15) NULL,
    Aduana          CHAR(10) NULL,
    Agente          CHAR(10) NULL,
    Pais            CHAR(10) NULL,
    TC              FLOAT    NULL,
    bloqueado       NUMERIC(18,0) NULL,
    CONSTRAINT PK_SALIDAS PRIMARY KEY CLUSTERED (SalidaKey)
);
GO

IF OBJECT_ID('dbo.PSALIDAS', 'U') IS NULL
CREATE TABLE dbo.PSALIDAS (
    Psalidakey      NUMERIC(18,0) NOT NULL,
    Clave           VARCHAR(50) NULL,
    Descripcion     VARCHAR(250) NULL,
    Cantidad        NUMERIC(18,4) NULL,
    Fraccion        CHAR(12) NULL,
    Unidad          CHAR(5)  NULL,
    Salidalink      FLOAT    NULL,
    partida         FLOAT    NULL,
    descargaDirigida VARCHAR(50) NULL,
    Fecha           DATETIME NULL,
    Val_pesos       NUMERIC(18,4) NULL,
    CONSTRAINT PK_PSALIDAS PRIMARY KEY CLUSTERED (Psalidakey)
);
GO

IF OBJECT_ID('dbo.DIRIGIDO', 'U') IS NULL
CREATE TABLE dbo.DIRIGIDO (
    dirigidokey     BIGINT NOT NULL,
    documento       VARCHAR(35) NULL,
    clave           VARCHAR(50) NULL,
    incorporado     FLOAT NULL,
    desperdicio     FLOAT NULL,
    merma           FLOAT NULL,
    salidakey       INT NULL,
    psalidakey      INT NULL,
    CONSTRAINT PK_DIRIGIDO PRIMARY KEY CLUSTERED (dirigidokey)
);
GO

IF OBJECT_ID('dbo.GENERADORES', 'U') IS NULL
CREATE TABLE dbo.GENERADORES (
    tabla           CHAR(40) NOT NULL,
    consecutivo     INT NULL,
    CONSTRAINT PK_GENERADORES PRIMARY KEY CLUSTERED (tabla)
);
GO

IF OBJECT_ID('dbo.MATERIAL', 'U') IS NULL
CREATE TABLE dbo.MATERIAL (
    materialkey     NUMERIC(18,0) NOT NULL,
    clave           VARCHAR(50) NULL,
    descripcion     VARCHAR(250) NULL,
    fraccion        CHAR(10) NULL,
    unidad          CHAR(5)  NULL,
    CONSTRAINT PK_MATERIAL PRIMARY KEY CLUSTERED (materialkey)
);
GO

IF OBJECT_ID('dbo.PRODUCTOS', 'U') IS NULL
CREATE TABLE dbo.PRODUCTOS (
    CVE_PRODUCTO VARCHAR(50) NULL,
    NOMBRE       VARCHAR(250) NULL,
    UNIDAD       CHAR(5) NULL,
    Fraccion     CHAR(12) NULL
);
GO

INSERT INTO dbo.PRODUCTOS (CVE_PRODUCTO, NOMBRE, UNIDAD, Fraccion)
VALUES ('P001', 'Producto sintetico uno', 'PIEZ', '7308.10.01');
GO
