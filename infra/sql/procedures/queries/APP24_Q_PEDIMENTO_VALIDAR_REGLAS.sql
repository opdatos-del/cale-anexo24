USE [CALE_IMMEX];
GO
SET ANSI_NULLS ON;
GO
SET QUOTED_IDENTIFIER ON;
GO

CREATE OR ALTER PROCEDURE dbo.APP24_Q_PEDIMENTO_VALIDAR_REGLAS
    @FilasXml XML
AS
BEGIN
    SET NOCOUNT ON;

    ;WITH Datos AS (
        SELECT nodo.value('@hoja', 'varchar(80)') AS hoja,
               nodo.value('@fila', 'int') AS fila,
               nodo.value('(datos/UnidadComercial/text())[1]', 'varchar(50)') AS unidad_comercial,
               nodo.value('(datos/UnidadTarifa/text())[1]', 'varchar(50)') AS unidad_tarifa,
               nodo.value('(datos/TipoOperacion/text())[1]', 'varchar(10)') AS tipo_operacion,
               nodo.value('(datos/Clave/text())[1]', 'varchar(50)') AS clave,
               nodo.value('(datos/NumeroPedimento/text())[1]', 'varchar(50)') AS numero_pedimento
        FROM @FilasXml.nodes('/filas/fila') AS filas(nodo)
    )
    SELECT hoja, fila, 'UnidadComercial' AS columna, 'PED-001' AS codigo,
           'La unidad comercial no existe en el catalogo autorizado.' AS mensaje
    FROM Datos
    WHERE NULLIF(LTRIM(RTRIM(unidad_comercial)), '') IS NOT NULL
      AND ISNULL(dbo.ISVALIDUNIT(unidad_comercial), 0) = 0

    UNION ALL

    SELECT hoja, fila, 'UnidadTarifa', 'PED-002',
           'La unidad tarifaria no existe en el catalogo autorizado.'
    FROM Datos
    WHERE NULLIF(LTRIM(RTRIM(unidad_tarifa)), '') IS NOT NULL
      AND ISNULL(dbo.ISVALIDUNIT(unidad_tarifa), 0) = 0

    UNION ALL

    SELECT hoja, fila, 'Clave', 'PED-003',
           'El material no existe en el catalogo.'
    FROM Datos AS d
    WHERE d.tipo_operacion = '1'
      AND NULLIF(LTRIM(RTRIM(d.clave)), '') IS NOT NULL
      AND NOT EXISTS (SELECT 1 FROM dbo.MATERIAL AS m WHERE m.CLAVE = d.clave)

    UNION ALL

    SELECT hoja, fila, 'Clave', 'PED-004',
           'El producto no existe en el catalogo.'
    FROM Datos AS d
    WHERE d.tipo_operacion = '2'
      AND NULLIF(LTRIM(RTRIM(d.clave)), '') IS NOT NULL
      AND NOT EXISTS (SELECT 1 FROM dbo.PRODUCTOS AS p WHERE p.CVE_PRODUCTO = d.clave)

    UNION ALL

    -- PED-007 (importacion): replica la guardia legacy de duplicado operativo,
    -- NUMEROPEDIMENTO vs IMPORTACIONES.NUMERO_PED, pero emite error explicito en
    -- lugar de omitir la fila en silencio. NULL-safe via EXISTS.
    SELECT hoja, fila, 'NumeroPedimento', 'PED-007',
           'El pedimento ya existe en las operaciones de importacion.'
    FROM Datos AS d
    WHERE d.tipo_operacion = '1'
      AND NULLIF(LTRIM(RTRIM(d.numero_pedimento)), '') IS NOT NULL
      AND EXISTS (SELECT 1 FROM dbo.IMPORTACIONES AS i WHERE i.NUMERO_PED = d.numero_pedimento)

    UNION ALL

    -- PED-007 (exportacion/salida): guardia legacy DOCUMENTO vs SALIDAS.DOCUMENTO.
    SELECT hoja, fila, 'NumeroPedimento', 'PED-007',
           'El pedimento ya existe en las operaciones de salida.'
    FROM Datos AS d
    WHERE d.tipo_operacion = '2'
      AND NULLIF(LTRIM(RTRIM(d.numero_pedimento)), '') IS NOT NULL
      AND EXISTS (SELECT 1 FROM dbo.SALIDAS AS s WHERE s.DOCUMENTO = d.numero_pedimento);
END;
GO
