USE ANEXO24_DEV;
GO

CREATE OR ALTER PROCEDURE app24.APP24_Q_USUARIO_ACCESO
    @UsuarioId BIGINT
AS
BEGIN
    SET NOCOUNT ON;

    SELECT
        p.id AS perfil_id,
        p.estado AS perfil_estado,
        a.clave AS permiso
    FROM app24.UsuarioApp u
    JOIN app24.PerfilApp p ON p.id = u.perfil_id
    LEFT JOIN app24.PerfilActividad pa ON pa.perfil_id = p.id
    LEFT JOIN app24.Actividad a ON a.id = pa.actividad_id
    WHERE u.id = @UsuarioId
    ORDER BY a.clave ASC;
END;
GO
