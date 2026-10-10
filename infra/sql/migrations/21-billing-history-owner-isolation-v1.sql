-- ANEXO24_DEV: owner isolation y recuperación de cargas de facturación.
-- Sin cambios de tablas, FKs ni índices; sólo contratos query existentes/nuevos.

USE ANEXO24_DEV;
GO

CREATE OR ALTER PROCEDURE app24.APP24_Q_FACTURACION_CARGAS_LISTAR
    @UsuarioId BIGINT,
    @Estado VARCHAR(20) = NULL,
    @Desde DATE = NULL,
    @Hasta DATE = NULL,
    @Pagina INT = 1,
    @Tamano INT = 20,
    @Total BIGINT OUTPUT
AS
BEGIN
    SET NOCOUNT ON;
    SET @Estado = NULLIF(LTRIM(RTRIM(@Estado)), '');
    IF @UsuarioId IS NULL OR NOT (@UsuarioId > 0) OR NOT (@Pagina >= 1) OR NOT (@Tamano BETWEEN 1 AND 100)
       OR (@Estado IS NOT NULL AND @Estado NOT IN ('PREVISUALIZADA', 'INVALIDA'))
       OR (@Desde IS NULL AND @Hasta IS NOT NULL) OR (@Desde IS NOT NULL AND @Hasta IS NULL)
       OR (@Desde IS NOT NULL AND @Desde > @Hasta)
        THROW 51108, 'PARAMETRO_INVALIDO', 1;

    SELECT @Total = COUNT_BIG(*)
    FROM app24.CargaFacturacion
    WHERE usuario_id = @UsuarioId
      AND (@Estado IS NULL OR estado = @Estado)
      AND (@Desde IS NULL OR CONVERT(DATE, fecha) >= @Desde)
      AND (@Hasta IS NULL OR CONVERT(DATE, fecha) <= @Hasta);

    SELECT id, archivo, hash, fecha, estado, total_registros, registros_validos, registros_invalidos
    FROM app24.CargaFacturacion
    WHERE usuario_id = @UsuarioId
      AND (@Estado IS NULL OR estado = @Estado)
      AND (@Desde IS NULL OR CONVERT(DATE, fecha) >= @Desde)
      AND (@Hasta IS NULL OR CONVERT(DATE, fecha) <= @Hasta)
    ORDER BY fecha DESC, id DESC
    OFFSET (CONVERT(BIGINT, @Pagina) - 1) * @Tamano ROWS FETCH NEXT @Tamano ROWS ONLY;
END;
GO

USE ANEXO24_DEV;
GO

CREATE OR ALTER PROCEDURE app24.APP24_Q_FACTURACION_CARGA_OBTENER
    @CargaId BIGINT,
    @UsuarioId BIGINT,
    @Pagina INT,
    @Tamano INT
AS
BEGIN
    SET NOCOUNT ON;
    IF @CargaId IS NULL OR @CargaId <= 0 OR @UsuarioId IS NULL OR @UsuarioId <= 0
       OR @Pagina < 1 OR @Tamano < 1 OR @Tamano > 100
        THROW 51108, 'PARAMETRO_INVALIDO', 1;

    SELECT id, archivo, hash, estado, total_registros, registros_validos, registros_invalidos
    FROM app24.CargaFacturacion
    WHERE id = @CargaId AND usuario_id = @UsuarioId;

    SELECT hoja, fila, datos_json
    FROM app24.CargaFacturacionFila
    WHERE carga_id = @CargaId
      AND EXISTS (SELECT 1 FROM app24.CargaFacturacion WHERE id = @CargaId AND usuario_id = @UsuarioId)
    ORDER BY fila, id
    OFFSET (CONVERT(BIGINT, @Pagina) - 1) * @Tamano ROWS FETCH NEXT @Tamano ROWS ONLY;

    SELECT COUNT_BIG(*) AS total_filas
    FROM app24.CargaFacturacionFila
    WHERE carga_id = @CargaId
      AND EXISTS (SELECT 1 FROM app24.CargaFacturacion WHERE id = @CargaId AND usuario_id = @UsuarioId);

    SELECT hoja, fila, columna, valor, regla, mensaje
    FROM app24.ErrorCarga
    WHERE carga_id = @CargaId
      AND EXISTS (SELECT 1 FROM app24.CargaFacturacion WHERE id = @CargaId AND usuario_id = @UsuarioId)
    ORDER BY id;
END;
GO

GRANT EXECUTE ON OBJECT::app24.APP24_Q_FACTURACION_CARGAS_LISTAR TO app24_runtime;
GO

PRINT 'Migration 21-billing-history-owner-isolation-v1 aplicada.';
GO
