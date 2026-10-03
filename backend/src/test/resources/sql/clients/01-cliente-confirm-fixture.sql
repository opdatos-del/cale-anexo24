/*
 * Fixture exclusivamente sintético para ClientLegacyStageConcurrencyTest.
 * Sólo crea el DDL mínimo de CALE_IMMEX necesario para validar el wrapper.
 * Las tablas app24.CargaCatalogoCliente* son creadas por la migration
 * productiva 15, NO por este fixture.
 */

/* ============================== CALE_IMMEX ============================== */
USE [CALE_IMMEX];
GO

ALTER DATABASE [CALE_IMMEX] SET COMPATIBILITY_LEVEL = 100;
GO

CREATE TABLE dbo.clientes (
    Clave CHAR(15) NOT NULL PRIMARY KEY,
    Nombre VARCHAR(150) NULL,
    Idfiscal CHAR(20) NULL,
    Tipone CHAR(2) NULL,
    Programa CHAR(20) NULL,
    Callenumero CHAR(50) NULL,
    Codigo CHAR(15) NULL,
    Colonia CHAR(30) NULL,
    Entidad CHAR(30) NULL,
    Pais CHAR(15) NULL,
    Telefono CHAR(25) NULL,
    Correo CHAR(40) NULL,
    Fax CHAR(25) NULL,
    clientekey NUMERIC(18,0) NOT NULL,
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

CREATE TABLE dbo.TMPCLIENTES (
    TMPCLIENTEKEY bigint IDENTITY(1,1) NOT NULL PRIMARY KEY,
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

CREATE TABLE dbo.ECARGACLIENTES (
    EPKEY bigint IDENTITY(1,1) NOT NULL PRIMARY KEY,
    TMPKEY bigint NOT NULL,
    ERROR VARCHAR(150) NOT NULL,
    CLAVE VARCHAR(50) NULL
);
GO

CREATE TABLE dbo.ECARGAPROVEEDORES (
    id int IDENTITY(1,1) NOT NULL PRIMARY KEY,
    TMPKEY bigint NOT NULL,
    ERROR VARCHAR(150) NOT NULL,
    CLAVE VARCHAR(50) NULL
);
GO