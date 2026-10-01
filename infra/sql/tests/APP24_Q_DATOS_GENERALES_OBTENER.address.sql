-- Prueba SQL sintética read-only del formato de domicilio fiscal.
-- No crea tablas ni modifica CALE_IMMEX.

DECLARE @casos TABLE (
    caso varchar(40) NOT NULL,
    CalleNumero nvarchar(100) NULL,
    CalleNumeroInterior nvarchar(100) NULL,
    Colonia nvarchar(100) NULL,
    Municipio nvarchar(100) NULL,
    Entidad nvarchar(100) NULL,
    Codigopostal nvarchar(20) NULL,
    esperado nvarchar(600) NULL
);

INSERT INTO @casos VALUES
    ('completo', N' Calle 1 ', N' Int 2 ', N' Colonia ', N' Municipio ', N' Estado ', N' 12345 ', N'Calle 1, Int 2, Colonia, Municipio, Estado, 12345'),
    ('sin interior', N'Calle 1', NULL, N'Colonia', N'Municipio', N'Estado', N'12345', N'Calle 1, Colonia, Municipio, Estado, 12345'),
    ('sin colonia', N'Calle 1', N'Int 2', N'   ', N'Municipio', N'Estado', N'12345', N'Calle 1, Int 2, Municipio, Estado, 12345'),
    ('componentes NULL', NULL, NULL, NULL, NULL, NULL, NULL, NULL),
    ('componentes blank', N'  ', N' ', N' ', N'  ', N' ', N' ', NULL);

DECLARE @fallos int;

SELECT @fallos = COUNT(*)
FROM @casos c
CROSS APPLY (VALUES (NULLIF(CONCAT_WS(N', ',
    NULLIF(LTRIM(RTRIM(c.CalleNumero)), N''),
    NULLIF(LTRIM(RTRIM(c.CalleNumeroInterior)), N''),
    NULLIF(LTRIM(RTRIM(c.Colonia)), N''),
    NULLIF(LTRIM(RTRIM(c.Municipio)), N''),
    NULLIF(LTRIM(RTRIM(c.Entidad)), N''),
    NULLIF(LTRIM(RTRIM(c.Codigopostal)), N'')
), N''))) formatted(domicilio)
WHERE NOT ((formatted.domicilio = c.esperado) OR (formatted.domicilio IS NULL AND c.esperado IS NULL));

IF @fallos > 0
    THROW 50072, 'El formato sintético de domicilio fiscal no coincide.', 1;

SELECT 'GENERAL_DATA_ADDRESS_FORMATTING = PASS' AS resultado;
