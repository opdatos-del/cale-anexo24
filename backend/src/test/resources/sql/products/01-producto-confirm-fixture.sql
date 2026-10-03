/*
 * Fixture exclusivamente sintético para ProductLegacyStageConcurrencyTest.
 * Se ejecuta sobre las bases efímeras CALE_IMMEX y ANEXO24_DEV del contenedor.
 * Reproduce únicamente el DDL mínimo compatible con la réplica del SP legacy.
 */

/* ============================== CALE_IMMEX ============================== */
USE [CALE_IMMEX];
GO

ALTER DATABASE [CALE_IMMEX] SET COMPATIBILITY_LEVEL = 100;
GO

CREATE TABLE dbo.UNIDAD (
    CVE_UNIDAD VARCHAR(20) NOT NULL PRIMARY KEY
);
GO

INSERT INTO dbo.UNIDAD (CVE_UNIDAD) VALUES ('KG'), ('PZA');
GO

CREATE TABLE dbo.tmpproductos (
    tmpPRODUCTOKEY numeric NOT NULL PRIMARY KEY IDENTITY(1, 1),
    CVE_PRODUCTO varchar(30) NULL,
    NOMBRE varchar(50) NULL,
    UNIDAD varchar(10) NULL,
    fraccion varchar(12) NULL,
    DIVISION varchar(50) NULL,
    CVE_PRODUCTO_CLIENTE varchar(30) NULL,
    AUXILIAR varchar(50) NULL
);
GO

CREATE TABLE dbo.ECargaProducto (
    ECargaProductokey bigint IDENTITY(1,1) NOT NULL PRIMARY KEY,
    CargaProductoKey bigint NOT NULL,
    error varchar(150) NOT NULL
);
GO

CREATE TABLE dbo.productos (
    PRODUCTOKEY numeric(18,0) NOT NULL PRIMARY KEY,
    CVE_PRODUCTO varchar(50) NULL,
    NOMBRE varchar(250) NULL,
    UNIDAD varchar(10) NULL,
    fraccion varchar(12) NULL,
    CVE_PRODUCTO_CLIENTE varchar(30) NULL,
    ALMACENKEY numeric(18,0) NULL,
    AUXILIAR varchar(50) NULL,
    TIPO varchar(20) NULL,
    UNIDADT varchar(10) NULL,
    NICO varchar(5) NULL
);
GO

CREATE FUNCTION dbo.VALIDUNIT (@unidad VARCHAR(20))
RETURNS VARCHAR(20)
AS
BEGIN
    RETURN @unidad;
END;
GO

CREATE FUNCTION dbo.ENTIDAD (@division VARCHAR(30))
RETURNS BIGINT
AS
BEGIN
    RETURN 1;
END;
GO

/* ============================== ANEXO24_DEV ============================= */
USE [ANEXO24_DEV];
GO

CREATE SCHEMA app24;
GO

CREATE TABLE app24.CargaCatalogoProducto (
    id BIGINT IDENTITY(1,1) NOT NULL PRIMARY KEY,
    archivo VARCHAR(255) NOT NULL,
    hash CHAR(64) NOT NULL,
    usuario_id BIGINT NOT NULL,
    estado VARCHAR(20) NOT NULL CONSTRAINT CK_CargaCatalogoProducto_estado
        CHECK (estado IN ('PREVISUALIZADA', 'CONFIRMADA', 'CON_ERRORES')),
    total_filas INT NOT NULL,
    filas_validas INT NOT NULL,
    filas_invalidas INT NOT NULL,
    version_contrato VARCHAR(100) NOT NULL,
    correlation_id VARCHAR(100) NOT NULL,
    fecha_confirmacion DATETIME2 NULL
);
GO

CREATE TABLE app24.CargaCatalogoProductoFila (
    carga_id BIGINT NOT NULL,
    hoja VARCHAR(100) NOT NULL,
    fila INT NOT NULL,
    datos_json NVARCHAR(MAX) NOT NULL,
    CONSTRAINT PK_CargaCatalogoProductoFila PRIMARY KEY (carga_id, hoja, fila),
    CONSTRAINT FK_CargaCatalogoProductoFila_Carga FOREIGN KEY (carga_id) REFERENCES app24.CargaCatalogoProducto(id)
);
GO

CREATE TABLE app24.ErrorCargaProducto (
    carga_id BIGINT NOT NULL,
    hoja VARCHAR(100) NOT NULL,
    fila INT NOT NULL,
    columna VARCHAR(100) NOT NULL,
    valor_enmascarado VARCHAR(500) NULL,
    codigo VARCHAR(50) NOT NULL,
    mensaje VARCHAR(500) NOT NULL,
    CONSTRAINT FK_ErrorCargaProducto_Carga FOREIGN KEY (carga_id) REFERENCES app24.CargaCatalogoProducto(id)
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