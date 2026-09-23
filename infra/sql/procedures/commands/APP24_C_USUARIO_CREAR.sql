USE ANEXO24_DEV;
GO

CREATE OR ALTER PROCEDURE app24.APP24_C_USUARIO_CREAR
    @Clave VARCHAR(30),
    @Nombre VARCHAR(120),
    @Correo VARCHAR(150),
    @PasswordHash VARCHAR(100),
    @Vigencia DATE = NULL,
    @PerfilId BIGINT,
    @NuevoUsuarioId BIGINT OUTPUT
AS
BEGIN
    SET NOCOUNT ON;
    SET XACT_ABORT ON;

    IF @Clave IS NULL OR LTRIM(RTRIM(@Clave)) = ''
       OR @Nombre IS NULL OR LTRIM(RTRIM(@Nombre)) = ''
       OR @Correo IS NULL OR LTRIM(RTRIM(@Correo)) = ''
       OR @PasswordHash IS NULL OR LTRIM(RTRIM(@PasswordHash)) = ''
       OR @PerfilId IS NULL OR @PerfilId <= 0
        THROW 51108, 'PARAMETRO_INVALIDO', 1;

    DECLARE @TransaccionPropia BIT = 0;
    BEGIN TRY
        IF @@TRANCOUNT = 0
        BEGIN
            SET @TransaccionPropia = 1;
            BEGIN TRANSACTION;
        END;

        IF NOT EXISTS (SELECT 1 FROM app24.PerfilApp WITH (UPDLOCK, HOLDLOCK) WHERE id = @PerfilId)
            THROW 51102, 'PERFIL_NO_EXISTE', 1;
        IF NOT EXISTS (SELECT 1 FROM app24.PerfilApp WITH (UPDLOCK, HOLDLOCK)
                       WHERE id = @PerfilId AND estado = 'ACTIVO')
            THROW 51103, 'PERFIL_INACTIVO', 1;
        IF EXISTS (SELECT 1 FROM app24.UsuarioApp WITH (UPDLOCK, HOLDLOCK) WHERE clave = @Clave)
           OR EXISTS (SELECT 1 FROM app24.UsuarioApp WITH (UPDLOCK, HOLDLOCK) WHERE correo = @Correo)
            THROW 51104, 'RECURSO_DUPLICADO', 1;

        BEGIN TRY
            INSERT INTO app24.UsuarioApp (clave, nombre, correo, password_hash, estado, vigencia, perfil_id)
            VALUES (@Clave, @Nombre, @Correo, @PasswordHash, 'ACTIVO', @Vigencia, @PerfilId);
        END TRY
        BEGIN CATCH
            IF ERROR_NUMBER() IN (2601, 2627)
                THROW 51104, 'RECURSO_DUPLICADO', 1;
            THROW;
        END CATCH;

        SET @NuevoUsuarioId = CONVERT(BIGINT, SCOPE_IDENTITY());
        IF @NuevoUsuarioId IS NULL OR @NuevoUsuarioId <= 0
            THROW 51150, 'FILAS_AFECTADAS_INCONSISTENTES', 1;

        IF @TransaccionPropia = 1 COMMIT TRANSACTION;
    END TRY
    BEGIN CATCH
        IF @TransaccionPropia = 1 AND XACT_STATE() <> 0 ROLLBACK TRANSACTION;
        THROW;
    END CATCH;
END;
GO
