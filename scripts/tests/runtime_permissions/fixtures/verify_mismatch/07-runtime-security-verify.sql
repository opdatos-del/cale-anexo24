-- Fixture verify_mismatch: contrato desalineado con el fixture Java.
DECLARE @esperado TABLE (esquema SYSNAME, objeto SYSNAME);
INSERT INTO @esperado (esquema, objeto) VALUES
    ('app24', 'APP24_Q_DEMO_OTRO');
GO
