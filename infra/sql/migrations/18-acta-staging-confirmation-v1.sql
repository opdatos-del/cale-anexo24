-- Habilita la confirmacion autoritativa de cargas de actas reutilizando el SP
-- legacy dbo.CARGAACTAS (SP_EXISTING_REUSABLE, READ-ONLY 2026-10-05).
-- Crea staging app24.CargaActa* y los SP app24.APP24_*_ACTA_*.
-- dbo.CARGAACTAS no tiene error stage (dbo.ERRORACTA no existe): app24.ErrorCargaActa
-- es solo para errores del parser/estructura moderno, nunca validaciones legacy.
USE ANEXO24_DEV;
GO

IF OBJECT_ID('app24.CargaActa', 'U') IS NULL
BEGIN
    CREATE TABLE app24.CargaActa (
        id                BIGINT IDENTITY(1,1) NOT NULL,
        archivo           VARCHAR(255) NOT NULL,
        hash              VARCHAR(64) NOT NULL,
        fecha             DATETIME2(3) NOT NULL CONSTRAINT DF_CargaActa_fecha DEFAULT SYSUTCDATETIME(),
        usuario_id        BIGINT NOT NULL,
        estado            VARCHAR(20) NOT NULL,
        total_filas       INT NOT NULL DEFAULT 0,
        filas_validas     INT NOT NULL DEFAULT 0,
        filas_invalidas   INT NOT NULL DEFAULT 0,
        version_contrato  VARCHAR(60) NOT NULL,
        correlation_id    VARCHAR(40) NOT NULL,
        CONSTRAINT PK_CargaActa PRIMARY KEY (id),
        CONSTRAINT UQ_CargaActa_hash UNIQUE (hash),
        CONSTRAINT FK_CargaActa_Usuario FOREIGN KEY (usuario_id) REFERENCES app24.UsuarioApp (id)
    );
END;
GO

IF OBJECT_ID('app24.CargaActaFila', 'U') IS NULL
BEGIN
    CREATE TABLE app24.CargaActaFila (
        id BIGINT IDENTITY(1,1) NOT NULL,
        carga_id BIGINT NOT NULL,
        hoja VARCHAR(80) NOT NULL,
        fila INT NOT NULL,
        datos_json NVARCHAR(MAX) NOT NULL,
        CONSTRAINT PK_CargaActaFila PRIMARY KEY (id),
        CONSTRAINT FK_CargaActaFila_Carga FOREIGN KEY (carga_id) REFERENCES app24.CargaActa(id),
        CONSTRAINT UQ_CargaActaFila UNIQUE (carga_id, hoja, fila),
        CONSTRAINT CK_CargaActaFila_JSON CHECK (ISJSON(datos_json) = 1)
    );
END;
GO

IF OBJECT_ID('app24.ErrorCargaActa', 'U') IS NULL
BEGIN
    CREATE TABLE app24.ErrorCargaActa (
        id BIGINT IDENTITY(1,1) NOT NULL,
        carga_id BIGINT NOT NULL,
        hoja VARCHAR(80) NULL,
        fila INT NULL,
        columna VARCHAR(80) NULL,
        valor_enmascarado VARCHAR(80) NULL,
        codigo VARCHAR(120) NOT NULL,
        mensaje VARCHAR(500) NOT NULL,
        CONSTRAINT PK_ErrorCargaActa PRIMARY KEY (id),
        CONSTRAINT FK_ErrorCargaActa_Carga FOREIGN KEY (carga_id) REFERENCES app24.CargaActa(id)
    );
END;
GO

IF NOT EXISTS (SELECT 1 FROM sys.indexes WHERE name = 'IX_CargaActaFila_Carga' AND object_id = OBJECT_ID('app24.CargaActaFila'))
    CREATE INDEX IX_CargaActaFila_Carga ON app24.CargaActaFila(carga_id, fila);
IF NOT EXISTS (SELECT 1 FROM sys.indexes WHERE name = 'IX_ErrorCargaActa_Carga' AND object_id = OBJECT_ID('app24.ErrorCargaActa'))
    CREATE INDEX IX_ErrorCargaActa_Carga ON app24.ErrorCargaActa(carga_id, fila, id);
GO

DECLARE @RestriccionEstado SYSNAME = (
    SELECT TOP (1) name FROM sys.check_constraints
    WHERE parent_object_id = OBJECT_ID('app24.CargaActa') AND definition LIKE '%estado%'
    ORDER BY name
);
IF @RestriccionEstado IS NOT NULL
   AND NOT EXISTS (SELECT 1 FROM sys.check_constraints
       WHERE parent_object_id = OBJECT_ID('app24.CargaActa') AND name = @RestriccionEstado AND definition LIKE '%CONFIRMADA%')
BEGIN
    DECLARE @SqlDrop NVARCHAR(500) = N'ALTER TABLE app24.CargaActa DROP CONSTRAINT ' + QUOTENAME(@RestriccionEstado);
    EXEC sp_executesql @SqlDrop;
    SET @RestriccionEstado = NULL;
END;
IF @RestriccionEstado IS NULL
    ALTER TABLE app24.CargaActa WITH CHECK
        ADD CONSTRAINT CK_CargaActa_estado CHECK (estado IN ('PREVISUALIZADA', 'CON_ERRORES', 'CONFIRMADA'));
GO

IF COL_LENGTH('app24.CargaActa', 'fecha_confirmacion') IS NULL
    ALTER TABLE app24.CargaActa ADD fecha_confirmacion DATETIME2(3) NULL;
GO

IF NOT EXISTS (SELECT 1 FROM app24.Actividad WHERE clave = 'ACTAS_CONFIRMAR')
    INSERT INTO app24.Actividad (clave, nombre, recurso, accion)
    VALUES ('ACTAS_CONFIRMAR', 'Confirmar actas', 'actas', 'CONFIRMAR');
IF NOT EXISTS (SELECT 1 FROM app24.Actividad WHERE clave = 'ACTAS_CARGAR')
    INSERT INTO app24.Actividad (clave, nombre, recurso, accion)
    VALUES ('ACTAS_CARGAR', 'Cargar actas para previsualizacion', 'actas', 'CARGAR');
INSERT INTO app24.PerfilActividad (perfil_id, actividad_id)
SELECT p.id, a.id
FROM app24.PerfilApp p
JOIN app24.Actividad a ON a.clave IN ('ACTAS_CONFIRMAR', 'ACTAS_CARGAR')
WHERE p.nombre = 'ADMINISTRADOR'
  AND NOT EXISTS (SELECT 1 FROM app24.PerfilActividad pa WHERE pa.perfil_id = p.id AND pa.actividad_id = a.id);
GO

CREATE OR ALTER PROCEDURE app24.APP24_Q_ACTA_CARGA_POR_HASH @Hash VARCHAR(64), @Existe BIT OUTPUT AS
BEGIN SET NOCOUNT ON; SELECT @Existe = CASE WHEN EXISTS (SELECT 1 FROM app24.CargaActa WHERE hash = @Hash) THEN 1 ELSE 0 END; END;
GO

CREATE OR ALTER PROCEDURE app24.APP24_C_ACTA_CARGA_CREAR
    @Archivo VARCHAR(255), @Hash VARCHAR(64), @UsuarioId BIGINT, @Estado VARCHAR(20), @TotalFilas INT,
    @FilasValidas INT, @VersionContrato VARCHAR(60), @CorrelationId VARCHAR(40), @FilasJson NVARCHAR(MAX),
    @ErroresJson NVARCHAR(MAX), @CargaId BIGINT OUTPUT
AS
BEGIN
    SET NOCOUNT ON; SET XACT_ABORT ON;
    IF @Archivo IS NULL OR @Hash IS NULL OR LEN(@Hash) <> 64 OR @UsuarioId IS NULL OR @Estado NOT IN ('PREVISUALIZADA','CON_ERRORES')
       OR @TotalFilas < 0 OR @FilasValidas < 0 OR @FilasValidas > @TotalFilas OR @VersionContrato IS NULL OR @CorrelationId IS NULL
       OR ISJSON(@FilasJson) <> 1 OR ISJSON(@ErroresJson) <> 1 THROW 51330, 'PARAMETRO_INVALIDO', 1;
    BEGIN TRY
        BEGIN TRANSACTION;
        INSERT INTO app24.CargaActa(archivo,hash,usuario_id,estado,total_filas,filas_validas,filas_invalidas,version_contrato,correlation_id)
        VALUES(@Archivo,@Hash,@UsuarioId,@Estado,@TotalFilas,@FilasValidas,@TotalFilas-@FilasValidas,@VersionContrato,@CorrelationId);
        SET @CargaId = CONVERT(BIGINT,SCOPE_IDENTITY());
        INSERT INTO app24.CargaActaFila(carga_id,hoja,fila,datos_json)
        SELECT @CargaId,hoja,fila,datos FROM OPENJSON(@FilasJson) WITH(hoja VARCHAR(80) '$.hoja',fila INT '$.fila',datos NVARCHAR(MAX) '$.datos' AS JSON);
        INSERT INTO app24.ErrorCargaActa(carga_id,hoja,fila,columna,valor_enmascarado,codigo,mensaje)
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

CREATE OR ALTER PROCEDURE app24.APP24_Q_ACTA_CARGA_OBTENER @CargaId BIGINT, @Pagina INT=1, @Tamano INT=100 AS
BEGIN
    SET NOCOUNT ON; IF @CargaId IS NULL OR @CargaId < 1 OR @Pagina < 1 OR @Tamano < 1 OR @Tamano > 100 THROW 51331, 'PARAMETRO_INVALIDO', 1;
    SELECT id,archivo,hash,estado,total_filas,filas_validas,filas_invalidas,version_contrato,correlation_id FROM app24.CargaActa WHERE id=@CargaId;
    SELECT hoja,fila,datos_json FROM app24.CargaActaFila WHERE carga_id=@CargaId ORDER BY fila,id OFFSET (CONVERT(BIGINT,@Pagina)-1)*CONVERT(BIGINT,@Tamano) ROWS FETCH NEXT @Tamano ROWS ONLY;
    SELECT COUNT_BIG(1) AS total_filas FROM app24.CargaActaFila WHERE carga_id=@CargaId;
    SELECT hoja,fila,columna,valor_enmascarado,codigo,mensaje FROM app24.ErrorCargaActa WHERE carga_id=@CargaId ORDER BY ISNULL(fila,0),id;
END;
GO

CREATE OR ALTER PROCEDURE app24.APP24_Q_ACTA_CARGA_ERRORES @CargaId BIGINT, @Pagina INT=1, @Tamano INT=100 AS
BEGIN
    SET NOCOUNT ON; IF @CargaId IS NULL OR @CargaId < 1 OR @Pagina < 1 OR @Tamano < 1 OR @Tamano > 100 THROW 51332, 'PARAMETRO_INVALIDO', 1;
    SELECT hoja,fila,columna,valor_enmascarado,codigo,mensaje FROM app24.ErrorCargaActa WHERE carga_id=@CargaId ORDER BY ISNULL(fila,0),id OFFSET (CONVERT(BIGINT,@Pagina)-1)*CONVERT(BIGINT,@Tamano) ROWS FETCH NEXT @Tamano ROWS ONLY;
END;
GO


PRINT 'Migration 18-acta-staging-confirmation-v1 aplicada.';
GO
