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
               nodo.value('(datos/Clave/text())[1]', 'varchar(50)') AS clave
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
      AND NOT EXISTS (SELECT 1 FROM dbo.PRODUCTOS AS p WHERE p.CVE_PRODUCTO = d.clave);
END;
GO
