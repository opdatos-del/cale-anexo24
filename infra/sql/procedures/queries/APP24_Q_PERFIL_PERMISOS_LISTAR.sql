USE ANEXO24_DEV;
GO

CREATE OR ALTER PROCEDURE app24.APP24_Q_PERFIL_PERMISOS_LISTAR
    @PerfilId BIGINT
AS
BEGIN
    SET NOCOUNT ON;

    IF @PerfilId IS NULL OR @PerfilId <= 0
        THROW 51108, 'PARAMETRO_INVALIDO', 1;

    SELECT
        p.id AS perfil_id,
        p.nombre AS perfil_nombre,
        p.estado AS perfil_estado,
        a.id AS actividad_id,
        a.clave,
        a.nombre,
        a.recurso,
        a.accion
    FROM app24.PerfilApp p
    LEFT JOIN app24.PerfilActividad pa ON pa.perfil_id = p.id
    LEFT JOIN app24.Actividad a ON a.id = pa.actividad_id
    WHERE p.id = @PerfilId
    ORDER BY a.clave ASC, a.id ASC;
END;
GO
