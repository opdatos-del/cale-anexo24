/*
 * Fixture exclusivamente sintético para ProviderLegacyStageConcurrencyTest.
 * Sólo crea el DDL mínimo de CALE_IMMEX necesario para validar el wrapper.
 * Las tablas app24.CargaCatalogoProveedor* las crea la migration productiva 16.
 */

/* ============================== CALE_IMMEX ============================== */
USE [CALE_IMMEX];
GO

ALTER DATABASE [CALE_IMMEX] SET COMPATIBILITY_LEVEL = 100;
GO

CREATE TABLE dbo.Proveedores (
    Clave CHAR(15) NOT NULL PRIMARY KEY,
    Nombre VARCHAR(150) NULL,
    Idfiscal CHAR(15) NULL,
    Tipone CHAR(2) NULL,
    Programa CHAR(20) NULL,
    CalleNumero CHAR(150) NULL,
    Codigo CHAR(10) NULL,
    Colonia CHAR(150) NULL,
    Entidad CHAR(150) NULL,
    Pais CHAR(15) NULL,
    Telefono CHAR(25) NULL,
    Correo CHAR(40) NULL,
    Fax CHAR(25) NULL,
    proveedorkey NUMERIC(18,0) NOT NULL,
    ALMACENKEY NUMERIC(18,0) NULL,
    ApellidoPaterno VARCHAR(50) NULL,
    ApellidoMaterno VARCHAR(50) NULL,
    calle VARCHAR(150) NULL,
    callenumerointerior INT NULL,
    localidad CHAR(150) NULL,
    referencia VARCHAR(100) NULL,
    municipio CHAR(150) NULL,
    tipoidentificador VARCHAR(50) NULL,
    codigopostal VARCHAR(50) NULL
);
GO

CREATE TABLE dbo.TMPPROVEEDORES (
    TMPPROVEEDORKEY bigint IDENTITY(1,1) NOT NULL PRIMARY KEY,
    Clave CHAR(50) NULL,
    Nombre VARCHAR(150) NULL,
    Idfiscal CHAR(50) NULL,
    Tipone CHAR(2) NULL,
    Programa CHAR(120) NULL,
    CalleNumero CHAR(150) NULL,
    Codigo CHAR(110) NULL,
    Colonia CHAR(130) NULL,
    Entidad CHAR(140) NULL,
    Pais CHAR(115) NULL,
    Telefono CHAR(125) NULL,
    Correo CHAR(140) NULL,
    Fax CHAR(125) NULL,
    ALMACENKEY NUMERIC(18,0) NULL,
    ApellidoPaterno VARCHAR(50) NULL,
    ApellidoMaterno VARCHAR(50) NULL,
    calle VARCHAR(50) NULL,
    callenumerointerior INT NULL,
    localidad VARCHAR(50) NULL,
    referencia VARCHAR(100) NULL,
    municipio VARCHAR(50) NULL,
    tipoidentificador VARCHAR(50) NULL,
    codigopostal VARCHAR(50) NULL
);
GO

CREATE TABLE dbo.ECARGAPROVEEDORES (
    id int IDENTITY(1,1) NOT NULL PRIMARY KEY,
    TMPKEY bigint NOT NULL,
    ERROR VARCHAR(150) NOT NULL,
    CLAVE VARCHAR(50) NULL
);
GO