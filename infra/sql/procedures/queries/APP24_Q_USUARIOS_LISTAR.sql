USE ANEXO24_DEV;
GO

CREATE OR ALTER PROCEDURE app24.APP24_Q_USUARIOS_LISTAR
    @Clave VARCHAR(30) = NULL,
    @Nombre VARCHAR(120) = NULL,
    @Correo VARCHAR(150) = NULL,
    @Estado VARCHAR(20) = NULL,
    @PerfilId BIGINT = NULL,
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
    FROM app24.UsuarioApp u
    JOIN app24.PerfilApp p ON p.id = u.perfil_id
    WHERE (@Clave IS NULL OR u.clave = @Clave)
      AND (@Correo IS NULL OR u.correo = @Correo)
      AND (@Estado IS NULL OR u.estado = @Estado)
      AND (@PerfilId IS NULL OR u.perfil_id = @PerfilId)
      AND (@NombrePatron IS NULL OR LOWER(u.nombre) LIKE LOWER(@NombrePatron) ESCAPE '\');

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
    WHERE (@Clave IS NULL OR u.clave = @Clave)
      AND (@Correo IS NULL OR u.correo = @Correo)
      AND (@Estado IS NULL OR u.estado = @Estado)
      AND (@PerfilId IS NULL OR u.perfil_id = @PerfilId)
      AND (@NombrePatron IS NULL OR LOWER(u.nombre) LIKE LOWER(@NombrePatron) ESCAPE '\')
    ORDER BY u.clave ASC, u.id ASC
    OFFSET (CONVERT(BIGINT, @Pagina) - 1) * CONVERT(BIGINT, @Tamano) ROWS
    FETCH NEXT @Tamano ROWS ONLY;
END;
GO
