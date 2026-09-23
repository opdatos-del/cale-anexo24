USE ANEXO24_DEV;
GO

CREATE OR ALTER PROCEDURE app24.APP24_C_USUARIO_CAMBIAR_PERFIL
    @UsuarioId BIGINT,
    @PerfilId BIGINT,
    @ActorId BIGINT,
    @FechaActual DATE
AS
BEGIN
    SET NOCOUNT ON;
    SET XACT_ABORT ON;

    IF @UsuarioId IS NULL OR @UsuarioId <= 0
       OR @PerfilId IS NULL OR @PerfilId <= 0
       OR @ActorId IS NULL OR @ActorId <= 0
       OR @FechaActual IS NULL
        THROW 51108, 'PARAMETRO_INVALIDO', 1;

    DECLARE @TransaccionPropia BIT = 0;
    DECLARE @PerfilActual BIGINT;
    BEGIN TRY
        IF @@TRANCOUNT = 0
        BEGIN
            SET @TransaccionPropia = 1;
            BEGIN TRANSACTION;
        END;

        SELECT @PerfilActual = perfil_id
        FROM app24.UsuarioApp WITH (UPDLOCK, HOLDLOCK)
        WHERE id = @UsuarioId;
        IF @PerfilActual IS NULL
            THROW 51101, 'USUARIO_NO_EXISTE', 1;
        IF @PerfilActual = @PerfilId
        BEGIN
            IF @TransaccionPropia = 1 COMMIT TRANSACTION;
            RETURN;
        END;
        IF @UsuarioId = @ActorId
            THROW 51106, 'AUTO_CAMBIO_PERFIL_NO_PERMITIDO', 1;
        IF NOT EXISTS (SELECT 1 FROM app24.PerfilApp WITH (UPDLOCK, HOLDLOCK) WHERE id = @PerfilId)
            THROW 51102, 'PERFIL_NO_EXISTE', 1;
        IF NOT EXISTS (SELECT 1 FROM app24.PerfilApp WITH (UPDLOCK, HOLDLOCK)
                       WHERE id = @PerfilId AND estado = 'ACTIVO')
            THROW 51103, 'PERFIL_INACTIVO', 1;

        UPDATE app24.UsuarioApp SET perfil_id = @PerfilId WHERE id = @UsuarioId;
        IF @@ROWCOUNT <> 1
            THROW 51150, 'FILAS_AFECTADAS_INCONSISTENTES', 1;

        IF NOT EXISTS (
            SELECT 1
            FROM app24.UsuarioApp u WITH (UPDLOCK, HOLDLOCK)
            JOIN app24.PerfilApp p WITH (UPDLOCK, HOLDLOCK) ON p.id = u.perfil_id
            WHERE u.estado = 'ACTIVO'
              AND (u.vigencia IS NULL OR u.vigencia >= @FechaActual)
              AND p.estado = 'ACTIVO'
              AND EXISTS (SELECT 1 FROM app24.PerfilActividad pa WITH (UPDLOCK, HOLDLOCK)
                          JOIN app24.Actividad a WITH (UPDLOCK, HOLDLOCK) ON a.id = pa.actividad_id
                          WHERE pa.perfil_id = p.id AND a.clave = 'USUARIOS_ADMINISTRAR')
              AND EXISTS (SELECT 1 FROM app24.PerfilActividad pa WITH (UPDLOCK, HOLDLOCK)
                          JOIN app24.Actividad a WITH (UPDLOCK, HOLDLOCK) ON a.id = pa.actividad_id
                          WHERE pa.perfil_id = p.id AND a.clave = 'PERFILES_ADMINISTRAR'))
            THROW 51107, 'SIN_ADMINISTRADOR_EFECTIVO', 1;

        IF @TransaccionPropia = 1 COMMIT TRANSACTION;
    END TRY
    BEGIN CATCH
        IF @TransaccionPropia = 1 AND XACT_STATE() <> 0 ROLLBACK TRANSACTION;
        THROW;
    END CATCH;
END;
GO
