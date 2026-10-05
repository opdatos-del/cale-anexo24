-- Habilita la confirmación autoritativa de cargas de agentes aduanales:
--   * crea el staging real app24.CargaCatalogoAgente*;
--   * agrega estado CONFIRMADA + fecha_confirmacion a CargaCatalogoAgente;
--   * instala el SP de autorización app24.APP24_Q_CATALOGO_AGENTE_CARGA_POR_HASH;
--   * instala el command de staging app24.APP24_C_CATALOGO_AGENTE_CARGA_CREAR;
--   * instala los queries de lectura y errores;
--   * otorga EXECUTE al rol app24_runtime cuando existe;
--   * crea la actividad AGENTES_CONFIRMAR/AGENTES_CARGAR y la asigna
--     al perfil ADMINISTRADOR.
--
-- La operación autoritativa corre en CALE_IMMEX.dbo.APP24_C_AGENTE_CARGA_CONFIRMAR,
-- que orquesta el stage legacy dbo.TMPagentes / dbo.ECARGAagentes y delega la
-- regla de negocio en dbo.CARGAAgentes.
USE ANEXO24_DEV;
GO

IF OBJECT_ID('app24.CargaCatalogoAgente', 'U') IS NULL
BEGIN
    CREATE TABLE app24.CargaCatalogoAgente (
        id                BIGINT IDENTITY(1,1) NOT NULL,
        archivo           VARCHAR(255) NOT NULL,
        hash              VARCHAR(64) NOT NULL,
        fecha             DATETIME2(3) NOT NULL CONSTRAINT DF_CargaCatalogoAgente_fecha DEFAULT SYSUTCDATETIME(),
        usuario_id        BIGINT NOT NULL,
        estado            VARCHAR(20) NOT NULL,
        total_filas       INT NOT NULL DEFAULT 0,
        filas_validas     INT NOT NULL DEFAULT 0,
        filas_invalidas   INT NOT NULL DEFAULT 0,
        version_contrato  VARCHAR(60) NOT NULL,
        correlation_id    VARCHAR(40) NOT NULL,
        CONSTRAINT PK_CargaCatalogoAgente PRIMARY KEY (id),
        CONSTRAINT UQ_CargaCatalogoAgente_hash UNIQUE (hash),
        CONSTRAINT FK_CargaCatalogoAgente_Usuario FOREIGN KEY (usuario_id) REFERENCES app24.UsuarioApp (id)
    );
END;
GO

IF OBJECT_ID('app24.CargaCatalogoAgenteFila', 'U') IS NULL
BEGIN
    CREATE TABLE app24.CargaCatalogoAgenteFila (
        id BIGINT IDENTITY(1,1) NOT NULL,
        carga_id BIGINT NOT NULL,
        hoja VARCHAR(80) NOT NULL,
        fila INT NOT NULL,
        datos_json NVARCHAR(MAX) NOT NULL,
        CONSTRAINT PK_CargaCatalogoAgenteFila PRIMARY KEY (id),
        CONSTRAINT FK_CargaCatalogoAgenteFila_Carga FOREIGN KEY (carga_id) REFERENCES app24.CargaCatalogoAgente(id),
        CONSTRAINT UQ_CargaCatalogoAgenteFila UNIQUE (carga_id, hoja, fila),
        CONSTRAINT CK_CargaCatalogoAgenteFila_JSON CHECK (ISJSON(datos_json) = 1)
    );
END;
GO

IF OBJECT_ID('app24.ErrorCargaAgente', 'U') IS NULL
BEGIN
    CREATE TABLE app24.ErrorCargaAgente (
        id BIGINT IDENTITY(1,1) NOT NULL,
        carga_id BIGINT NOT NULL,
        hoja VARCHAR(80) NULL,
        fila INT NULL,
        columna VARCHAR(80) NULL,
        valor_enmascarado VARCHAR(80) NULL,
        codigo VARCHAR(120) NOT NULL,
        mensaje VARCHAR(500) NOT NULL,
        CONSTRAINT PK_ErrorCargaAgente PRIMARY KEY (id),
        CONSTRAINT FK_ErrorCargaAgente_Carga FOREIGN KEY (carga_id) REFERENCES app24.CargaCatalogoAgente(id)
    );
END;
GO

IF NOT EXISTS (SELECT 1 FROM sys.indexes WHERE name = 'IX_CargaCatalogoAgenteFila_Carga' AND object_id = OBJECT_ID('app24.CargaCatalogoAgenteFila'))
    CREATE INDEX IX_CargaCatalogoAgenteFila_Carga ON app24.CargaCatalogoAgenteFila(carga_id, fila);
IF NOT EXISTS (SELECT 1 FROM sys.indexes WHERE name = 'IX_ErrorCargaAgente_Carga' AND object_id = OBJECT_ID('app24.ErrorCargaAgente'))
    CREATE INDEX IX_ErrorCargaAgente_Carga ON app24.ErrorCargaAgente(carga_id, fila, id);
GO

DECLARE @RestriccionEstado SYSNAME = (
    SELECT TOP (1) name
    FROM sys.check_constraints
    WHERE parent_object_id = OBJECT_ID('app24.CargaCatalogoAgente')
      AND definition LIKE '%estado%'
    ORDER BY name
);

IF @RestriccionEstado IS NOT NULL
   AND NOT EXISTS (
       SELECT 1
       FROM sys.check_constraints
       WHERE parent_object_id = OBJECT_ID('app24.CargaCatalogoAgente')
         AND name = @RestriccionEstado
         AND definition LIKE '%CONFIRMADA%'
   )
BEGIN
    DECLARE @SqlDrop NVARCHAR(500) =
        N'ALTER TABLE app24.CargaCatalogoAgente DROP CONSTRAINT ' + QUOTENAME(@RestriccionEstado);
    EXEC sp_executesql @SqlDrop;
    SET @RestriccionEstado = NULL;
END;

IF @RestriccionEstado IS NULL
BEGIN
    ALTER TABLE app24.CargaCatalogoAgente WITH CHECK
        ADD CONSTRAINT CK_CargaCatalogoAgente_estado
        CHECK (estado IN ('PREVISUALIZADA', 'CON_ERRORES', 'CONFIRMADA'));
END;
GO

IF COL_LENGTH('app24.CargaCatalogoAgente', 'fecha_confirmacion') IS NULL
BEGIN
    ALTER TABLE app24.CargaCatalogoAgente ADD fecha_confirmacion DATETIME2(3) NULL;
END;
GO

IF NOT EXISTS (SELECT 1 FROM app24.Actividad WHERE clave = 'AGENTES_CONFIRMAR')
    INSERT INTO app24.Actividad (clave, nombre, recurso, accion)
    VALUES ('AGENTES_CONFIRMAR', 'Confirmar agentes aduanales', 'agentes', 'CONFIRMAR');

IF NOT EXISTS (SELECT 1 FROM app24.Actividad WHERE clave = 'AGENTES_CARGAR')
    INSERT INTO app24.Actividad (clave, nombre, recurso, accion)
    VALUES ('AGENTES_CARGAR', 'Cargar agentes aduanales para previsualización', 'agentes', 'CARGAR');

INSERT INTO app24.PerfilActividad (perfil_id, actividad_id)
SELECT p.id, a.id
FROM app24.PerfilApp p
JOIN app24.Actividad a ON a.clave IN ('AGENTES_CONFIRMAR', 'AGENTES_CARGAR')
WHERE p.nombre = 'ADMINISTRADOR'
  AND NOT EXISTS (
      SELECT 1
      FROM app24.PerfilActividad pa
      WHERE pa.perfil_id = p.id
        AND pa.actividad_id = a.id
  );
GO

CREATE OR ALTER PROCEDURE app24.APP24_Q_CATALOGO_AGENTE_CARGA_POR_HASH @Hash VARCHAR(64), @Existe BIT OUTPUT AS
BEGIN SET NOCOUNT ON; SELECT @Existe = CASE WHEN EXISTS (SELECT 1 FROM app24.CargaCatalogoAgente WHERE hash = @Hash) THEN 1 ELSE 0 END; END;
GO

CREATE OR ALTER PROCEDURE app24.APP24_C_CATALOGO_AGENTE_CARGA_CREAR
    @Archivo VARCHAR(255), @Hash VARCHAR(64), @UsuarioId BIGINT, @Estado VARCHAR(20), @TotalFilas INT,
    @FilasValidas INT, @VersionContrato VARCHAR(60), @CorrelationId VARCHAR(40), @FilasJson NVARCHAR(MAX),
    @ErroresJson NVARCHAR(MAX), @CargaId BIGINT OUTPUT
AS
BEGIN
    SET NOCOUNT ON; SET XACT_ABORT ON;
    -- Invariante: la creación de staging sólo admite PREVISUALIZADA o CON_ERRORES.
    -- CONFIRMADA es un estado exclusivamente terminal y lo establece el wrapper autoritativo
    -- dbo.APP24_C_AGENTE_CARGA_CONFIRMAR tras delegar en dbo.CARGAAgentes.
    IF @Archivo IS NULL OR @Hash IS NULL OR LEN(@Hash) <> 64 OR @UsuarioId IS NULL OR @Estado NOT IN ('PREVISUALIZADA','CON_ERRORES')
       OR @TotalFilas < 0 OR @FilasValidas < 0 OR @FilasValidas > @TotalFilas OR @VersionContrato IS NULL OR @CorrelationId IS NULL
       OR ISJSON(@FilasJson) <> 1 OR ISJSON(@ErroresJson) <> 1 THROW 51320, 'PARAMETRO_INVALIDO', 1;
    BEGIN TRY
        BEGIN TRANSACTION;
        INSERT INTO app24.CargaCatalogoAgente(archivo,hash,usuario_id,estado,total_filas,filas_validas,filas_invalidas,version_contrato,correlation_id)
        VALUES(@Archivo,@Hash,@UsuarioId,@Estado,@TotalFilas,@FilasValidas,@TotalFilas-@FilasValidas,@VersionContrato,@CorrelationId);
        SET @CargaId = CONVERT(BIGINT,SCOPE_IDENTITY());
        INSERT INTO app24.CargaCatalogoAgenteFila(carga_id,hoja,fila,datos_json)
        SELECT @CargaId,hoja,fila,datos FROM OPENJSON(@FilasJson) WITH(hoja VARCHAR(80) '$.hoja',fila INT '$.fila',datos NVARCHAR(MAX) '$.datos' AS JSON);
        INSERT INTO app24.ErrorCargaAgente(carga_id,hoja,fila,columna,valor_enmascarado,codigo,mensaje)
        SELECT @CargaId,hoja,fila,columna,valor_enmascarado,codigo,mensaje FROM OPENJSON(@ErroresJson)
        WITH(hoja VARCHAR(80) '$.hoja',fila INT '$.fila',columna VARCHAR(80) '$.columna',valor_enmascarado VARCHAR(80) '$.valorEnmascarado',codigo VARCHAR(120) '$.codigo',mensaje VARCHAR(500) '$.mensaje');
        DECLARE @Accion VARCHAR(40) = CASE WHEN @Estado='PREVISUALIZADA' THEN 'CARGA_VALIDADA' ELSE 'CARGA_CON_ERRORES' END,
                @Resultado VARCHAR(20) = CASE WHEN @Estado='PREVISUALIZADA' THEN 'EXITO' ELSE 'FALLO' END,
                @Detalle VARCHAR(500) = CONCAT('agentes carga=',@CargaId), @EventoId BIGINT;
        EXEC app24.APP24_C_BITACORA_REGISTRAR @UsuarioId=@UsuarioId,@Modulo='CATALOGOS',@Accion=@Accion,@Detalle=@Detalle,@CorrelacionId=@CorrelationId,@Resultado=@Resultado,@EventoId=@EventoId OUTPUT;
        COMMIT TRANSACTION;
    END TRY BEGIN CATCH IF XACT_STATE() <> 0 ROLLBACK TRANSACTION; THROW; END CATCH;
END;
GO

CREATE OR ALTER PROCEDURE app24.APP24_Q_CATALOGO_AGENTE_CARGA_OBTENER @CargaId BIGINT, @Pagina INT=1, @Tamano INT=100 AS
BEGIN
    SET NOCOUNT ON; IF @CargaId IS NULL OR @CargaId < 1 OR @Pagina < 1 OR @Tamano < 1 OR @Tamano > 100 THROW 51321, 'PARAMETRO_INVALIDO', 1;
    SELECT id,archivo,hash,estado,total_filas,filas_validas,filas_invalidas,version_contrato,correlation_id FROM app24.CargaCatalogoAgente WHERE id=@CargaId;
    SELECT hoja,fila,datos_json FROM app24.CargaCatalogoAgenteFila WHERE carga_id=@CargaId ORDER BY fila,id OFFSET (CONVERT(BIGINT,@Pagina)-1)*CONVERT(BIGINT,@Tamano) ROWS FETCH NEXT @Tamano ROWS ONLY;
    SELECT COUNT_BIG(1) AS total_filas FROM app24.CargaCatalogoAgenteFila WHERE carga_id=@CargaId;
    SELECT hoja,fila,columna,valor_enmascarado,codigo,mensaje FROM app24.ErrorCargaAgente WHERE carga_id=@CargaId ORDER BY ISNULL(fila,0),id;
END;
GO

CREATE OR ALTER PROCEDURE app24.APP24_Q_CATALOGO_AGENTE_CARGA_ERRORES @CargaId BIGINT, @Pagina INT=1, @Tamano INT=100 AS
BEGIN
    SET NOCOUNT ON; IF @CargaId IS NULL OR @CargaId < 1 OR @Pagina < 1 OR @Tamano < 1 OR @Tamano > 100 THROW 51322, 'PARAMETRO_INVALIDO', 1;
    SELECT hoja,fila,columna,valor_enmascarado,codigo,mensaje FROM app24.ErrorCargaAgente WHERE carga_id=@CargaId ORDER BY ISNULL(fila,0),id OFFSET (CONVERT(BIGINT,@Pagina)-1)*CONVERT(BIGINT,@Tamano) ROWS FETCH NEXT @Tamano ROWS ONLY;
END;
GO

IF DATABASE_PRINCIPAL_ID('app24_runtime') IS NOT NULL
BEGIN
    GRANT EXECUTE ON OBJECT::app24.APP24_Q_CATALOGO_AGENTE_CARGA_POR_HASH TO app24_runtime;
    GRANT EXECUTE ON OBJECT::app24.APP24_C_CATALOGO_AGENTE_CARGA_CREAR TO app24_runtime;
    GRANT EXECUTE ON OBJECT::app24.APP24_Q_CATALOGO_AGENTE_CARGA_OBTENER TO app24_runtime;
    GRANT EXECUTE ON OBJECT::app24.APP24_Q_CATALOGO_AGENTE_CARGA_ERRORES TO app24_runtime;
END;
GO

PRINT 'Migration 17-agente-staging-confirmation-v1 aplicada.';
GO