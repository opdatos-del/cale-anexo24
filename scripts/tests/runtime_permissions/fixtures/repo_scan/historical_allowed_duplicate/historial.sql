-- Duplicado histórico permitido: SPs canónicos del fixture Java + membership runtime.
GRANT EXECUTE ON OBJECT::app24.APP24_Q_DEMO_OBTENER TO app24_runtime;
GRANT EXECUTE ON OBJECT::app24.APP24_Q_DEMO_ERRORES TO app24_runtime;
GRANT EXECUTE ON OBJECT::dbo.APP24_Q_DEMO_LISTAR TO cale_immex_runtime;
ALTER ROLE app24_runtime ADD MEMBER anexo24_app;
GO
