-- Fixture schema_grant: el grant por schema debe fallar el gate.
GRANT EXECUTE ON OBJECT::app24.APP24_Q_DEMO_OBTENER TO app24_runtime;
GRANT EXECUTE ON OBJECT::app24.APP24_Q_DEMO_ERRORES TO app24_runtime;
GRANT EXECUTE ON SCHEMA::app24 TO app24_runtime;
GO
