USE ANEXO24_DEV;
GO

CREATE OR ALTER PROCEDURE app24.APP24_C_PERFIL_CREAR
    @Nombre VARCHAR(80),
    @NuevoPerfilId BIGINT OUTPUT
AS
BEGIN
    SET NOCOUNT ON;
    SET XACT_ABORT ON;

    SET @Nombre = NULLIF(LTRIM(RTRIM(@Nombre)), '');
    IF @Nombre IS NULL
        THROW 51108, 'PARAMETRO_INVALIDO', 1;

    DECLARE @TransaccionPropia BIT = 0;
    BEGIN TRY
        IF @@TRANCOUNT = 0
        BEGIN
            SET @TransaccionPropia = 1;
            BEGIN TRANSACTION;
        END;

        IF EXISTS (SELECT 1 FROM app24.PerfilApp WITH (UPDLOCK, HOLDLOCK) WHERE nombre = @Nombre)
            THROW 51104, 'RECURSO_DUPLICADO', 1;

        BEGIN TRY
            INSERT INTO app24.PerfilApp (nombre, estado)
            VALUES (@Nombre, 'ACTIVO');
        END TRY
        BEGIN CATCH
            IF ERROR_NUMBER() IN (2601, 2627)
                THROW 51104, 'RECURSO_DUPLICADO', 1;
            THROW;
        END CATCH;

        SET @NuevoPerfilId = CONVERT(BIGINT, SCOPE_IDENTITY());
        IF @NuevoPerfilId IS NULL OR @NuevoPerfilId <= 0
            THROW 51150, 'FILAS_AFECTADAS_INCONSISTENTES', 1;

        IF @TransaccionPropia = 1 COMMIT TRANSACTION;
    END TRY
    BEGIN CATCH
        IF @TransaccionPropia = 1 AND XACT_STATE() <> 0 ROLLBACK TRANSACTION;
        THROW;
    END CATCH;
END;
GO
