USE CALE_IMMEX;
GO

CREATE OR ALTER PROCEDURE dbo.APP24_Q_DATOS_GENERALES_OBTENER
AS
BEGIN
    SET NOCOUNT ON;

    IF (SELECT COUNT_BIG(*) FROM dbo.DatosGenerales) > 1
    BEGIN
        THROW 50071, 'La fuente DatosGenerales debe contener como máximo un registro.', 1;
    END;

    SELECT
        NULLIF(LTRIM(RTRIM(Denominacion)), '') AS razon_social,
        NULLIF(LTRIM(RTRIM(Rfc)), '') AS rfc,
        NULLIF(LTRIM(RTRIM(RegistroIMMEX)), '') AS registro_immex,
        NULLIF(CONCAT_WS(', ',
            NULLIF(LTRIM(RTRIM(CalleNumero)), ''),
            NULLIF(LTRIM(RTRIM(CalleNumeroInterior)), ''),
            NULLIF(LTRIM(RTRIM(Colonia)), ''),
            NULLIF(LTRIM(RTRIM(Municipio)), ''),
            NULLIF(LTRIM(RTRIM(Entidad)), ''),
            NULLIF(LTRIM(RTRIM(Codigopostal)), '')
        ), '') AS domicilio_fiscal
    FROM dbo.DatosGenerales;
END;
GO
