-- Schema fiel a la metadata LIVE de CALE_IMMEX (2026-10-06) para el contrato
-- dbo.CARGACONSTANCIAS: dbo.Constanciatransf (stage), dbo.errorcarga (shared),
-- dbo.settings, dbo.productos, dbo.Importaciones, dbo.partidas, dbo.salidas,
-- dbo.psalidas, dbo.dirigido, dbo.clientes, dbo.Proveedores, dbo.generadores.
-- Tipos, longitudes, precision/scale, nullability, IDENTITY y PK segun sys.columns.
USE [CALE_IMMEX];
GO

IF OBJECT_ID('dbo.settings', 'U') IS NULL
CREATE TABLE dbo.settings (
    settings VARCHAR(50) NOT NULL,
    value    VARCHAR(150) NULL,
    CONSTRAINT PK_settings PRIMARY KEY CLUSTERED (settings)
);
GO

IF OBJECT_ID('dbo.Constanciatransf', 'U') IS NULL
CREATE TABLE dbo.Constanciatransf (
    CONSTANCIAKEY  BIGINT IDENTITY(1,1) NOT NULL,
    NUMERODEFOLIO  VARCHAR(50) NULL,
    PERIODO        DATETIME NULL,
    SEC            VARCHAR(5) NULL,
    FECHACREACION  DATETIME NULL,
    PROV           VARCHAR(50) NULL,
    LIN            NUMERIC(18,0) NULL,
    NOPARTE        VARCHAR(50) NULL,
    DESCRIPCION    VARCHAR(100) NULL,
    CANTIDAD       NUMERIC(18,0) NULL,
    PEDIMENTO      VARCHAR(50) NULL,
    ADUANA         VARCHAR(10) NULL,
    FECHAPEDIMENTO DATETIME NULL,
    MER            VARCHAR(10) NULL,
    PAI            VARCHAR(10) NULL,
    ER             VARCHAR(5) NULL,
    Val_dolares    DECIMAL(18,10) NULL,
    Val_Comercial  DECIMAL(18,10) NULL,
    CONSTRAINT PK_Constanciatransf PRIMARY KEY CLUSTERED (CONSTANCIAKEY)
);
GO

IF OBJECT_ID('dbo.errorcarga', 'U') IS NULL
CREATE TABLE dbo.errorcarga (
    errorkey BIGINT IDENTITY(1,1) NOT NULL,
    error    VARCHAR(250) NULL,
    cargakey BIGINT NULL,
    CONSTRAINT PK_errorcarga PRIMARY KEY CLUSTERED (errorkey)
);
GO

IF OBJECT_ID('dbo.productos', 'U') IS NULL
CREATE TABLE dbo.productos (
    PRODUCTOKEY          NUMERIC(18,0) NOT NULL,
    CVE_PRODUCTO         VARCHAR(50) NULL,
    NOMBRE               VARCHAR(250) NULL,
    UNIDAD               VARCHAR(10) NULL,
    fraccion             VARCHAR(12) NULL,
    CVE_PRODUCTO_CLIENTE VARCHAR(30) NULL,
    ALMACENKEY           NUMERIC(18,0) NULL,
    AUXILIAR             VARCHAR(50) NULL,
    TIPO                 VARCHAR(20) NULL,
    UNIDADT              VARCHAR(10) NULL,
    NICO                 VARCHAR(5) NULL,
    CONSTRAINT PK_productos PRIMARY KEY CLUSTERED (PRODUCTOKEY)
);
GO

IF OBJECT_ID('dbo.Importaciones', 'U') IS NULL
CREATE TABLE dbo.Importaciones (
    Ipedimentokey NUMERIC(18,0) NOT NULL,
    CONSTRAINT PK_Importaciones PRIMARY KEY CLUSTERED (Ipedimentokey)
);
GO

IF OBJECT_ID('dbo.partidas', 'U') IS NULL
CREATE TABLE dbo.partidas (
    Partidakey NUMERIC(18,0) NOT NULL,
    CONSTRAINT PK_partidas PRIMARY KEY CLUSTERED (Partidakey)
);
GO

IF OBJECT_ID('dbo.salidas', 'U') IS NULL
CREATE TABLE dbo.salidas (
    SalidaKey         NUMERIC(18,0) NOT NULL,
    Tipo_operacion    CHAR(25) NULL,
    Aduana            CHAR(10) NULL,
    Agente            CHAR(10) NULL,
    Documento         CHAR(60) NULL,
    Fecha             DATETIME NULL,
    Cve_cliente       CHAR(15) NULL,
    Cve_pedimento     CHAR(5)  NULL,
    Pais              CHAR(10) NULL,
    Origen            CHAR(30) NULL,
    Tc                FLOAT    NULL,
    dta               FLOAT    NULL,
    DESCARGA          VARCHAR(2) NULL,
    PedimentoOriginal VARCHAR(20) NULL,
    prev              FLOAT    NULL,
    FECHADESCARGA     DATETIME NULL,
    CONSTRAINT PK_salidas PRIMARY KEY CLUSTERED (SalidaKey)
);
GO

IF OBJECT_ID('dbo.psalidas', 'U') IS NULL
CREATE TABLE dbo.psalidas (
    Psalidakey              NUMERIC(18,0) NOT NULL,
    Factura                 CHAR(50) NULL,
    Fecha                   DATETIME NULL,
    Fraccion                CHAR(12) NULL,
    Descripcion             VARCHAR(250) NULL,
    Cantidad                NUMERIC(18,4) NULL,
    Unidad                  CHAR(5)  NULL,
    Val_pesos               NUMERIC(18,4) NULL,
    Val_dolares             NUMERIC(18,4) NULL,
    Salidalink              FLOAT    NULL,
    Clave                   VARCHAR(50) NULL,
    corte                   VARCHAR(50) NULL,
    Anexo                   CHAR(3)  NULL,
    partida                 FLOAT    NULL,
    paisd                   VARCHAR(10) NULL,
    unidadt                 VARCHAR(10) NULL,
    cantidadt               FLOAT    NULL,
    CodProveedor            VARCHAR(20) NULL,
    bloqueado               INT      NULL,
    descargaDirigida        VARCHAR(50) NULL,
    pedimentoexportacionctm VARCHAR(50) NULL,
    CONSTRAINT PK_psalidas PRIMARY KEY CLUSTERED (Psalidakey)
);
GO

IF OBJECT_ID('dbo.dirigido', 'U') IS NULL
CREATE TABLE dbo.dirigido (
    dirigidokey  BIGINT NOT NULL,
    documento    VARCHAR(35) NULL,
    clave        VARCHAR(50) NULL,
    incorporado  FLOAT NULL,
    desperdicio  FLOAT NULL,
    merma        FLOAT NULL,
    salidakey    INT NULL,
    psalidakey   INT NULL,
    CONSTRAINT PK_dirigido PRIMARY KEY CLUSTERED (dirigidokey)
);
GO

IF OBJECT_ID('dbo.clientes', 'U') IS NULL
CREATE TABLE dbo.clientes (
    Clave      CHAR(15) NOT NULL,
    clientekey NUMERIC(18,0) NULL
);
GO

IF OBJECT_ID('dbo.Proveedores', 'U') IS NULL
CREATE TABLE dbo.Proveedores (
    Clave        CHAR(15) NOT NULL,
    proveedorkey NUMERIC(18,0) NULL
);
GO

IF OBJECT_ID('dbo.generadores', 'U') IS NULL
CREATE TABLE dbo.generadores (
    tabla       CHAR(40) NOT NULL,
    consecutivo INT NULL,
    CONSTRAINT PK_generadores PRIMARY KEY CLUSTERED (tabla)
);
GO
