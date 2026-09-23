USE ANEXO24_DEV;
GO

CREATE OR ALTER PROCEDURE app24.APP24_Q_USUARIO_POR_CLAVE
    @Clave VARCHAR(30)
AS
BEGIN
    SET NOCOUNT ON;

    SELECT
        id,
        clave,
        nombre,
        correo,
        password_hash,
        estado,
        vigencia,
        perfil_id
    FROM app24.UsuarioApp
    WHERE clave = @Clave;
END;
GO
