USE ANEXO24_DEV;
GO

CREATE OR ALTER PROCEDURE app24.APP24_C_USUARIO_ACTUALIZAR_DATOS
    @UsuarioId BIGINT,
    @Nombre VARCHAR(120),
    @Correo VARCHAR(150)
AS
BEGIN
    SET NOCOUNT ON;
    SET XACT_ABORT ON;

    IF @UsuarioId IS NULL OR @UsuarioId <= 0
       OR @Nombre IS NULL OR LTRIM(RTRIM(@Nombre)) = ''
       OR @Correo IS NULL OR LTRIM(RTRIM(@Correo)) = ''
        THROW 51108, 'PARAMETRO_INVALIDO', 1;

    DECLARE @TransaccionPropia BIT = 0;
    DECLARE @FilasAfectadas INT;
    BEGIN TRY
        IF @@TRANCOUNT = 0
        BEGIN
            SET @TransaccionPropia = 1;
            BEGIN TRANSACTION;
        END;

        IF NOT EXISTS (SELECT 1 FROM app24.UsuarioApp WITH (UPDLOCK, HOLDLOCK) WHERE id = @UsuarioId)
            THROW 51101, 'USUARIO_NO_EXISTE', 1;
        IF EXISTS (SELECT 1 FROM app24.UsuarioApp WITH (UPDLOCK, HOLDLOCK)
                   WHERE correo = @Correo AND id <> @UsuarioId)
            THROW 51104, 'RECURSO_DUPLICADO', 1;

        BEGIN TRY
            UPDATE app24.UsuarioApp
            SET nombre = @Nombre, correo = @Correo
            WHERE id = @UsuarioId;
            SET @FilasAfectadas = @@ROWCOUNT;
        END TRY
        BEGIN CATCH
            IF ERROR_NUMBER() IN (2601, 2627)
                THROW 51104, 'RECURSO_DUPLICADO', 1;
            THROW;
        END CATCH;

        IF @FilasAfectadas <> 1
            THROW 51150, 'FILAS_AFECTADAS_INCONSISTENTES', 1;

        IF @TransaccionPropia = 1 COMMIT TRANSACTION;
    END TRY
    BEGIN CATCH
        IF @TransaccionPropia = 1 AND XACT_STATE() <> 0 ROLLBACK TRANSACTION;
        THROW;
    END CATCH;
END;
GO
