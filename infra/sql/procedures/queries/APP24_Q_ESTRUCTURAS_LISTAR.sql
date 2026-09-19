USE CALE_IMMEX;
GO

CREATE OR ALTER PROCEDURE dbo.APP24_Q_ESTRUCTURAS_LISTAR
    @Producto VARCHAR(50) = NULL,
    @Material VARCHAR(50) = NULL,
    @Pagina INT = 1,
    @Tamano INT = 20,
    @Total BIGINT OUTPUT
AS
BEGIN
    SET NOCOUNT ON;

    IF @Pagina IS NULL OR @Pagina < 1
       OR @Tamano IS NULL OR @Tamano < 1 OR @Tamano > 100
    BEGIN
        THROW 50001, 'Parámetros de paginación inválidos.', 1;
    END;

    SET @Producto = NULLIF(LTRIM(RTRIM(@Producto)), '');
    SET @Material = NULLIF(LTRIM(RTRIM(@Material)), '');

    SELECT @Total = COUNT_BIG(1)
    FROM dbo.productos AS p
    INNER JOIN dbo.estructuras AS e
        ON e.PRODUCTOLINK = p.PRODUCTOKEY
    INNER JOIN dbo.productomaterial AS pm
        ON pm.ESTRUCTURALINK = e.ESTRUCTURAKEY
    LEFT JOIN dbo.material AS m
        ON pm.CVE_MATERIAL = m.CLAVE
    WHERE (@Producto IS NULL OR p.CVE_PRODUCTO = @Producto)
      AND (@Material IS NULL OR pm.CVE_MATERIAL = @Material);

    SELECT
        e.ESTRUCTURAKEY AS ESTRUCTURAKEY,
        p.PRODUCTOKEY AS PRODUCTOKEY,
        p.CVE_PRODUCTO AS CVE_PRODUCTO,
        p.NOMBRE AS PRODUCTO_DESCRIPCION,
        p.UNIDAD AS PRODUCTO_UNIDAD,
        e.INICIO AS FECHA_INICIO,
        fechaFin.FECHA_FIN AS FECHA_FIN,
        pm.PRODMATKEY AS PRODMATKEY,
        pm.CVE_MATERIAL AS CVE_MATERIAL,
        m.DESCRIPCION AS MATERIAL_DESCRIPCION,
        m.UNIDAD AS MATERIAL_UNIDAD,
        m.FRACCION AS MATERIAL_FRACCION,
        pm.CANT_UTILIZADA AS CANT_UTILIZADA,
        pm.CANT_MERMADA AS CANT_MERMADA,
        pm.CANT_DESPERDICIADA AS CANT_DESPERDICIADA
    FROM dbo.productos AS p
    INNER JOIN dbo.estructuras AS e
        ON e.PRODUCTOLINK = p.PRODUCTOKEY
    INNER JOIN dbo.productomaterial AS pm
        ON pm.ESTRUCTURALINK = e.ESTRUCTURAKEY
    LEFT JOIN dbo.material AS m
        ON pm.CVE_MATERIAL = m.CLAVE
    OUTER APPLY (
        SELECT TOP (1)
            eSiguiente.INICIO AS FECHA_FIN
        FROM dbo.estructuras AS eSiguiente
        WHERE eSiguiente.PRODUCTOLINK = e.PRODUCTOLINK
          AND eSiguiente.INICIO > e.INICIO
        ORDER BY eSiguiente.INICIO ASC, eSiguiente.ESTRUCTURAKEY ASC
    ) AS fechaFin
    WHERE (@Producto IS NULL OR p.CVE_PRODUCTO = @Producto)
      AND (@Material IS NULL OR pm.CVE_MATERIAL = @Material)
    ORDER BY
        p.PRODUCTOKEY,
        e.INICIO,
        e.ESTRUCTURAKEY,
        pm.PRODMATKEY
    OFFSET (CAST(@Pagina AS BIGINT) - 1) * CAST(@Tamano AS BIGINT) ROWS
    FETCH NEXT @Tamano ROWS ONLY;
END;
GO
