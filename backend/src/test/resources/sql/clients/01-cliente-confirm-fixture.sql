/*
 * Fixture exclusivamente sintético para ClientLegacyStageConcurrencyTest.
 * Se ejecuta sobre las bases efímeras CALE_IMMEX y ANEXO24_DEV del contenedor.
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

/* ============================== ANEXO24_DEV ============================= */
USE [ANEXO24_DEV];
GO

CREATE SCHEMA app24;
GO

CREATE TABLE app24.CargaCatalogoCliente (
    id BIGINT IDENTITY(1,1) NOT NULL PRIMARY KEY,
    archivo VARCHAR(255) NOT NULL,
    hash CHAR(64) NOT NULL,
    usuario_id BIGINT NOT NULL,
    estado VARCHAR(20) NOT NULL CONSTRAINT CK_CargaCatalogoCliente_estado
        CHECK (estado IN ('PREVISUALIZADA', 'CONFIRMADA', 'CON_ERRORES')),
    total_filas INT NOT NULL,
    filas_validas INT NOT NULL,
    filas_invalidas INT NOT NULL,
    version_contrato VARCHAR(100) NOT NULL,
    correlation_id VARCHAR(100) NOT NULL,
    fecha_confirmacion DATETIME2 NULL
);
GO

CREATE TABLE app24.CargaCatalogoClienteFila (
    carga_id BIGINT NOT NULL,
    hoja VARCHAR(100) NOT NULL,
    fila INT NOT NULL,
    datos_json NVARCHAR(MAX) NOT NULL,
    CONSTRAINT PK_CargaCatalogoClienteFila PRIMARY KEY (carga_id, hoja, fila),
    CONSTRAINT FK_CargaCatalogoClienteFila_Carga FOREIGN KEY (carga_id) REFERENCES app24.CargaCatalogoCliente(id)
);
GO

CREATE TABLE app24.ErrorCargaCliente (
    carga_id BIGINT NOT NULL,
    hoja VARCHAR(100) NOT NULL,
    fila INT NOT NULL,
    columna VARCHAR(100) NOT NULL,
    valor_enmascarado VARCHAR(500) NULL,
    codigo VARCHAR(50) NOT NULL,
    mensaje VARCHAR(500) NOT NULL,
    CONSTRAINT FK_ErrorCargaCliente_Carga FOREIGN KEY (carga_id) REFERENCES app24.CargaCatalogoCliente(id)
);
GO

CREATE TABLE app24.PerfilApp (
    id BIGINT IDENTITY(1,1) NOT NULL PRIMARY KEY,
    nombre VARCHAR(100) NOT NULL UNIQUE
);
GO

CREATE TABLE app24.Actividad (
    id BIGINT IDENTITY(1,1) NOT NULL PRIMARY KEY,
    clave VARCHAR(100) NOT NULL UNIQUE,
    nombre VARCHAR(200) NOT NULL,
    recurso VARCHAR(100) NOT NULL,
    accion VARCHAR(100) NOT NULL
);
GO

CREATE TABLE app24.PerfilActividad (
    perfil_id BIGINT NOT NULL,
    actividad_id BIGINT NOT NULL,
    CONSTRAINT PK_PerfilActividad PRIMARY KEY (perfil_id, actividad_id)
);
GO

INSERT INTO app24.PerfilApp (nombre) VALUES ('ADMINISTRADOR');
GO

CREATE TABLE app24.BitacoraEvento (
    id BIGINT IDENTITY(1,1) NOT NULL PRIMARY KEY,
    usuario_id BIGINT NOT NULL,
    modulo VARCHAR(100) NOT NULL,
    accion VARCHAR(80) NOT NULL,
    detalle VARCHAR(500) NOT NULL,
    correlation_id VARCHAR(100) NOT NULL,
    resultado VARCHAR(50) NOT NULL,
    fecha DATETIME2 NOT NULL CONSTRAINT DF_BitacoraEvento_fecha DEFAULT SYSUTCDATETIME()
);
GO

CREATE PROCEDURE app24.APP24_C_BITACORA_REGISTRAR
    @UsuarioId BIGINT,
    @Modulo VARCHAR(100),
    @Accion VARCHAR(80),
    @Detalle VARCHAR(500),
    @CorrelacionId VARCHAR(100),
    @Resultado VARCHAR(50),
    @EventoId BIGINT OUTPUT
AS
BEGIN
    SET NOCOUNT ON;
    INSERT INTO app24.BitacoraEvento (usuario_id, modulo, accion, detalle, correlation_id, resultado)
    VALUES (@UsuarioId, @Modulo, @Accion, @Detalle, @CorrelacionId, @Resultado);
    SET @EventoId = SCOPE_IDENTITY();
END;
GO