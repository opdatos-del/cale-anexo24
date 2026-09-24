USE ANEXO24_DEV;
GO

CREATE OR ALTER PROCEDURE app24.APP24_C_PERFIL_CAMBIAR_ESTADO
    @PerfilId BIGINT,
    @NuevoEstado VARCHAR(20),
    @FechaActual DATE
AS
BEGIN
    SET NOCOUNT ON;
    SET XACT_ABORT ON;

    IF @PerfilId IS NULL OR @PerfilId <= 0
       OR @FechaActual IS NULL
       OR @NuevoEstado IS NULL
       OR @NuevoEstado NOT IN ('ACTIVO', 'INACTIVO')
        THROW 51108, 'PARAMETRO_INVALIDO', 1;

    DECLARE @TransaccionPropia BIT = 0;
    DECLARE @EstadoActual VARCHAR(20);
    DECLARE @FilasAfectadas INT;
    BEGIN TRY
        IF @@TRANCOUNT = 0
        BEGIN
            SET @TransaccionPropia = 1;
            BEGIN TRANSACTION;
        END;

        SELECT @EstadoActual = estado
        FROM app24.PerfilApp WITH (UPDLOCK, HOLDLOCK)
        WHERE id = @PerfilId;
        IF @EstadoActual IS NULL
            THROW 51102, 'PERFIL_NO_EXISTE', 1;
        IF @EstadoActual = @NuevoEstado
        BEGIN
            IF @TransaccionPropia = 1 COMMIT TRANSACTION;
            RETURN;
        END;

        UPDATE app24.PerfilApp
        SET estado = @NuevoEstado
        WHERE id = @PerfilId;
        SET @FilasAfectadas = @@ROWCOUNT;
        IF @FilasAfectadas <> 1
            THROW 51150, 'FILAS_AFECTADAS_INCONSISTENTES', 1;

        IF @NuevoEstado = 'INACTIVO'
           AND NOT EXISTS (
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
