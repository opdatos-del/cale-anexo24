-- Habilita la confirmacion autoritativa de cargas de constancias reutilizando el SP
-- legacy dbo.CARGACONSTANCIAS (SP_EXISTING_REUSABLE, READ-ONLY 2026-10-06).
-- Crea staging app24.CargaConstancia* y los SP app24.APP24_*_CONSTANCIA_*.
-- dbo.CARGACONSTANCIAS no tiene error stage propio: usa dbo.errorcarga (compartida con
-- CARGAPEDIMENTOS). app24.ErrorCargaConstancia es solo para la normalizacion del wrapper
-- y errores del parser moderno, nunca validaciones legacy inventadas.
USE ANEXO24_DEV;
GO

IF OBJECT_ID('app24.CargaConstancia', 'U') IS NULL
BEGIN
    CREATE TABLE app24.CargaConstancia (
        id                BIGINT IDENTITY(1,1) NOT NULL,
        archivo           VARCHAR(255) NOT NULL,
        hash              VARCHAR(64) NOT NULL,
        fecha             DATETIME2(3) NOT NULL CONSTRAINT DF_CargaConstancia_fecha DEFAULT SYSUTCDATETIME(),
        usuario_id        BIGINT NOT NULL,
        estado            VARCHAR(20) NOT NULL,
        total_filas       INT NOT NULL DEFAULT 0,
        filas_validas     INT NOT NULL DEFAULT 0,
        filas_invalidas   INT NOT NULL DEFAULT 0,
        version_contrato  VARCHAR(60) NOT NULL,
        correlation_id    VARCHAR(40) NOT NULL,
        CONSTRAINT PK_CargaConstancia PRIMARY KEY (id),
        CONSTRAINT UQ_CargaConstancia_hash UNIQUE (hash),
        CONSTRAINT FK_CargaConstancia_Usuario FOREIGN KEY (usuario_id) REFERENCES app24.UsuarioApp (id)
    );
END;
GO

IF OBJECT_ID('app24.CargaConstanciaFila', 'U') IS NULL
BEGIN
    CREATE TABLE app24.CargaConstanciaFila (
        id BIGINT IDENTITY(1,1) NOT NULL,
        carga_id BIGINT NOT NULL,
        hoja VARCHAR(80) NOT NULL,
        fila INT NOT NULL,
        datos_json NVARCHAR(MAX) NOT NULL,
        CONSTRAINT PK_CargaConstanciaFila PRIMARY KEY (id),
        CONSTRAINT FK_CargaConstanciaFila_Carga FOREIGN KEY (carga_id) REFERENCES app24.CargaConstancia(id),
        CONSTRAINT UQ_CargaConstanciaFila UNIQUE (carga_id, hoja, fila),
        CONSTRAINT CK_CargaConstanciaFila_JSON CHECK (ISJSON(datos_json) = 1)
    );
END;
GO

IF OBJECT_ID('app24.ErrorCargaConstancia', 'U') IS NULL
BEGIN
    CREATE TABLE app24.ErrorCargaConstancia (
        id BIGINT IDENTITY(1,1) NOT NULL,
        carga_id BIGINT NOT NULL,
        hoja VARCHAR(80) NULL,
        fila INT NULL,
        columna VARCHAR(80) NULL,
        valor_enmascarado VARCHAR(80) NULL,
        codigo VARCHAR(120) NOT NULL,
        mensaje VARCHAR(500) NOT NULL,
        CONSTRAINT PK_ErrorCargaConstancia PRIMARY KEY (id),
        CONSTRAINT FK_ErrorCargaConstancia_Carga FOREIGN KEY (carga_id) REFERENCES app24.CargaConstancia(id)
    );
END;
GO

IF NOT EXISTS (SELECT 1 FROM sys.indexes WHERE name = 'IX_CargaConstanciaFila_Carga' AND object_id = OBJECT_ID('app24.CargaConstanciaFila'))
    CREATE INDEX IX_CargaConstanciaFila_Carga ON app24.CargaConstanciaFila(carga_id, fila);
IF NOT EXISTS (SELECT 1 FROM sys.indexes WHERE name = 'IX_ErrorCargaConstancia_Carga' AND object_id = OBJECT_ID('app24.ErrorCargaConstancia'))
    CREATE INDEX IX_ErrorCargaConstancia_Carga ON app24.ErrorCargaConstancia(carga_id, fila, id);
GO

DECLARE @RestriccionEstado SYSNAME = (
    SELECT TOP (1) name FROM sys.check_constraints
    WHERE parent_object_id = OBJECT_ID('app24.CargaConstancia') AND definition LIKE '%estado%'
    ORDER BY name
);
IF @RestriccionEstado IS NOT NULL
   AND NOT EXISTS (SELECT 1 FROM sys.check_constraints
       WHERE parent_object_id = OBJECT_ID('app24.CargaConstancia') AND name = @RestriccionEstado AND definition LIKE '%CONFIRMADA%')
BEGIN
    DECLARE @SqlDrop NVARCHAR(500) = N'ALTER TABLE app24.CargaConstancia DROP CONSTRAINT ' + QUOTENAME(@RestriccionEstado);
    EXEC sp_executesql @SqlDrop;
    SET @RestriccionEstado = NULL;
END;
IF @RestriccionEstado IS NULL
    ALTER TABLE app24.CargaConstancia WITH CHECK
        ADD CONSTRAINT CK_CargaConstancia_estado CHECK (estado IN ('PREVISUALIZADA', 'CON_ERRORES', 'CONFIRMADA'));
GO

IF COL_LENGTH('app24.CargaConstancia', 'fecha_confirmacion') IS NULL
    ALTER TABLE app24.CargaConstancia ADD fecha_confirmacion DATETIME2(3) NULL;
GO

IF NOT EXISTS (SELECT 1 FROM app24.Actividad WHERE clave = 'CONSTANCIAS_CONFIRMAR')
    INSERT INTO app24.Actividad (clave, nombre, recurso, accion)
    VALUES ('CONSTANCIAS_CONFIRMAR', 'Confirmar constancias', 'constancias', 'CONFIRMAR');
IF NOT EXISTS (SELECT 1 FROM app24.Actividad WHERE clave = 'CONSTANCIAS_CARGAR')
    INSERT INTO app24.Actividad (clave, nombre, recurso, accion)
    VALUES ('CONSTANCIAS_CARGAR', 'Cargar constancias para previsualizacion', 'constancias', 'CARGAR');
INSERT INTO app24.PerfilActividad (perfil_id, actividad_id)
SELECT p.id, a.id
FROM app24.PerfilApp p
JOIN app24.Actividad a ON a.clave IN ('CONSTANCIAS_CONFIRMAR', 'CONSTANCIAS_CARGAR')
WHERE p.nombre = 'ADMINISTRADOR'
  AND NOT EXISTS (SELECT 1 FROM app24.PerfilActividad pa WHERE pa.perfil_id = p.id AND pa.actividad_id = a.id);
GO

CREATE OR ALTER PROCEDURE app24.APP24_Q_CONSTANCIA_CARGA_POR_HASH @Hash VARCHAR(64), @Existe BIT OUTPUT AS
BEGIN SET NOCOUNT ON; SELECT @Existe = CASE WHEN EXISTS (SELECT 1 FROM app24.CargaConstancia WHERE hash = @Hash) THEN 1 ELSE 0 END; END;
GO

CREATE OR ALTER PROCEDURE app24.APP24_C_CONSTANCIA_CARGA_CREAR
    @Archivo VARCHAR(255), @Hash VARCHAR(64), @UsuarioId BIGINT, @Estado VARCHAR(20), @TotalFilas INT,
    @FilasValidas INT, @VersionContrato VARCHAR(60), @CorrelationId VARCHAR(40), @FilasJson NVARCHAR(MAX),
    @ErroresJson NVARCHAR(MAX), @CargaId BIGINT OUTPUT
AS
BEGIN
    SET NOCOUNT ON; SET XACT_ABORT ON;
    IF @Archivo IS NULL OR @Hash IS NULL OR LEN(@Hash) <> 64 OR @UsuarioId IS NULL OR @Estado NOT IN ('PREVISUALIZADA','CON_ERRORES')
       OR @TotalFilas < 0 OR @FilasValidas < 0 OR @FilasValidas > @TotalFilas OR @VersionContrato IS NULL OR @CorrelationId IS NULL
       OR ISJSON(@FilasJson) <> 1 OR ISJSON(@ErroresJson) <> 1 THROW 51340, 'PARAMETRO_INVALIDO', 1;
    BEGIN TRY
        BEGIN TRANSACTION;
        INSERT INTO app24.CargaConstancia(archivo,hash,usuario_id,estado,total_filas,filas_validas,filas_invalidas,version_contrato,correlation_id)
        VALUES(@Archivo,@Hash,@UsuarioId,@Estado,@TotalFilas,@FilasValidas,@TotalFilas-@FilasValidas,@VersionContrato,@CorrelationId);
        SET @CargaId = CONVERT(BIGINT,SCOPE_IDENTITY());
        INSERT INTO app24.CargaConstanciaFila(carga_id,hoja,fila,datos_json)
        SELECT @CargaId,hoja,fila,datos FROM OPENJSON(@FilasJson) WITH(hoja VARCHAR(80) '$.hoja',fila INT '$.fila',datos NVARCHAR(MAX) '$.datos' AS JSON);
        INSERT INTO app24.ErrorCargaConstancia(carga_id,hoja,fila,columna,valor_enmascarado,codigo,mensaje)
        SELECT @CargaId,hoja,fila,columna,valor_enmascarado,codigo,mensaje FROM OPENJSON(@ErroresJson)
        WITH(hoja VARCHAR(80) '$.hoja',fila INT '$.fila',columna VARCHAR(80) '$.columna',valor_enmascarado VARCHAR(80) '$.valorEnmascarado',codigo VARCHAR(120) '$.codigo',mensaje VARCHAR(500) '$.mensaje');
        DECLARE @Accion VARCHAR(40) = CASE WHEN @Estado='PREVISUALIZADA' THEN 'ACTA_CARGA_VALIDADA' ELSE 'ACTA_CARGA_CON_ERRORES' END,
                @Resultado VARCHAR(20) = CASE WHEN @Estado='PREVISUALIZADA' THEN 'EXITO' ELSE 'FALLO' END,
                @Detalle VARCHAR(500) = CONCAT('cargaId=',@CargaId), @EventoId BIGINT;
        EXEC app24.APP24_C_BITACORA_REGISTRAR @UsuarioId=@UsuarioId,@Modulo='OPERACIONES_ACTAS',@Accion=@Accion,@Detalle=@Detalle,@CorrelacionId=@CorrelationId,@Resultado=@Resultado,@EventoId=@EventoId OUTPUT;
        COMMIT TRANSACTION;
    END TRY BEGIN CATCH IF XACT_STATE() <> 0 ROLLBACK TRANSACTION; THROW; END CATCH;
END;
GO

CREATE OR ALTER PROCEDURE app24.APP24_Q_CONSTANCIA_CARGA_OBTENER @CargaId BIGINT, @Pagina INT=1, @Tamano INT=100 AS
BEGIN
    SET NOCOUNT ON; IF @CargaId IS NULL OR @CargaId < 1 OR @Pagina < 1 OR @Tamano < 1 OR @Tamano > 100 THROW 51341, 'PARAMETRO_INVALIDO', 1;
    SELECT id,archivo,hash,estado,total_filas,filas_validas,filas_invalidas,version_contrato,correlation_id FROM app24.CargaConstancia WHERE id=@CargaId;
    SELECT hoja,fila,datos_json FROM app24.CargaConstanciaFila WHERE carga_id=@CargaId ORDER BY fila,id OFFSET (CONVERT(BIGINT,@Pagina)-1)*CONVERT(BIGINT,@Tamano) ROWS FETCH NEXT @Tamano ROWS ONLY;
    SELECT COUNT_BIG(1) AS total_filas FROM app24.CargaConstanciaFila WHERE carga_id=@CargaId;
    SELECT hoja,fila,columna,valor_enmascarado,codigo,mensaje FROM app24.ErrorCargaConstancia WHERE carga_id=@CargaId ORDER BY ISNULL(fila,0),id;
END;
GO

CREATE OR ALTER PROCEDURE app24.APP24_Q_CONSTANCIA_CARGA_ERRORES @CargaId BIGINT, @Pagina INT=1, @Tamano INT=100 AS
BEGIN
    SET NOCOUNT ON; IF @CargaId IS NULL OR @CargaId < 1 OR @Pagina < 1 OR @Tamano < 1 OR @Tamano > 100 THROW 51342, 'PARAMETRO_INVALIDO', 1;
    SELECT hoja,fila,columna,valor_enmascarado,codigo,mensaje FROM app24.ErrorCargaConstancia WHERE carga_id=@CargaId ORDER BY ISNULL(fila,0),id OFFSET (CONVERT(BIGINT,@Pagina)-1)*CONVERT(BIGINT,@Tamano) ROWS FETCH NEXT @Tamano ROWS ONLY;
END;
GO


PRINT 'Migration 19-constancia-staging-confirmation-v1 aplicada.';
GO
