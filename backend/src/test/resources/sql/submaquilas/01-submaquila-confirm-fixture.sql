/*
 * Réplica sintética del stage legacy dbo.TMPSUBMAQUILA y de las tablas
 * autoritativas que toca dbo.CARGA_SUBMAQUILA, auditadas READ-ONLY contra
 * CALE_IMMEX LIVE el 2026-10-05. Se carga en SQL Testcontainers.
 *
 * Sólo se declaran las columnas que el SP legacy y el wrapper leen o escriben,
 * con los tipos y longitudes reales para que los límites del parser sean
 * verificables contra las restricciones físicas verdaderas.
 */

USE [CALE_IMMEX];
GO

-- Stage legacy auditado (compat level 100).
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

-- dbo.SALIDAS (columnas reales: SalidaKey NUMERIC(18,0) NOT NULL).
IF OBJECT_ID('dbo.SALIDAS', 'U') IS NULL
CREATE TABLE dbo.SALIDAS (
    SalidaKey       NUMERIC(18,0) NOT NULL,
    Tipo_operacion  CHAR(25) NULL,
    Aduana          CHAR(10) NULL,
    Agente          CHAR(10) NULL,
    Documento       CHAR(60) NULL,
    Fecha           DATETIME  NULL,
    Cve_cliente     CHAR(15) NULL,
    Cve_pedimento   CHAR(5)  NULL,
    Transfiere      CHAR(20) NULL,
    Pais            CHAR(10) NULL,
    CONSTRAINT PK_SALIDAS PRIMARY KEY CLUSTERED (SalidaKey)
);
GO

-- dbo.PSALIDAS (columnas reales: Psalidakey NUMERIC(18,0) NOT NULL, Fraccion CHAR(12),
-- Unidad CHAR(5), Salidalink FLOAT, Descripcion VARCHAR(250), partida FLOAT).
IF OBJECT_ID('dbo.PSALIDAS', 'U') IS NULL
CREATE TABLE dbo.PSALIDAS (
    Psalidakey   NUMERIC(18,0) NOT NULL,
    Fraccion     CHAR(12)  NULL,
    Descripcion VARCHAR(250) NULL,
    Cantidad     NUMERIC(18,4) NULL,
    Unidad       CHAR(5)   NULL,
    Salidalink   FLOAT     NULL,
    Clave        VARCHAR(50) NULL,
    partida      FLOAT     NULL,
    CONSTRAINT PK_PSALIDAS PRIMARY KEY CLUSTERED (Psalidakey)
);
GO

-- Catálogo de productos del que el legacy resuelve la fracción.
IF OBJECT_ID('dbo.PRODUCTOS', 'U') IS NULL
CREATE TABLE dbo.PRODUCTOS (
    CVE_PRODUCTO VARCHAR(50) NULL,
    NOMBRE       VARCHAR(250) NULL,
    UNIDAD       CHAR(5) NULL,
    Fraccion     CHAR(12) NULL
);
GO

-- Producto sintético: la fracción tiene exactamente 8 dígitos y es el contrato
-- que ya valida el parser para materiales y productos.
INSERT INTO dbo.PRODUCTOS (CVE_PRODUCTO, NOMBRE, UNIDAD, Fraccion)
VALUES ('P001', 'Producto sintetico uno', 'PIEZ', '7308.10.01');
GO
