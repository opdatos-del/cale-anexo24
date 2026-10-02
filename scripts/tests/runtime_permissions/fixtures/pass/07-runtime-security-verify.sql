-- Fixture pass: listas del verificador alineadas con el fixture Java.
DECLARE @esperado TABLE (esquema SYSNAME, objeto SYSNAME);
INSERT INTO @esperado (esquema, objeto) VALUES
    ('app24', 'APP24_Q_DEMO_OBTENER'),
    ('app24', 'APP24_Q_DEMO_ERRORES'),
    ('dbo', 'APP24_Q_DEMO_LISTAR');
GO
