USE ANEXO24_DEV;
GO

CREATE OR ALTER PROCEDURE app24.APP24_Q_USUARIO_OBTENER
    @UsuarioId BIGINT
AS
BEGIN
    SET NOCOUNT ON;

    SELECT
        u.id,
        u.clave,
        u.nombre,
        u.correo,
        u.estado,
        u.vigencia,
        u.perfil_id,
        p.nombre AS perfil_nombre
    FROM app24.UsuarioApp u
    JOIN app24.PerfilApp p ON p.id = u.perfil_id
    WHERE u.id = @UsuarioId;
END;
GO
