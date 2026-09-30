-- ANEXO24_DEV: staging aislado para importaciones V1 de materiales y productos.
-- No ejecuta CARGA_MATERIALES/CARGA_PRODUCTOS ni escribe CALE_IMMEX.
USE ANEXO24_DEV;
GO

IF OBJECT_ID('app24.CargaCatalogoMaterial', 'U') IS NULL
BEGIN
    CREATE TABLE app24.CargaCatalogoMaterial (
        id                BIGINT IDENTITY(1,1) NOT NULL,
        archivo           VARCHAR(255) NOT NULL,
        hash              VARCHAR(64) NOT NULL,
        fecha             DATETIME2(3) NOT NULL CONSTRAINT DF_CargaCatalogoMaterial_fecha DEFAULT SYSUTCDATETIME(),
        usuario_id        BIGINT NOT NULL,
        estado            VARCHAR(20) NOT NULL,
        total_filas       INT NOT NULL DEFAULT 0,
        filas_validas     INT NOT NULL DEFAULT 0,
        filas_invalidas   INT NOT NULL DEFAULT 0,
        version_contrato  VARCHAR(60) NOT NULL,
        correlation_id    VARCHAR(40) NOT NULL,
        CONSTRAINT PK_CargaCatalogoMaterial PRIMARY KEY (id),
        CONSTRAINT UQ_CargaCatalogoMaterial_hash UNIQUE (hash),
        CONSTRAINT FK_CargaCatalogoMaterial_Usuario FOREIGN KEY (usuario_id) REFERENCES app24.UsuarioApp (id),
        CONSTRAINT CK_CargaCatalogoMaterial_estado CHECK (estado IN ('PREVISUALIZADA', 'CON_ERRORES'))
    );
END;
GO
IF OBJECT_ID('app24.CargaCatalogoMaterialFila', 'U') IS NULL
BEGIN
    CREATE TABLE app24.CargaCatalogoMaterialFila (
        id BIGINT IDENTITY(1,1) NOT NULL,
        carga_id BIGINT NOT NULL,
        hoja VARCHAR(80) NOT NULL,
        fila INT NOT NULL,
        datos_json NVARCHAR(MAX) NOT NULL,
        CONSTRAINT PK_CargaCatalogoMaterialFila PRIMARY KEY (id),
        CONSTRAINT FK_CargaCatalogoMaterialFila_Carga FOREIGN KEY (carga_id) REFERENCES app24.CargaCatalogoMaterial(id),
        CONSTRAINT UQ_CargaCatalogoMaterialFila UNIQUE (carga_id, hoja, fila),
        CONSTRAINT CK_CargaCatalogoMaterialFila_JSON CHECK (ISJSON(datos_json) = 1)
    );
END;
GO
IF OBJECT_ID('app24.ErrorCargaMaterial', 'U') IS NULL
BEGIN
    CREATE TABLE app24.ErrorCargaMaterial (
        id BIGINT IDENTITY(1,1) NOT NULL,
        carga_id BIGINT NOT NULL,
        hoja VARCHAR(80) NULL,
        fila INT NULL,
        columna VARCHAR(80) NULL,
        valor_enmascarado VARCHAR(80) NULL,
        codigo VARCHAR(120) NOT NULL,
        mensaje VARCHAR(500) NOT NULL,
        CONSTRAINT PK_ErrorCargaMaterial PRIMARY KEY (id),
        CONSTRAINT FK_ErrorCargaMaterial_Carga FOREIGN KEY (carga_id) REFERENCES app24.CargaCatalogoMaterial(id)
    );
END;
GO
IF OBJECT_ID('app24.CargaCatalogoProducto', 'U') IS NULL
BEGIN
    CREATE TABLE app24.CargaCatalogoProducto (
        id                BIGINT IDENTITY(1,1) NOT NULL,
        archivo           VARCHAR(255) NOT NULL,
        hash              VARCHAR(64) NOT NULL,
        fecha             DATETIME2(3) NOT NULL CONSTRAINT DF_CargaCatalogoProducto_fecha DEFAULT SYSUTCDATETIME(),
        usuario_id        BIGINT NOT NULL,
        estado            VARCHAR(20) NOT NULL,
        total_filas       INT NOT NULL DEFAULT 0,
        filas_validas     INT NOT NULL DEFAULT 0,
        filas_invalidas   INT NOT NULL DEFAULT 0,
        version_contrato  VARCHAR(60) NOT NULL,
        correlation_id    VARCHAR(40) NOT NULL,
        CONSTRAINT PK_CargaCatalogoProducto PRIMARY KEY (id),
        CONSTRAINT UQ_CargaCatalogoProducto_hash UNIQUE (hash),
        CONSTRAINT FK_CargaCatalogoProducto_Usuario FOREIGN KEY (usuario_id) REFERENCES app24.UsuarioApp (id),
        CONSTRAINT CK_CargaCatalogoProducto_estado CHECK (estado IN ('PREVISUALIZADA', 'CON_ERRORES'))
    );
END;
GO
IF OBJECT_ID('app24.CargaCatalogoProductoFila', 'U') IS NULL
BEGIN
    CREATE TABLE app24.CargaCatalogoProductoFila (
        id BIGINT IDENTITY(1,1) NOT NULL,
        carga_id BIGINT NOT NULL,
        hoja VARCHAR(80) NOT NULL,
        fila INT NOT NULL,
        datos_json NVARCHAR(MAX) NOT NULL,
        CONSTRAINT PK_CargaCatalogoProductoFila PRIMARY KEY (id),
        CONSTRAINT FK_CargaCatalogoProductoFila_Carga FOREIGN KEY (carga_id) REFERENCES app24.CargaCatalogoProducto(id),
        CONSTRAINT UQ_CargaCatalogoProductoFila UNIQUE (carga_id, hoja, fila),
        CONSTRAINT CK_CargaCatalogoProductoFila_JSON CHECK (ISJSON(datos_json) = 1)
    );
END;
GO
IF OBJECT_ID('app24.ErrorCargaProducto', 'U') IS NULL
BEGIN
    CREATE TABLE app24.ErrorCargaProducto (
        id BIGINT IDENTITY(1,1) NOT NULL,
        carga_id BIGINT NOT NULL,
        hoja VARCHAR(80) NULL,
        fila INT NULL,
        columna VARCHAR(80) NULL,
        valor_enmascarado VARCHAR(80) NULL,
        codigo VARCHAR(120) NOT NULL,
        mensaje VARCHAR(500) NOT NULL,
        CONSTRAINT PK_ErrorCargaProducto PRIMARY KEY (id),
        CONSTRAINT FK_ErrorCargaProducto_Carga FOREIGN KEY (carga_id) REFERENCES app24.CargaCatalogoProducto(id)
    );
END;
GO

IF NOT EXISTS (SELECT 1 FROM sys.indexes WHERE name = 'IX_CargaCatalogoMaterialFila_Carga' AND object_id = OBJECT_ID('app24.CargaCatalogoMaterialFila'))
    CREATE INDEX IX_CargaCatalogoMaterialFila_Carga ON app24.CargaCatalogoMaterialFila(carga_id, fila);
IF NOT EXISTS (SELECT 1 FROM sys.indexes WHERE name = 'IX_ErrorCargaMaterial_Carga' AND object_id = OBJECT_ID('app24.ErrorCargaMaterial'))
    CREATE INDEX IX_ErrorCargaMaterial_Carga ON app24.ErrorCargaMaterial(carga_id, fila, id);
IF NOT EXISTS (SELECT 1 FROM sys.indexes WHERE name = 'IX_CargaCatalogoProductoFila_Carga' AND object_id = OBJECT_ID('app24.CargaCatalogoProductoFila'))
    CREATE INDEX IX_CargaCatalogoProductoFila_Carga ON app24.CargaCatalogoProductoFila(carga_id, fila);
IF NOT EXISTS (SELECT 1 FROM sys.indexes WHERE name = 'IX_ErrorCargaProducto_Carga' AND object_id = OBJECT_ID('app24.ErrorCargaProducto'))
    CREATE INDEX IX_ErrorCargaProducto_Carga ON app24.ErrorCargaProducto(carga_id, fila, id);
GO

IF NOT EXISTS (SELECT 1 FROM app24.Actividad WHERE clave = 'MATERIALES_CARGAR')
    INSERT INTO app24.Actividad (clave, nombre, recurso, accion) VALUES ('MATERIALES_CARGAR', 'Cargar materiales para previsualización', 'materiales', 'CARGAR');
IF NOT EXISTS (SELECT 1 FROM app24.Actividad WHERE clave = 'PRODUCTOS_CARGAR')
    INSERT INTO app24.Actividad (clave, nombre, recurso, accion) VALUES ('PRODUCTOS_CARGAR', 'Cargar productos para previsualización', 'productos', 'CARGAR');
INSERT INTO app24.PerfilActividad (perfil_id, actividad_id)
SELECT p.id, a.id FROM app24.PerfilApp p JOIN app24.Actividad a ON a.clave IN ('MATERIALES_CARGAR', 'PRODUCTOS_CARGAR')
WHERE p.nombre = 'ADMINISTRADOR' AND NOT EXISTS (SELECT 1 FROM app24.PerfilActividad pa WHERE pa.perfil_id = p.id AND pa.actividad_id = a.id);
GO

CREATE OR ALTER PROCEDURE app24.APP24_Q_CATALOGO_MATERIAL_CARGA_POR_HASH @Hash VARCHAR(64), @Existe BIT OUTPUT AS
BEGIN SET NOCOUNT ON; SELECT @Existe = CASE WHEN EXISTS (SELECT 1 FROM app24.CargaCatalogoMaterial WHERE hash = @Hash) THEN 1 ELSE 0 END; END;
GO
CREATE OR ALTER PROCEDURE app24.APP24_Q_CATALOGO_PRODUCTO_CARGA_POR_HASH @Hash VARCHAR(64), @Existe BIT OUTPUT AS
BEGIN SET NOCOUNT ON; SELECT @Existe = CASE WHEN EXISTS (SELECT 1 FROM app24.CargaCatalogoProducto WHERE hash = @Hash) THEN 1 ELSE 0 END; END;
GO

CREATE OR ALTER PROCEDURE app24.APP24_C_CATALOGO_MATERIAL_CARGA_CREAR
    @Archivo VARCHAR(255), @Hash VARCHAR(64), @UsuarioId BIGINT, @Estado VARCHAR(20), @TotalFilas INT,
    @FilasValidas INT, @VersionContrato VARCHAR(60), @CorrelationId VARCHAR(40), @FilasJson NVARCHAR(MAX),
    @ErroresJson NVARCHAR(MAX), @CargaId BIGINT OUTPUT
AS
BEGIN
    SET NOCOUNT ON; SET XACT_ABORT ON;
    IF @Archivo IS NULL OR @Hash IS NULL OR LEN(@Hash) <> 64 OR @UsuarioId IS NULL OR @Estado NOT IN ('PREVISUALIZADA','CON_ERRORES')
       OR @TotalFilas < 0 OR @FilasValidas < 0 OR @FilasValidas > @TotalFilas OR @VersionContrato IS NULL OR @CorrelationId IS NULL
       OR ISJSON(@FilasJson) <> 1 OR ISJSON(@ErroresJson) <> 1 THROW 51301, 'PARAMETRO_INVALIDO', 1;
    BEGIN TRY
        BEGIN TRANSACTION;
        INSERT INTO app24.CargaCatalogoMaterial(archivo,hash,usuario_id,estado,total_filas,filas_validas,filas_invalidas,version_contrato,correlation_id)
        VALUES(@Archivo,@Hash,@UsuarioId,@Estado,@TotalFilas,@FilasValidas,@TotalFilas-@FilasValidas,@VersionContrato,@CorrelationId);
        SET @CargaId = CONVERT(BIGINT,SCOPE_IDENTITY());
        INSERT INTO app24.CargaCatalogoMaterialFila(carga_id,hoja,fila,datos_json)
        SELECT @CargaId,hoja,fila,datos FROM OPENJSON(@FilasJson) WITH(hoja VARCHAR(80) '$.hoja',fila INT '$.fila',datos NVARCHAR(MAX) '$.datos' AS JSON);
        INSERT INTO app24.ErrorCargaMaterial(carga_id,hoja,fila,columna,valor_enmascarado,codigo,mensaje)
        SELECT @CargaId,hoja,fila,columna,valor_enmascarado,codigo,mensaje FROM OPENJSON(@ErroresJson)
        WITH(hoja VARCHAR(80) '$.hoja',fila INT '$.fila',columna VARCHAR(80) '$.columna',valor_enmascarado VARCHAR(80) '$.valorEnmascarado',codigo VARCHAR(120) '$.codigo',mensaje VARCHAR(500) '$.mensaje');
        DECLARE @Accion VARCHAR(40) = CASE WHEN @Estado='PREVISUALIZADA' THEN 'CARGA_VALIDADA' ELSE 'CARGA_CON_ERRORES' END,
                @Resultado VARCHAR(20) = CASE WHEN @Estado='PREVISUALIZADA' THEN 'EXITO' ELSE 'FALLO' END,
                @Detalle VARCHAR(500) = CONCAT('materiales carga=',@CargaId), @EventoId BIGINT;
        EXEC app24.APP24_C_BITACORA_REGISTRAR @UsuarioId=@UsuarioId,@Modulo='CATALOGOS',@Accion=@Accion,@Detalle=@Detalle,@CorrelacionId=@CorrelationId,@Resultado=@Resultado,@EventoId=@EventoId OUTPUT;
        COMMIT TRANSACTION;
    END TRY BEGIN CATCH IF XACT_STATE() <> 0 ROLLBACK TRANSACTION; THROW; END CATCH;
END;
GO

CREATE OR ALTER PROCEDURE app24.APP24_C_CATALOGO_PRODUCTO_CARGA_CREAR
    @Archivo VARCHAR(255), @Hash VARCHAR(64), @UsuarioId BIGINT, @Estado VARCHAR(20), @TotalFilas INT,
    @FilasValidas INT, @VersionContrato VARCHAR(60), @CorrelationId VARCHAR(40), @FilasJson NVARCHAR(MAX),
    @ErroresJson NVARCHAR(MAX), @CargaId BIGINT OUTPUT
AS
BEGIN
    SET NOCOUNT ON; SET XACT_ABORT ON;
    IF @Archivo IS NULL OR @Hash IS NULL OR LEN(@Hash) <> 64 OR @UsuarioId IS NULL OR @Estado NOT IN ('PREVISUALIZADA','CON_ERRORES')
       OR @TotalFilas < 0 OR @FilasValidas < 0 OR @FilasValidas > @TotalFilas OR @VersionContrato IS NULL OR @CorrelationId IS NULL
       OR ISJSON(@FilasJson) <> 1 OR ISJSON(@ErroresJson) <> 1 THROW 51302, 'PARAMETRO_INVALIDO', 1;
    BEGIN TRY
        BEGIN TRANSACTION;
        INSERT INTO app24.CargaCatalogoProducto(archivo,hash,usuario_id,estado,total_filas,filas_validas,filas_invalidas,version_contrato,correlation_id)
        VALUES(@Archivo,@Hash,@UsuarioId,@Estado,@TotalFilas,@FilasValidas,@TotalFilas-@FilasValidas,@VersionContrato,@CorrelationId);
        SET @CargaId = CONVERT(BIGINT,SCOPE_IDENTITY());
        INSERT INTO app24.CargaCatalogoProductoFila(carga_id,hoja,fila,datos_json)
        SELECT @CargaId,hoja,fila,datos FROM OPENJSON(@FilasJson) WITH(hoja VARCHAR(80) '$.hoja',fila INT '$.fila',datos NVARCHAR(MAX) '$.datos' AS JSON);
        INSERT INTO app24.ErrorCargaProducto(carga_id,hoja,fila,columna,valor_enmascarado,codigo,mensaje)
        SELECT @CargaId,hoja,fila,columna,valor_enmascarado,codigo,mensaje FROM OPENJSON(@ErroresJson)
        WITH(hoja VARCHAR(80) '$.hoja',fila INT '$.fila',columna VARCHAR(80) '$.columna',valor_enmascarado VARCHAR(80) '$.valorEnmascarado',codigo VARCHAR(120) '$.codigo',mensaje VARCHAR(500) '$.mensaje');
        DECLARE @Accion VARCHAR(40) = CASE WHEN @Estado='PREVISUALIZADA' THEN 'CARGA_VALIDADA' ELSE 'CARGA_CON_ERRORES' END,
                @Resultado VARCHAR(20) = CASE WHEN @Estado='PREVISUALIZADA' THEN 'EXITO' ELSE 'FALLO' END,
                @Detalle VARCHAR(500) = CONCAT('productos carga=',@CargaId), @EventoId BIGINT;
        EXEC app24.APP24_C_BITACORA_REGISTRAR @UsuarioId=@UsuarioId,@Modulo='CATALOGOS',@Accion=@Accion,@Detalle=@Detalle,@CorrelacionId=@CorrelationId,@Resultado=@Resultado,@EventoId=@EventoId OUTPUT;
        COMMIT TRANSACTION;
    END TRY BEGIN CATCH IF XACT_STATE() <> 0 ROLLBACK TRANSACTION; THROW; END CATCH;
END;
GO

CREATE OR ALTER PROCEDURE app24.APP24_Q_CATALOGO_MATERIAL_CARGA_OBTENER @CargaId BIGINT, @Pagina INT=1, @Tamano INT=100 AS
BEGIN
    SET NOCOUNT ON; IF @CargaId IS NULL OR @CargaId < 1 OR @Pagina < 1 OR @Tamano < 1 OR @Tamano > 100 THROW 51303, 'PARAMETRO_INVALIDO', 1;
    SELECT id,archivo,hash,estado,total_filas,filas_validas,filas_invalidas,version_contrato,correlation_id FROM app24.CargaCatalogoMaterial WHERE id=@CargaId;
    SELECT hoja,fila,datos_json FROM app24.CargaCatalogoMaterialFila WHERE carga_id=@CargaId ORDER BY fila,id OFFSET (CONVERT(BIGINT,@Pagina)-1)*CONVERT(BIGINT,@Tamano) ROWS FETCH NEXT @Tamano ROWS ONLY;
    SELECT COUNT_BIG(1) AS total_filas FROM app24.CargaCatalogoMaterialFila WHERE carga_id=@CargaId;
    SELECT hoja,fila,columna,valor_enmascarado,codigo,mensaje FROM app24.ErrorCargaMaterial WHERE carga_id=@CargaId ORDER BY ISNULL(fila,0),id;
END;
GO
CREATE OR ALTER PROCEDURE app24.APP24_Q_CATALOGO_PRODUCTO_CARGA_OBTENER @CargaId BIGINT, @Pagina INT=1, @Tamano INT=100 AS
BEGIN
    SET NOCOUNT ON; IF @CargaId IS NULL OR @CargaId < 1 OR @Pagina < 1 OR @Tamano < 1 OR @Tamano > 100 THROW 51304, 'PARAMETRO_INVALIDO', 1;
    SELECT id,archivo,hash,estado,total_filas,filas_validas,filas_invalidas,version_contrato,correlation_id FROM app24.CargaCatalogoProducto WHERE id=@CargaId;
    SELECT hoja,fila,datos_json FROM app24.CargaCatalogoProductoFila WHERE carga_id=@CargaId ORDER BY fila,id OFFSET (CONVERT(BIGINT,@Pagina)-1)*CONVERT(BIGINT,@Tamano) ROWS FETCH NEXT @Tamano ROWS ONLY;
    SELECT COUNT_BIG(1) AS total_filas FROM app24.CargaCatalogoProductoFila WHERE carga_id=@CargaId;
    SELECT hoja,fila,columna,valor_enmascarado,codigo,mensaje FROM app24.ErrorCargaProducto WHERE carga_id=@CargaId ORDER BY ISNULL(fila,0),id;
END;
GO
CREATE OR ALTER PROCEDURE app24.APP24_Q_CATALOGO_MATERIAL_CARGA_ERRORES @CargaId BIGINT, @Pagina INT=1, @Tamano INT=100 AS
BEGIN
    SET NOCOUNT ON; IF @CargaId IS NULL OR @CargaId < 1 OR @Pagina < 1 OR @Tamano < 1 OR @Tamano > 100 THROW 51305, 'PARAMETRO_INVALIDO', 1;
    SELECT hoja,fila,columna,valor_enmascarado,codigo,mensaje FROM app24.ErrorCargaMaterial WHERE carga_id=@CargaId ORDER BY ISNULL(fila,0),id OFFSET (CONVERT(BIGINT,@Pagina)-1)*CONVERT(BIGINT,@Tamano) ROWS FETCH NEXT @Tamano ROWS ONLY;
END;
GO
CREATE OR ALTER PROCEDURE app24.APP24_Q_CATALOGO_PRODUCTO_CARGA_ERRORES @CargaId BIGINT, @Pagina INT=1, @Tamano INT=100 AS
BEGIN
    SET NOCOUNT ON; IF @CargaId IS NULL OR @CargaId < 1 OR @Pagina < 1 OR @Tamano < 1 OR @Tamano > 100 THROW 51306, 'PARAMETRO_INVALIDO', 1;
    SELECT hoja,fila,columna,valor_enmascarado,codigo,mensaje FROM app24.ErrorCargaProducto WHERE carga_id=@CargaId ORDER BY ISNULL(fila,0),id OFFSET (CONVERT(BIGINT,@Pagina)-1)*CONVERT(BIGINT,@Tamano) ROWS FETCH NEXT @Tamano ROWS ONLY;
END;
GO
GRANT EXECUTE ON OBJECT::app24.APP24_Q_CATALOGO_MATERIAL_CARGA_POR_HASH TO app24_runtime;
GRANT EXECUTE ON OBJECT::app24.APP24_C_CATALOGO_MATERIAL_CARGA_CREAR TO app24_runtime;
GRANT EXECUTE ON OBJECT::app24.APP24_Q_CATALOGO_MATERIAL_CARGA_OBTENER TO app24_runtime;
GRANT EXECUTE ON OBJECT::app24.APP24_Q_CATALOGO_MATERIAL_CARGA_ERRORES TO app24_runtime;
GRANT EXECUTE ON OBJECT::app24.APP24_Q_CATALOGO_PRODUCTO_CARGA_POR_HASH TO app24_runtime;
GRANT EXECUTE ON OBJECT::app24.APP24_C_CATALOGO_PRODUCTO_CARGA_CREAR TO app24_runtime;
GRANT EXECUTE ON OBJECT::app24.APP24_Q_CATALOGO_PRODUCTO_CARGA_OBTENER TO app24_runtime;
GRANT EXECUTE ON OBJECT::app24.APP24_Q_CATALOGO_PRODUCTO_CARGA_ERRORES TO app24_runtime;
GO
