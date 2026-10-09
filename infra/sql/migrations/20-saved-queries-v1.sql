USE ANEXO24_DEV;
GO

-- Presets estructurados por usuario. Nunca almacena ni ejecuta SQL legado.
IF OBJECT_ID('app24.ConsultaGuardada', 'U') IS NULL
BEGIN
    CREATE TABLE app24.ConsultaGuardada (
        id                  BIGINT IDENTITY(1,1) NOT NULL,
        usuario_id          BIGINT NOT NULL,
        nombre              NVARCHAR(80) NOT NULL,
        descripcion         NVARCHAR(250) NULL,
        alcance             VARCHAR(40) NOT NULL,
        criterios_json      NVARCHAR(4000) NOT NULL,
        fecha_creacion      DATETIME2(3) NOT NULL CONSTRAINT DF_ConsultaGuardada_fecha_creacion DEFAULT SYSUTCDATETIME(),
        fecha_actualizacion DATETIME2(3) NOT NULL CONSTRAINT DF_ConsultaGuardada_fecha_actualizacion DEFAULT SYSUTCDATETIME(),
        CONSTRAINT PK_ConsultaGuardada PRIMARY KEY (id),
        CONSTRAINT FK_ConsultaGuardada_Usuario FOREIGN KEY (usuario_id) REFERENCES app24.UsuarioApp(id),
        CONSTRAINT UQ_ConsultaGuardada_UsuarioAlcanceNombre UNIQUE (usuario_id, alcance, nombre),
        CONSTRAINT CK_ConsultaGuardada_alcance CHECK (alcance IN ('REPORTES', 'ENTRADAS', 'SALIDAS', 'MATERIALES_UTILIZADOS', 'ACTIVOS_FIJOS')),
        CONSTRAINT CK_ConsultaGuardada_criterios_json CHECK (ISJSON(criterios_json) = 1)
    );
END;
GO

IF NOT EXISTS (SELECT 1 FROM sys.indexes WHERE name = 'IX_ConsultaGuardada_UsuarioActualizacion' AND object_id = OBJECT_ID('app24.ConsultaGuardada'))
    CREATE INDEX IX_ConsultaGuardada_UsuarioActualizacion ON app24.ConsultaGuardada(usuario_id, fecha_actualizacion DESC, nombre ASC, id ASC);
GO

CREATE OR ALTER PROCEDURE app24.APP24_Q_CONSULTAS_GUARDADAS_LISTAR
    @UsuarioId BIGINT,
    @Alcance VARCHAR(40) = NULL
AS
BEGIN
    SET NOCOUNT ON;
    SET @Alcance = NULLIF(LTRIM(RTRIM(@Alcance)), '');
    IF @UsuarioId IS NULL OR @UsuarioId <= 0
        THROW 51201, 'PARAMETRO_INVALIDO', 1;
    IF @Alcance IS NOT NULL AND @Alcance NOT IN ('REPORTES', 'ENTRADAS', 'SALIDAS', 'MATERIALES_UTILIZADOS', 'ACTIVOS_FIJOS')
        THROW 51201, 'PARAMETRO_INVALIDO', 1;

    SELECT id, nombre, descripcion, alcance, criterios_json, fecha_creacion, fecha_actualizacion
    FROM app24.ConsultaGuardada
    WHERE usuario_id = @UsuarioId AND (@Alcance IS NULL OR alcance = @Alcance)
    ORDER BY fecha_actualizacion DESC, nombre ASC, id ASC;
END;
GO

CREATE OR ALTER PROCEDURE app24.APP24_C_CONSULTA_GUARDADA_CREAR
    @UsuarioId BIGINT,
    @Nombre NVARCHAR(80),
    @Descripcion NVARCHAR(250) = NULL,
    @Alcance VARCHAR(40),
    @CriteriosJson NVARCHAR(4000)
AS
BEGIN
    SET NOCOUNT ON;
    SET XACT_ABORT ON;
    SET @Nombre = NULLIF(LTRIM(RTRIM(@Nombre)), '');
    SET @Descripcion = NULLIF(LTRIM(RTRIM(@Descripcion)), '');
    SET @Alcance = NULLIF(LTRIM(RTRIM(@Alcance)), '');
    IF @UsuarioId IS NULL OR @UsuarioId <= 0 OR @Nombre IS NULL OR LEN(@Nombre) > 80
       OR LEN(@Descripcion) > 250 OR @Alcance NOT IN ('REPORTES', 'ENTRADAS', 'SALIDAS', 'MATERIALES_UTILIZADOS', 'ACTIVOS_FIJOS')
       OR @CriteriosJson IS NULL OR LEN(@CriteriosJson) > 4000 OR ISJSON(@CriteriosJson) <> 1
        THROW 51201, 'PARAMETRO_INVALIDO', 1;

    BEGIN TRY
        BEGIN TRANSACTION;
        IF (SELECT COUNT_BIG(*) FROM app24.ConsultaGuardada WITH (UPDLOCK, HOLDLOCK) WHERE usuario_id = @UsuarioId) >= 100
            THROW 51204, 'LIMITE_CONSULTAS_ALCANZADO', 1;
        IF EXISTS (SELECT 1 FROM app24.ConsultaGuardada WITH (UPDLOCK, HOLDLOCK)
                   WHERE usuario_id = @UsuarioId AND alcance = @Alcance AND nombre = @Nombre)
            THROW 51203, 'RECURSO_DUPLICADO', 1;

        INSERT INTO app24.ConsultaGuardada(usuario_id, nombre, descripcion, alcance, criterios_json)
        VALUES (@UsuarioId, @Nombre, @Descripcion, @Alcance, @CriteriosJson);

        DECLARE @Id BIGINT = CONVERT(BIGINT, SCOPE_IDENTITY());
        SELECT id, nombre, descripcion, alcance, criterios_json, fecha_creacion, fecha_actualizacion
        FROM app24.ConsultaGuardada WHERE id = @Id AND usuario_id = @UsuarioId;
        COMMIT TRANSACTION;
    END TRY
    BEGIN CATCH
        IF XACT_STATE() <> 0 ROLLBACK TRANSACTION;
        IF ERROR_NUMBER() IN (2601, 2627) THROW 51203, 'RECURSO_DUPLICADO', 1;
        THROW;
    END CATCH;
END;
GO

CREATE OR ALTER PROCEDURE app24.APP24_C_CONSULTA_GUARDADA_ACTUALIZAR
    @Id BIGINT,
    @UsuarioId BIGINT,
    @Nombre NVARCHAR(80),
    @Descripcion NVARCHAR(250) = NULL,
    @Alcance VARCHAR(40),
    @CriteriosJson NVARCHAR(4000)
AS
BEGIN
    SET NOCOUNT ON;
    SET XACT_ABORT ON;
    SET @Nombre = NULLIF(LTRIM(RTRIM(@Nombre)), '');
    SET @Descripcion = NULLIF(LTRIM(RTRIM(@Descripcion)), '');
    SET @Alcance = NULLIF(LTRIM(RTRIM(@Alcance)), '');
    IF @Id IS NULL OR @Id <= 0 OR @UsuarioId IS NULL OR @UsuarioId <= 0 OR @Nombre IS NULL OR LEN(@Nombre) > 80
       OR LEN(@Descripcion) > 250 OR @Alcance NOT IN ('REPORTES', 'ENTRADAS', 'SALIDAS', 'MATERIALES_UTILIZADOS', 'ACTIVOS_FIJOS')
       OR @CriteriosJson IS NULL OR LEN(@CriteriosJson) > 4000 OR ISJSON(@CriteriosJson) <> 1
        THROW 51201, 'PARAMETRO_INVALIDO', 1;

    BEGIN TRY
        BEGIN TRANSACTION;
        IF NOT EXISTS (SELECT 1 FROM app24.ConsultaGuardada WITH (UPDLOCK, HOLDLOCK) WHERE id = @Id AND usuario_id = @UsuarioId)
            THROW 51202, 'RECURSO_NO_ENCONTRADO', 1;
        IF EXISTS (SELECT 1 FROM app24.ConsultaGuardada WITH (UPDLOCK, HOLDLOCK)
                   WHERE usuario_id = @UsuarioId AND alcance = @Alcance AND nombre = @Nombre AND id <> @Id)
            THROW 51203, 'RECURSO_DUPLICADO', 1;

        UPDATE app24.ConsultaGuardada
        SET nombre = @Nombre, descripcion = @Descripcion, alcance = @Alcance,
            criterios_json = @CriteriosJson, fecha_actualizacion = SYSUTCDATETIME()
        WHERE id = @Id AND usuario_id = @UsuarioId;

        SELECT id, nombre, descripcion, alcance, criterios_json, fecha_creacion, fecha_actualizacion
        FROM app24.ConsultaGuardada WHERE id = @Id AND usuario_id = @UsuarioId;
        COMMIT TRANSACTION;
    END TRY
    BEGIN CATCH
        IF XACT_STATE() <> 0 ROLLBACK TRANSACTION;
        IF ERROR_NUMBER() IN (2601, 2627) THROW 51203, 'RECURSO_DUPLICADO', 1;
        THROW;
    END CATCH;
END;
GO

CREATE OR ALTER PROCEDURE app24.APP24_C_CONSULTA_GUARDADA_ELIMINAR
    @Id BIGINT,
    @UsuarioId BIGINT
AS
BEGIN
    SET NOCOUNT ON;
    IF @Id IS NULL OR @Id <= 0 OR @UsuarioId IS NULL OR @UsuarioId <= 0
        THROW 51201, 'PARAMETRO_INVALIDO', 1;
    DELETE FROM app24.ConsultaGuardada WHERE id = @Id AND usuario_id = @UsuarioId;
    IF @@ROWCOUNT <> 1 THROW 51202, 'RECURSO_NO_ENCONTRADO', 1;
END;
GO

PRINT 'Migration 20-saved-queries-v1 aplicada.';
GO
