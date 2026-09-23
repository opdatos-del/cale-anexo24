USE ANEXO24_DEV;
GO

CREATE OR ALTER PROCEDURE app24.APP24_C_USUARIO_RESTABLECER_PASSWORD
    @UsuarioId BIGINT,
    @PasswordHash VARCHAR(100)
AS
BEGIN
    SET NOCOUNT ON;
    SET XACT_ABORT ON;

    IF @UsuarioId IS NULL OR @UsuarioId <= 0
       OR @PasswordHash IS NULL OR LTRIM(RTRIM(@PasswordHash)) = ''
        THROW 51108, 'PARAMETRO_INVALIDO', 1;

    IF NOT EXISTS (SELECT 1 FROM app24.UsuarioApp WHERE id = @UsuarioId)
        THROW 51101, 'USUARIO_NO_EXISTE', 1;

    UPDATE app24.UsuarioApp
    SET password_hash = @PasswordHash
    WHERE id = @UsuarioId;

    DECLARE @FilasAfectadas INT = @@ROWCOUNT;
    IF @FilasAfectadas <> 1
        THROW 51150, 'FILAS_AFECTADAS_INCONSISTENTES', 1;
END;
GO
