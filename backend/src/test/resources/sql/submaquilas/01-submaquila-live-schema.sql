-- Schema fiel a la metadata LIVE de CALE_IMMEX (2026-10-05) para las tablas que
-- toca dbo.CARGA_SUBMAQUILA. Se declaran las columnas referenciadas con sus tipos
-- reales. El objetivo es reproducir que SALIDAS.TipoOperacion es INT en LIVE,
-- mientras el SP intenta insertar 'SUBMAQUILA' en ella.

USE [CALE_IMMEX];
GO

IF OBJECT_ID('dbo.TMPSUBMAQUILA', 'U') IS NULL
CREATE TABLE dbo.TMPSUBMAQUILA (
    TMPSKEY        BIGINT IDENTITY(1,1) NOT NULL,
    FOLIO          VARCHAR(50) NULL,
    FECHA          DATE        NULL,
    SUBMAQUILADOR  VARCHAR(50) NULL,
    CLAVE          VARCHAR(50) NULL,
    CANTIDAD       NUMERIC(18,4) NULL,
    UNIDAD         VARCHAR(5)  NULL,
    DESCRIPCION    VARCHAR(250) NULL,
    LINEA          INT         NULL,
    CONSTRAINT PK_TMPSUBMAQUILA PRIMARY KEY CLUSTERED (TMPSKEY)
);
GO

IF OBJECT_ID('dbo.SALIDAS', 'U') IS NULL
CREATE TABLE dbo.SALIDAS (
    SalidaKey       NUMERIC(18,0) NOT NULL,
    Tipo_operacion  CHAR(25) NULL,
    TipoOperacion   INT NULL,
    Documento       CHAR(60) NULL,
    Fecha           DATETIME NULL,
    Cve_cliente     CHAR(15) NULL,
    Cve_pedimento   CHAR(5)  NULL,
    Transfiere      CHAR(20) NULL,
    CONSTRAINT PK_SALIDAS PRIMARY KEY CLUSTERED (SalidaKey)
);
GO

IF OBJECT_ID('dbo.PSALIDAS', 'U') IS NULL
CREATE TABLE dbo.PSALIDAS (
    Psalidakey   NUMERIC(18,0) NOT NULL,
    Factura      CHAR(50)  NULL,
    Fecha        DATETIME  NULL,
    Fraccion     CHAR(12)  NULL,
    Descripcion  VARCHAR(250) NULL,
    Cantidad     NUMERIC(18,4) NULL,
    Unidad       CHAR(5)   NULL,
    Salidalink   FLOAT     NULL,
    Clave        VARCHAR(50) NULL,
    partida      FLOAT     NULL,
    CONSTRAINT PK_PSALIDAS PRIMARY KEY CLUSTERED (Psalidakey)
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
