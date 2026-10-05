-- Habilita la confirmación autoritativa de cargas de submaquila (constancias de
-- transferencia) reutilizando el SP legacy dbo.CARGA_SUBMAQUILA.
--
--   * crea el staging real app24.CargaSubmaquila*;
--   * agrega estado CONFIRMADA + fecha_confirmacion a CargaSubmaquila;
--   * instala el SP de autorización app24.APP24_Q_SUBMAQUILA_CARGA_POR_HASH;
--   * instala el command de staging app24.APP24_C_SUBMAQUILA_CARGA_CREAR;
--   * instala los queries de lectura y errores;
--   * otorga EXECUTE al rol app24_runtime cuando existe;
--   * crea las actividades SUBMAQUILA_CARGAR/SUBMAQUILA_CONFIRMAR y las asigna
--     al perfil ADMINISTRADOR.
--
-- La operación autoritativa corre en CALE_IMMEX.dbo.APP24_C_SUBMAQUILA_CARGA_CONFIRMAR,
-- que orquesta el stage legacy dbo.TMPSUBMAQUILA y delega la regla de negocio
-- (inserción de SALIDAS y PSALIDAS) en dbo.CARGA_SUBMAQUILA.
--
-- Auditoría READ ONLY de CALE_IMMEX 2026-10-05 (compat level 100):
--   dbo.TMPSUBMAQUILA: TMPSKEY BIGINT IDENTITY PK, FOLIO VARCHAR(50), FECHA DATE,
--     SUBMAQUILADOR VARCHAR(50), CLAVE VARCHAR(50), CANTIDAD NUMERIC(18,4),
--     UNIDAD VARCHAR(5), DESCRIPCION VARCHAR(250), LINEA INT.
--   dbo.CARGA_SUBMAQUILA no recibe parámetros, no valida y no usa error stage;
--   inserta SALIDAS (TIPO_OPERACION='SUBMAQUILA', CVE_PEDIMENTO='SUB') agrupadas
--   por FOLIO/FECHA/SUBMAQUILADOR y PSALIDAS con fraccion resuelta desde
--   dbo.PRODUCTOS y SALIDALINK resuelto por DOCUMENTO.
USE ANEXO24_DEV;
GO

IF OBJECT_ID('app24.CargaSubmaquila', 'U') IS NULL
BEGIN
    CREATE TABLE app24.CargaSubmaquila (
        id                BIGINT IDENTITY(1,1) NOT NULL,
        archivo           VARCHAR(255) NOT NULL,
        hash              VARCHAR(64) NOT NULL,
        fecha             DATETIME2(3) NOT NULL CONSTRAINT DF_CargaSubmaquila_fecha DEFAULT SYSUTCDATETIME(),
        usuario_id        BIGINT NOT NULL,
        estado            VARCHAR(20) NOT NULL,
        total_filas       INT NOT NULL DEFAULT 0,
        filas_validas     INT NOT NULL DEFAULT 0,
        filas_invalidas   INT NOT NULL DEFAULT 0,
        version_contrato  VARCHAR(60) NOT NULL,
        correlation_id    VARCHAR(40) NOT NULL,
        CONSTRAINT PK_CargaSubmaquila PRIMARY KEY (id),
        CONSTRAINT UQ_CargaSubmaquila_hash UNIQUE (hash),
        CONSTRAINT FK_CargaSubmaquila_Usuario FOREIGN KEY (usuario_id) REFERENCES app24.UsuarioApp (id)
    );
END;
GO

IF OBJECT_ID('app24.CargaSubmaquilaFila', 'U') IS NULL
BEGIN
    CREATE TABLE app24.CargaSubmaquilaFila (
        id BIGINT IDENTITY(1,1) NOT NULL,
        carga_id BIGINT NOT NULL,
        hoja VARCHAR(80) NOT NULL,
        fila INT NOT NULL,
        datos_json NVARCHAR(MAX) NOT NULL,
        CONSTRAINT PK_CargaSubmaquilaFila PRIMARY KEY (id),
        CONSTRAINT FK_CargaSubmaquilaFila_Carga FOREIGN KEY (carga_id) REFERENCES app24.CargaSubmaquila(id),
        CONSTRAINT UQ_CargaSubmaquilaFila UNIQUE (carga_id, hoja, fila),
        CONSTRAINT CK_CargaSubmaquilaFila_JSON CHECK (ISJSON(datos_json) = 1)
    );
END;
GO

IF OBJECT_ID('app24.ErrorCargaSubmaquila', 'U') IS NULL
BEGIN
    CREATE TABLE app24.ErrorCargaSubmaquila (
        id BIGINT IDENTITY(1,1) NOT NULL,
        carga_id BIGINT NOT NULL,
        hoja VARCHAR(80) NULL,
        fila INT NULL,
        columna VARCHAR(80) NULL,
        valor_enmascarado VARCHAR(80) NULL,
        codigo VARCHAR(120) NOT NULL,
        mensaje VARCHAR(500) NOT NULL,
        CONSTRAINT PK_ErrorCargaSubmaquila PRIMARY KEY (id),
        CONSTRAINT FK_ErrorCargaSubmaquila_Carga FOREIGN KEY (carga_id) REFERENCES app24.CargaSubmaquila(id)
    );
END;
GO

IF NOT EXISTS (SELECT 1 FROM sys.indexes WHERE name = 'IX_CargaSubmaquilaFila_Carga' AND object_id = OBJECT_ID('app24.CargaSubmaquilaFila'))
    CREATE INDEX IX_CargaSubmaquilaFila_Carga ON app24.CargaSubmaquilaFila(carga_id, fila);
IF NOT EXISTS (SELECT 1 FROM sys.indexes WHERE name = 'IX_ErrorCargaSubmaquila_Carga' AND object_id = OBJECT_ID('app24.ErrorCargaSubmaquila'))
    CREATE INDEX IX_ErrorCargaSubmaquila_Carga ON app24.ErrorCargaSubmaquila(carga_id, fila, id);
GO

DECLARE @RestriccionEstado SYSNAME = (
    SELECT TOP (1) name
    FROM sys.check_constraints
    WHERE parent_object_id = OBJECT_ID('app24.CargaSubmaquila')
      AND definition LIKE '%estado%'
    ORDER BY name
);

IF @RestriccionEstado IS NOT NULL
   AND NOT EXISTS (
       SELECT 1
       FROM sys.check_constraints
       WHERE parent_object_id = OBJECT_ID('app24.CargaSubmaquila')
         AND name = @RestriccionEstado
         AND definition LIKE '%CONFIRMADA%'
   )
BEGIN
    DECLARE @SqlDrop NVARCHAR(500) =
        N'ALTER TABLE app24.CargaSubmaquila DROP CONSTRAINT ' + QUOTENAME(@RestriccionEstado);
    EXEC sp_executesql @SqlDrop;
    SET @RestriccionEstado = NULL;
END;

IF @RestriccionEstado IS NULL
BEGIN
    ALTER TABLE app24.CargaSubmaquila WITH CHECK
        ADD CONSTRAINT CK_CargaSubmaquila_estado
        CHECK (estado IN ('PREVISUALIZADA', 'CON_ERRORES', 'CONFIRMADA'));
END;
GO

IF COL_LENGTH('app24.CargaSubmaquila', 'fecha_confirmacion') IS NULL
BEGIN
    ALTER TABLE app24.CargaSubmaquila ADD fecha_confirmacion DATETIME2(3) NULL;
END;
GO

IF NOT EXISTS (SELECT 1 FROM app24.Actividad WHERE clave = 'SUBMAQUILA_CONFIRMAR')
    INSERT INTO app24.Actividad (clave, nombre, recurso, accion)
    VALUES ('SUBMAQUILA_CONFIRMAR', 'Confirmar constancias de transferencia', 'submaquilas', 'CONFIRMAR');

IF NOT EXISTS (SELECT 1 FROM app24.Actividad WHERE clave = 'SUBMAQUILA_CARGAR')
    INSERT INTO app24.Actividad (clave, nombre, recurso, accion)
    VALUES ('SUBMAQUILA_CARGAR', 'Cargar constancias de transferencia para previsualización', 'submaquilas', 'CARGAR');

INSERT INTO app24.PerfilActividad (perfil_id, actividad_id)
SELECT p.id, a.id
FROM app24.PerfilApp p
JOIN app24.Actividad a ON a.clave IN ('SUBMAQUILA_CONFIRMAR', 'SUBMAQUILA_CARGAR')
WHERE p.nombre = 'ADMINISTRADOR'
  AND NOT EXISTS (
      SELECT 1
      FROM app24.PerfilActividad pa
      WHERE pa.perfil_id = p.id
        AND pa.actividad_id = a.id
  );
GO

CREATE OR ALTER PROCEDURE app24.APP24_Q_SUBMAQUILA_CARGA_POR_HASH @Hash VARCHAR(64), @Existe BIT OUTPUT AS
BEGIN SET NOCOUNT ON; SELECT @Existe = CASE WHEN EXISTS (SELECT 1 FROM app24.CargaSubmaquila WHERE hash = @Hash) THEN 1 ELSE 0 END; END;
GO

CREATE OR ALTER PROCEDURE app24.APP24_C_SUBMAQUILA_CARGA_CREAR
    @Archivo VARCHAR(255), @Hash VARCHAR(64), @UsuarioId BIGINT, @Estado VARCHAR(20), @TotalFilas INT,
    @FilasValidas INT, @VersionContrato VARCHAR(60), @CorrelationId VARCHAR(40), @FilasJson NVARCHAR(MAX),
    @ErroresJson NVARCHAR(MAX), @CargaId BIGINT OUTPUT
AS
BEGIN
    SET NOCOUNT ON; SET XACT_ABORT ON;
    -- Invariante: la creación de staging sólo admite PREVISUALIZADA o CON_ERRORES.
    -- CONFIRMADA es un estado exclusivamente terminal y lo establece el wrapper autoritativo
    -- dbo.APP24_C_SUBMAQUILA_CARGA_CONFIRMAR tras delegar en dbo.CARGA_SUBMAQUILA.
    IF @Archivo IS NULL OR @Hash IS NULL OR LEN(@Hash) <> 64 OR @UsuarioId IS NULL OR @Estado NOT IN ('PREVISUALIZADA','CON_ERRORES')
       OR @TotalFilas < 0 OR @FilasValidas < 0 OR @FilasValidas > @TotalFilas OR @VersionContrato IS NULL OR @CorrelationId IS NULL
       OR ISJSON(@FilasJson) <> 1 OR ISJSON(@ErroresJson) <> 1 THROW 51320, 'PARAMETRO_INVALIDO', 1;
    BEGIN TRY
        BEGIN TRANSACTION;
        INSERT INTO app24.CargaSubmaquila(archivo,hash,usuario_id,estado,total_filas,filas_validas,filas_invalidas,version_contrato,correlation_id)
        VALUES(@Archivo,@Hash,@UsuarioId,@Estado,@TotalFilas,@FilasValidas,@TotalFilas-@FilasValidas,@VersionContrato,@CorrelationId);
        SET @CargaId = CONVERT(BIGINT,SCOPE_IDENTITY());
        INSERT INTO app24.CargaSubmaquilaFila(carga_id,hoja,fila,datos_json)
        SELECT @CargaId,hoja,fila,datos FROM OPENJSON(@FilasJson) WITH(hoja VARCHAR(80) '$.hoja',fila INT '$.fila',datos NVARCHAR(MAX) '$.datos' AS JSON);
        INSERT INTO app24.ErrorCargaSubmaquila(carga_id,hoja,fila,columna,valor_enmascarado,codigo,mensaje)
        SELECT @CargaId,hoja,fila,columna,valor_enmascarado,codigo,mensaje FROM OPENJSON(@ErroresJson)
        WITH(hoja VARCHAR(80) '$.hoja',fila INT '$.fila',columna VARCHAR(80) '$.columna',valor_enmascarado VARCHAR(80) '$.valorEnmascarado',codigo VARCHAR(120) '$.codigo',mensaje VARCHAR(500) '$.mensaje');
        DECLARE @Accion VARCHAR(40) = CASE WHEN @Estado='PREVISUALIZADA' THEN 'CARGA_VALIDADA' ELSE 'CARGA_CON_ERRORES' END,
                @Resultado VARCHAR(20) = CASE WHEN @Estado='PREVISUALIZADA' THEN 'EXITO' ELSE 'FALLO' END,
                @Detalle VARCHAR(500) = CONCAT('submaquila carga=',@CargaId), @EventoId BIGINT;
        EXEC app24.APP24_C_BITACORA_REGISTRAR @UsuarioId=@UsuarioId,@Modulo='OPERACIONES',@Accion=@Accion,@Detalle=@Detalle,@CorrelacionId=@CorrelationId,@Resultado=@Resultado,@EventoId=@EventoId OUTPUT;
        COMMIT TRANSACTION;
    END TRY BEGIN CATCH IF XACT_STATE() <> 0 ROLLBACK TRANSACTION; THROW; END CATCH;
END;
GO

CREATE OR ALTER PROCEDURE app24.APP24_Q_SUBMAQUILA_CARGA_OBTENER @CargaId BIGINT, @Pagina INT=1, @Tamano INT=100 AS
BEGIN
    SET NOCOUNT ON; IF @CargaId IS NULL OR @CargaId < 1 OR @Pagina < 1 OR @Tamano < 1 OR @Tamano > 100 THROW 51321, 'PARAMETRO_INVALIDO', 1;
    SELECT id,archivo,hash,estado,total_filas,filas_validas,filas_invalidas,version_contrato,correlation_id FROM app24.CargaSubmaquila WHERE id=@CargaId;
    SELECT hoja,fila,datos_json FROM app24.CargaSubmaquilaFila WHERE carga_id=@CargaId ORDER BY fila,id OFFSET (CONVERT(BIGINT,@Pagina)-1)*CONVERT(BIGINT,@Tamano) ROWS FETCH NEXT @Tamano ROWS ONLY;
    SELECT COUNT_BIG(1) AS total_filas FROM app24.CargaSubmaquilaFila WHERE carga_id=@CargaId;
    SELECT hoja,fila,columna,valor_enmascarado,codigo,mensaje FROM app24.ErrorCargaSubmaquila WHERE carga_id=@CargaId ORDER BY ISNULL(fila,0),id;
END;
GO

CREATE OR ALTER PROCEDURE app24.APP24_Q_SUBMAQUILA_CARGA_ERRORES @CargaId BIGINT, @Pagina INT=1, @Tamano INT=100 AS
BEGIN
    SET NOCOUNT ON; IF @CargaId IS NULL OR @CargaId < 1 OR @Pagina < 1 OR @Tamano < 1 OR @Tamano > 100 THROW 51322, 'PARAMETRO_INVALIDO', 1;
    SELECT hoja,fila,columna,valor_enmascarado,codigo,mensaje FROM app24.ErrorCargaSubmaquila WHERE carga_id=@CargaId ORDER BY ISNULL(fila,0),id OFFSET (CONVERT(BIGINT,@Pagina)-1)*CONVERT(BIGINT,@Tamano) ROWS FETCH NEXT @Tamano ROWS ONLY;
END;
GO

IF DATABASE_PRINCIPAL_ID('app24_runtime') IS NOT NULL
BEGIN
    GRANT EXECUTE ON OBJECT::app24.APP24_Q_SUBMAQUILA_CARGA_POR_HASH TO app24_runtime;
    GRANT EXECUTE ON OBJECT::app24.APP24_C_SUBMAQUILA_CARGA_CREAR TO app24_runtime;
    GRANT EXECUTE ON OBJECT::app24.APP24_Q_SUBMAQUILA_CARGA_OBTENER TO app24_runtime;
    GRANT EXECUTE ON OBJECT::app24.APP24_Q_SUBMAQUILA_CARGA_ERRORES TO app24_runtime;
END;
GO

PRINT 'Migration 18-submaquila-staging-confirmation-v1 aplicada.';
GO
