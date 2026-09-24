USE ANEXO24_DEV;
GO

CREATE OR ALTER PROCEDURE app24.APP24_C_PERFIL_REEMPLAZAR_PERMISOS
    @PerfilId BIGINT,
    @ActividadIdsJson NVARCHAR(MAX),
    @FechaActual DATE
AS
BEGIN
    SET NOCOUNT ON;
    SET XACT_ABORT ON;

    IF @PerfilId IS NULL OR @PerfilId <= 0
       OR @FechaActual IS NULL
       OR @ActividadIdsJson IS NULL
       OR ISJSON(@ActividadIdsJson, ARRAY) <> 1
        THROW 51108, 'PARAMETRO_INVALIDO', 1;

    DECLARE @ActividadIds TABLE (
        actividad_id BIGINT NOT NULL PRIMARY KEY
    );
    DECLARE @TransaccionPropia BIT = 0;
    DECLARE @FilasAfectadas INT;

    IF EXISTS (
        SELECT 1
        FROM OPENJSON(@ActividadIdsJson)
        WHERE [type] <> 2
           OR [value] LIKE '%[^0-9]%'
           OR TRY_CONVERT(BIGINT, [value]) IS NULL
           OR TRY_CONVERT(BIGINT, [value]) <= 0
    )
        THROW 51108, 'PARAMETRO_INVALIDO', 1;

    IF EXISTS (
        SELECT 1
        FROM OPENJSON(@ActividadIdsJson)
        GROUP BY TRY_CONVERT(BIGINT, [value])
        HAVING COUNT_BIG(*) > 1
    )
        THROW 51108, 'PARAMETRO_INVALIDO', 1;

    INSERT INTO @ActividadIds (actividad_id)
    SELECT CONVERT(BIGINT, [value])
    FROM OPENJSON(@ActividadIdsJson);

    BEGIN TRY
        IF @@TRANCOUNT = 0
        BEGIN
            SET @TransaccionPropia = 1;
            BEGIN TRANSACTION;
        END;

        IF NOT EXISTS (SELECT 1 FROM app24.PerfilApp WITH (UPDLOCK, HOLDLOCK) WHERE id = @PerfilId)
            THROW 51102, 'PERFIL_NO_EXISTE', 1;

        SELECT @FilasAfectadas = COUNT(*)
        FROM app24.PerfilActividad WITH (UPDLOCK, HOLDLOCK)
        WHERE perfil_id = @PerfilId;

        IF EXISTS (
            SELECT 1
            FROM @ActividadIds ids
            LEFT JOIN app24.Actividad a WITH (UPDLOCK, HOLDLOCK) ON a.id = ids.actividad_id
            WHERE a.id IS NULL
        )
            THROW 51102, 'ACTIVIDAD_NO_EXISTE', 1;

        DELETE FROM app24.PerfilActividad
        WHERE perfil_id = @PerfilId;
        SET @FilasAfectadas = @@ROWCOUNT;

        INSERT INTO app24.PerfilActividad (perfil_id, actividad_id)
        SELECT @PerfilId, ids.actividad_id
        FROM @ActividadIds ids
        JOIN app24.Actividad a WITH (UPDLOCK, HOLDLOCK) ON a.id = ids.actividad_id;
        IF @@ROWCOUNT <> (SELECT COUNT(*) FROM @ActividadIds)
            THROW 51150, 'FILAS_AFECTADAS_INCONSISTENTES', 1;

        IF NOT EXISTS (
            SELECT 1
            FROM app24.UsuarioApp u WITH (UPDLOCK, HOLDLOCK)
            JOIN app24.PerfilApp p WITH (UPDLOCK, HOLDLOCK) ON p.id = u.perfil_id
            WHERE u.estado = 'ACTIVO'
              AND (u.vigencia IS NULL OR u.vigencia >= @FechaActual)
              AND p.estado = 'ACTIVO'
              AND EXISTS (
                  SELECT 1
                  FROM app24.PerfilActividad pa WITH (UPDLOCK, HOLDLOCK)
                  JOIN app24.Actividad a WITH (UPDLOCK, HOLDLOCK) ON a.id = pa.actividad_id
                  WHERE pa.perfil_id = p.id
                    AND a.clave = 'USUARIOS_ADMINISTRAR'
              )
              AND EXISTS (
                  SELECT 1
                  FROM app24.PerfilActividad pa WITH (UPDLOCK, HOLDLOCK)
                  JOIN app24.Actividad a WITH (UPDLOCK, HOLDLOCK) ON a.id = pa.actividad_id
                  WHERE pa.perfil_id = p.id
                    AND a.clave = 'PERFILES_ADMINISTRAR'
              )
        )
            THROW 51107, 'SIN_ADMINISTRADOR_EFECTIVO', 1;

        IF @TransaccionPropia = 1 COMMIT TRANSACTION;
    END TRY
    BEGIN CATCH
        IF @TransaccionPropia = 1 AND XACT_STATE() <> 0 ROLLBACK TRANSACTION;
        THROW;
    END CATCH;
END;
GO
