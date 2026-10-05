/*
 * Fixture exclusivamente sintético para AgentLegacyStageConcurrencyTest.
 * Sólo crea el DDL mínimo de CALE_IMMEX necesario para validar el wrapper.
 * Las tablas app24.CargaCatalogoAgente* las crea la migration productiva 17.
 * Layout copiado de la metadata LIVE de CALE_IMMEX auditada READ-ONLY 2026-10-05.
 */

/* ============================== CALE_IMMEX ============================== */
USE [CALE_IMMEX];
GO

ALTER DATABASE [CALE_IMMEX] SET COMPATIBILITY_LEVEL = 100;
GO

CREATE TABLE dbo.agentes (
    Clave CHAR(10) NOT NULL,
    Nombre CHAR(40) NULL,
    Domicilio CHAR(60) NULL,
    Rfc CHAR(20) NULL,
    Patente CHAR(10) NULL,
    Domicilio2 CHAR(60) NULL,
    AgenciaAduanal VARCHAR(60) NULL,
    RFCAgencia VARCHAR(50) NULL,
    Telefono VARCHAR(50) NULL,
    Contacto VARCHAR(100) NULL,
    AltaImportacion VARCHAR(50) NULL,
    AltaExportacion VARCHAR(50) NULL,
    [Proveedor/cliente] VARCHAR(250) NULL
);
GO

CREATE TABLE dbo.TMPagentes (
    AGENTEKEY INT IDENTITY(1,1) NOT NULL PRIMARY KEY,
    Clave CHAR(10) NOT NULL,
    Nombre CHAR(40) NULL,
    Domicilio CHAR(60) NULL,
    Rfc CHAR(20) NULL,
    Patente CHAR(10) NULL
);
GO

CREATE TABLE dbo.ECARGAagentes (
    EPKEY BIGINT IDENTITY(1,1) NOT NULL PRIMARY KEY,
    TMPKEY BIGINT NULL,
    ERROR VARCHAR(150) NULL,
    CLAVE VARCHAR(50) NULL
);
GO