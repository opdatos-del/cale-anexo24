-- Permite el estado terminal CONFIRMADA en app24.CargaPedimento.
-- No destructiva: amplía el CHECK existente y agrega fecha_confirmacion nullable.
-- No recrea la tabla, no borra filas, no hace backfill.

USE ANEXO24_DEV;
GO

IF EXISTS (
    SELECT 1
    FROM sys.check_constraints
    WHERE name = 'CK_CargaPedimento_estado'
      AND parent_object_id = OBJECT_ID('app24.CargaPedimento')
)
BEGIN
    ALTER TABLE app24.CargaPedimento DROP CONSTRAINT CK_CargaPedimento_estado;
END;
GO

ALTER TABLE app24.CargaPedimento WITH CHECK
    ADD CONSTRAINT CK_CargaPedimento_estado
    CHECK (estado IN ('PREVISUALIZADA', 'CON_ERRORES', 'CONFIRMADA'));
GO

IF COL_LENGTH('app24.CargaPedimento', 'fecha_confirmacion') IS NULL
BEGIN
    ALTER TABLE app24.CargaPedimento ADD fecha_confirmacion DATETIME2(3) NULL;
END;
GO

-- Bitácora: acciones controladas por el enum Java (no se insertan aquí).
-- PEDIMENTO_CONFIRMADO       (éxito, dentro de la transacción del command)
-- PEDIMENTO_CONFIRMACION_FALLIDA (fallo, registrado por el backend tras el rollback)
PRINT 'Migration 12-pedimento-confirmada-state aplicada.';
GO
