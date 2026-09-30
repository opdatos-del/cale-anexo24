USE ANEXO24_DEV;
GO
CREATE OR ALTER PROCEDURE app24.APP24_Q_CATALOGO_PRODUCTO_CARGA_OBTENER @CargaId BIGINT, @Pagina INT=1, @Tamano INT=100 AS
BEGIN
 SET NOCOUNT ON; IF @CargaId IS NULL OR @CargaId<1 OR @Pagina<1 OR @Tamano<1 OR @Tamano>100 THROW 51304,'PARAMETRO_INVALIDO',1;
 SELECT id,archivo,hash,estado,total_filas,filas_validas,filas_invalidas,version_contrato,correlation_id FROM app24.CargaCatalogoProducto WHERE id=@CargaId;
 SELECT hoja,fila,datos_json FROM app24.CargaCatalogoProductoFila WHERE carga_id=@CargaId ORDER BY fila,id OFFSET (CONVERT(BIGINT,@Pagina)-1)*CONVERT(BIGINT,@Tamano) ROWS FETCH NEXT @Tamano ROWS ONLY;
 SELECT COUNT_BIG(1) AS total_filas FROM app24.CargaCatalogoProductoFila WHERE carga_id=@CargaId;
 SELECT hoja,fila,columna,valor_enmascarado,codigo,mensaje FROM app24.ErrorCargaProducto WHERE carga_id=@CargaId ORDER BY ISNULL(fila,0),id;
END;
GO
