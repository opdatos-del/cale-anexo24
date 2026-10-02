-- Fixture table_grant: el DML directo debe fallar el gate.
GRANT EXECUTE ON OBJECT::app24.APP24_Q_DEMO_OBTENER TO app24_runtime;
GRANT EXECUTE ON OBJECT::app24.APP24_Q_DEMO_ERRORES TO app24_runtime;
GRANT SELECT ON OBJECT::app24.UsuarioApp TO app24_runtime;
GO
