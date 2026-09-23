USE ANEXO24_DEV;
GO

CREATE OR ALTER PROCEDURE app24.APP24_Q_PERFILES_LISTAR
    @Nombre VARCHAR(80) = NULL,
    @Estado VARCHAR(20) = NULL,
    @Pagina INT = 1,
    @Tamano INT = 20,
    @Total BIGINT OUTPUT
AS
BEGIN
    SET NOCOUNT ON;

    IF @Pagina < 1
        THROW 50001, 'Pagina debe ser mayor o igual que 1.', 1;
    IF @Tamano < 1 OR @Tamano > 100
        THROW 50002, 'Tamano debe estar entre 1 y 100.', 1;

    DECLARE @NombrePatron VARCHAR(400) = NULL;
    IF @Nombre IS NOT NULL
    BEGIN
        SET @NombrePatron = '%' + REPLACE(REPLACE(REPLACE(@Nombre, '\', '\\'), '%', '\%'), '_', '\_') + '%';
    END;

    SELECT @Total = COUNT_BIG(*)
    FROM app24.PerfilApp p
    WHERE (@Estado IS NULL OR p.estado = @Estado)
      AND (@NombrePatron IS NULL OR LOWER(p.nombre) LIKE LOWER(@NombrePatron) ESCAPE '\');

    SELECT
        p.id,
        p.nombre,
        p.estado,
        COUNT_BIG(pa.actividad_id) AS cantidad_permisos
    FROM app24.PerfilApp p
    LEFT JOIN app24.PerfilActividad pa ON pa.perfil_id = p.id
    WHERE (@Estado IS NULL OR p.estado = @Estado)
      AND (@NombrePatron IS NULL OR LOWER(p.nombre) LIKE LOWER(@NombrePatron) ESCAPE '\')
    GROUP BY p.id, p.nombre, p.estado
    ORDER BY p.nombre ASC, p.id ASC
    OFFSET (CONVERT(BIGINT, @Pagina) - 1) * CONVERT(BIGINT, @Tamano) ROWS
    FETCH NEXT @Tamano ROWS ONLY;
END;
GO
