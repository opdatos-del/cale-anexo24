USE ANEXO24_DEV;
GO

CREATE OR ALTER PROCEDURE app24.APP24_Q_ACTIVIDADES_LISTAR
AS
BEGIN
    SET NOCOUNT ON;

    SELECT
        a.id,
        a.clave,
        a.nombre,
        a.recurso,
        a.accion
    FROM app24.Actividad a
    ORDER BY a.clave ASC, a.id ASC;
END;
GO
