USE ANEXO24_DEV;
GO

CREATE OR ALTER PROCEDURE app24.APP24_Q_PEDIMENTO_CARGA_ERRORES
    @CargaId BIGINT,
    @Pagina INT = 1,
    @Tamano INT = 100
AS
BEGIN
    SET NOCOUNT ON;
    IF @CargaId IS NULL OR @CargaId < 1 OR @Pagina < 1 OR @Tamano < 1 OR @Tamano > 100
        THROW 51203, 'PARAMETRO_INVALIDO', 1;
    SELECT hoja, fila, columna, valor_enmascarado, codigo, mensaje
    FROM app24.ErrorCargaPedimento WHERE carga_id = @CargaId
    ORDER BY ISNULL(fila, 0), id
    OFFSET (CONVERT(BIGINT, @Pagina) - 1) * CONVERT(BIGINT, @Tamano) ROWS FETCH NEXT @Tamano ROWS ONLY;
END;
GO
