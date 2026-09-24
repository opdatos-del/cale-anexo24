USE ANEXO24_DEV;
GO

CREATE OR ALTER PROCEDURE app24.APP24_C_PERFIL_ACTUALIZAR_NOMBRE
    @PerfilId BIGINT,
    @Nombre VARCHAR(80)
AS
BEGIN
    SET NOCOUNT ON;
    SET XACT_ABORT ON;

    SET @Nombre = NULLIF(LTRIM(RTRIM(@Nombre)), '');
    IF @PerfilId IS NULL OR @PerfilId <= 0 OR @Nombre IS NULL
        THROW 51108, 'PARAMETRO_INVALIDO', 1;

    DECLARE @TransaccionPropia BIT = 0;
    DECLARE @NombreActual VARCHAR(80);
    DECLARE @FilasAfectadas INT;
    BEGIN TRY
        IF @@TRANCOUNT = 0
        BEGIN
            SET @TransaccionPropia = 1;
            BEGIN TRANSACTION;
        END;

        SELECT @NombreActual = nombre
        FROM app24.PerfilApp WITH (UPDLOCK, HOLDLOCK)
        WHERE id = @PerfilId;
        IF @NombreActual IS NULL
            THROW 51102, 'PERFIL_NO_EXISTE', 1;
        IF @NombreActual = @Nombre
        BEGIN
            IF @TransaccionPropia = 1 COMMIT TRANSACTION;
            RETURN;
        END;

        IF EXISTS (SELECT 1 FROM app24.PerfilApp WITH (UPDLOCK, HOLDLOCK)
                   WHERE nombre = @Nombre AND id <> @PerfilId)
            THROW 51104, 'RECURSO_DUPLICADO', 1;

        BEGIN TRY
            UPDATE app24.PerfilApp
            SET nombre = @Nombre
            WHERE id = @PerfilId;
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
