USE ANEXO24_DEV;
GO

CREATE OR ALTER PROCEDURE app24.APP24_Q_FACTURACION_CARGA_OBTENER
    @CargaId BIGINT,
    @Pagina INT,
    @Tamano INT
AS
BEGIN
    SET NOCOUNT ON;
    IF @CargaId IS NULL OR @CargaId <= 0 OR @Pagina < 1 OR @Tamano < 1 OR @Tamano > 100
        THROW 51108, 'PARAMETRO_INVALIDO', 1;

    SELECT id, archivo, hash, estado, total_registros, registros_validos, registros_invalidos
    FROM app24.CargaFacturacion WHERE id = @CargaId;

    SELECT hoja, fila, datos_json
    FROM app24.CargaFacturacionFila
    WHERE carga_id = @CargaId
    ORDER BY fila
    OFFSET (@Pagina - 1) * @Tamano ROWS FETCH NEXT @Tamano ROWS ONLY;

    SELECT COUNT_BIG(*) AS total_filas
    FROM app24.CargaFacturacionFila WHERE carga_id = @CargaId;

    SELECT hoja, fila, columna, valor, regla, mensaje
    FROM app24.ErrorCarga WHERE carga_id = @CargaId ORDER BY id;
END;
GO
