-- ANEXO24_DEV: staging aislado para preview de pedimentos V1.
-- No confirma operaciones en CALE_IMMEX ni ejecuta procedimientos legacy.
USE ANEXO24_DEV;
GO

IF OBJECT_ID('app24.CargaPedimento', 'U') IS NULL
BEGIN
    CREATE TABLE app24.CargaPedimento (
        id                  BIGINT IDENTITY(1,1) NOT NULL,
        archivo             VARCHAR(255) NOT NULL,
        hash                VARCHAR(64) NOT NULL,
        fecha               DATETIME2(3) NOT NULL CONSTRAINT DF_CargaPedimento_fecha DEFAULT SYSUTCDATETIME(),
        usuario_id          BIGINT NOT NULL,
        estado              VARCHAR(20) NOT NULL,
        total_filas         INT NOT NULL DEFAULT 0,
        filas_validas      INT NOT NULL DEFAULT 0,
        filas_invalidas    INT NOT NULL DEFAULT 0,
        version_plantilla  VARCHAR(40) NOT NULL,
        correlation_id     VARCHAR(40) NOT NULL,
        CONSTRAINT PK_CargaPedimento PRIMARY KEY (id),
        CONSTRAINT UQ_CargaPedimento_hash UNIQUE (hash),
        CONSTRAINT FK_CargaPedimento_Usuario FOREIGN KEY (usuario_id) REFERENCES app24.UsuarioApp (id),
        CONSTRAINT CK_CargaPedimento_estado CHECK (estado IN ('PREVISUALIZADA', 'CON_ERRORES'))
    );
END;
GO

IF OBJECT_ID('app24.CargaPedimentoFila', 'U') IS NULL
BEGIN
    CREATE TABLE app24.CargaPedimentoFila (
        id          BIGINT IDENTITY(1,1) NOT NULL,
        carga_id    BIGINT NOT NULL,
        hoja        VARCHAR(80) NOT NULL,
        fila        INT NOT NULL,
        datos_json  NVARCHAR(MAX) NOT NULL,
        CONSTRAINT PK_CargaPedimentoFila PRIMARY KEY (id),
        CONSTRAINT FK_CargaPedimentoFila_Carga FOREIGN KEY (carga_id) REFERENCES app24.CargaPedimento(id),
        CONSTRAINT UQ_CargaPedimentoFila_CargaFila UNIQUE (carga_id, hoja, fila),
        CONSTRAINT CK_CargaPedimentoFila_DatosJson CHECK (ISJSON(datos_json) = 1)
    );
END;
GO

IF OBJECT_ID('app24.ErrorCargaPedimento', 'U') IS NULL
BEGIN
    CREATE TABLE app24.ErrorCargaPedimento (
        id                  BIGINT IDENTITY(1,1) NOT NULL,
        carga_id            BIGINT NOT NULL,
        hoja                VARCHAR(80) NULL,
        fila                INT NULL,
        columna             VARCHAR(80) NULL,
        valor_enmascarado   VARCHAR(80) NULL,
        codigo              VARCHAR(120) NOT NULL,
        mensaje             VARCHAR(500) NOT NULL,
        CONSTRAINT PK_ErrorCargaPedimento PRIMARY KEY (id),
        CONSTRAINT FK_ErrorCargaPedimento_Carga FOREIGN KEY (carga_id) REFERENCES app24.CargaPedimento(id)
    );
END;
GO

IF NOT EXISTS (SELECT 1 FROM sys.indexes WHERE name = 'IX_CargaPedimentoFila_CargaFila'
               AND object_id = OBJECT_ID('app24.CargaPedimentoFila'))
    CREATE NONCLUSTERED INDEX IX_CargaPedimentoFila_CargaFila
        ON app24.CargaPedimentoFila (carga_id, fila);
GO

IF NOT EXISTS (SELECT 1 FROM sys.indexes WHERE name = 'IX_ErrorCargaPedimento_Carga'
               AND object_id = OBJECT_ID('app24.ErrorCargaPedimento'))
    CREATE NONCLUSTERED INDEX IX_ErrorCargaPedimento_Carga
        ON app24.ErrorCargaPedimento (carga_id, fila, id);
GO

IF NOT EXISTS (SELECT 1 FROM app24.Actividad WHERE clave = 'PEDIMENTOS_CARGAR')
    INSERT INTO app24.Actividad (clave, nombre, recurso, accion)
    VALUES ('PEDIMENTOS_CARGAR', 'Cargar pedimentos para previsualización', 'pedimentos', 'CARGAR');
GO

INSERT INTO app24.PerfilActividad (perfil_id, actividad_id)
SELECT p.id, a.id
FROM app24.PerfilApp p
JOIN app24.Actividad a ON a.clave = 'PEDIMENTOS_CARGAR'
WHERE p.nombre = 'ADMINISTRADOR'
  AND NOT EXISTS (
      SELECT 1 FROM app24.PerfilActividad pa
      WHERE pa.perfil_id = p.id AND pa.actividad_id = a.id
  );
GO

CREATE OR ALTER PROCEDURE app24.APP24_Q_PEDIMENTO_CARGA_POR_HASH
    @Hash VARCHAR(64),
    @Existe BIT OUTPUT
AS
BEGIN
    SET NOCOUNT ON;
    SELECT @Existe = CASE WHEN EXISTS (SELECT 1 FROM app24.CargaPedimento WHERE hash = @Hash) THEN 1 ELSE 0 END;
END;
GO

CREATE OR ALTER PROCEDURE app24.APP24_C_PEDIMENTO_CARGA_CREAR
    @Archivo VARCHAR(255),
    @Hash VARCHAR(64),
    @UsuarioId BIGINT,
    @Estado VARCHAR(20),
    @TotalFilas INT,
    @FilasValidas INT,
    @VersionPlantilla VARCHAR(40),
    @CorrelationId VARCHAR(40),
    @FilasJson NVARCHAR(MAX),
    @ErroresJson NVARCHAR(MAX),
    @CargaId BIGINT OUTPUT
AS
BEGIN
    SET NOCOUNT ON;
    SET XACT_ABORT ON;

    IF @Archivo IS NULL OR @Hash IS NULL OR LEN(@Hash) <> 64 OR @UsuarioId IS NULL
       OR @Estado NOT IN ('PREVISUALIZADA', 'CON_ERRORES')
       OR @TotalFilas < 0 OR @FilasValidas < 0 OR @FilasValidas > @TotalFilas
       OR @VersionPlantilla IS NULL OR @CorrelationId IS NULL
       OR ISJSON(@FilasJson) <> 1 OR ISJSON(@ErroresJson) <> 1
        THROW 51201, 'PARAMETRO_INVALIDO', 1;

    BEGIN TRY
        BEGIN TRANSACTION;
        INSERT INTO app24.CargaPedimento (archivo, hash, usuario_id, estado, total_filas,
                                           filas_validas, filas_invalidas, version_plantilla, correlation_id)
        VALUES (@Archivo, @Hash, @UsuarioId, @Estado, @TotalFilas, @FilasValidas,
                @TotalFilas - @FilasValidas, @VersionPlantilla, @CorrelationId);
        SET @CargaId = CONVERT(BIGINT, SCOPE_IDENTITY());

        INSERT INTO app24.CargaPedimentoFila (carga_id, hoja, fila, datos_json)
        SELECT @CargaId, hoja, fila, datos_json
        FROM OPENJSON(@FilasJson)
        WITH (hoja VARCHAR(80) '$.hoja', fila INT '$.fila', datos_json NVARCHAR(MAX) '$.datos' AS JSON);

        INSERT INTO app24.ErrorCargaPedimento (carga_id, hoja, fila, columna, valor_enmascarado, codigo, mensaje)
        SELECT @CargaId, hoja, fila, columna, valor_enmascarado, codigo, mensaje
        FROM OPENJSON(@ErroresJson)
        WITH (hoja VARCHAR(80) '$.hoja', fila INT '$.fila', columna VARCHAR(80) '$.columna',
              valor_enmascarado VARCHAR(80) '$.valorEnmascarado', codigo VARCHAR(120) '$.codigo',
              mensaje VARCHAR(500) '$.mensaje');

        DECLARE @Accion VARCHAR(40) = CASE WHEN @Estado = 'PREVISUALIZADA'
                                           THEN 'CARGA_PEDIMENTO_VALIDADA'
                                           ELSE 'CARGA_PEDIMENTO_CON_ERRORES' END;
        DECLARE @Resultado VARCHAR(20) = CASE WHEN @Estado = 'PREVISUALIZADA' THEN 'EXITO' ELSE 'FALLO' END;
        DECLARE @Detalle VARCHAR(500) = CONCAT('cargaId=', @CargaId, ';total=', @TotalFilas,
            ';validas=', @FilasValidas, ';invalidas=', @TotalFilas - @FilasValidas);
        DECLARE @EventoId BIGINT;
        EXEC app24.APP24_C_BITACORA_REGISTRAR
            @UsuarioId = @UsuarioId, @Modulo = 'OPERACIONES', @Accion = @Accion,
            @Detalle = @Detalle, @CorrelacionId = @CorrelationId,
            @Resultado = @Resultado, @EventoId = @EventoId OUTPUT;
        COMMIT TRANSACTION;
    END TRY
    BEGIN CATCH
        IF XACT_STATE() <> 0 ROLLBACK TRANSACTION;
        THROW;
    END CATCH;
END;
GO

CREATE OR ALTER PROCEDURE app24.APP24_Q_PEDIMENTO_CARGA_OBTENER
    @CargaId BIGINT,
    @Pagina INT = 1,
    @Tamano INT = 100
AS
BEGIN
    SET NOCOUNT ON;
    IF @CargaId IS NULL OR @CargaId < 1 OR @Pagina < 1 OR @Tamano < 1 OR @Tamano > 100
        THROW 51202, 'PARAMETRO_INVALIDO', 1;

    SELECT id, archivo, hash, estado, total_filas, filas_validas, filas_invalidas,
           version_plantilla, correlation_id
    FROM app24.CargaPedimento WHERE id = @CargaId;

    SELECT hoja, fila, datos_json
    FROM app24.CargaPedimentoFila
    WHERE carga_id = @CargaId
    ORDER BY fila, id
    OFFSET (CONVERT(BIGINT, @Pagina) - 1) * CONVERT(BIGINT, @Tamano) ROWS
    FETCH NEXT @Tamano ROWS ONLY;

    SELECT COUNT_BIG(1) AS total_filas
    FROM app24.CargaPedimentoFila WHERE carga_id = @CargaId;

    SELECT hoja, fila, columna, valor_enmascarado, codigo, mensaje
    FROM app24.ErrorCargaPedimento
    WHERE carga_id = @CargaId
    ORDER BY ISNULL(fila, 0), id;
END;
GO

CREATE OR ALTER PROCEDURE app24.APP24_Q_PEDIMENTO_CARGA_ERRORES
    @CargaId BIGINT,
    @Pagina INT = 1,
    @Tamano INT = 100
AS
BEGIN
    SET NOCOUNT ON;
    IF @CargaId IS NULL OR @CargaId < 1 OR @Pagina < 1 OR @Tamano < 1 OR @Tamano > 100
        THROW 51203, 'PARAMETRO_INVALIDO', 1;

    SELECT hoja, fila, columna, valor_enmascarado, codigo, mensaje
    FROM app24.ErrorCargaPedimento
    WHERE carga_id = @CargaId
    ORDER BY ISNULL(fila, 0), id
    OFFSET (CONVERT(BIGINT, @Pagina) - 1) * CONVERT(BIGINT, @Tamano) ROWS
    FETCH NEXT @Tamano ROWS ONLY;
END;
GO

GRANT EXECUTE ON OBJECT::app24.APP24_Q_PEDIMENTO_CARGA_POR_HASH TO app24_runtime;
GRANT EXECUTE ON OBJECT::app24.APP24_C_PEDIMENTO_CARGA_CREAR TO app24_runtime;
GRANT EXECUTE ON OBJECT::app24.APP24_Q_PEDIMENTO_CARGA_OBTENER TO app24_runtime;
GRANT EXECUTE ON OBJECT::app24.APP24_Q_PEDIMENTO_CARGA_ERRORES TO app24_runtime;
GO
