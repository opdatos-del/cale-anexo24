USE CALE_IMMEX;
GO
CREATE OR ALTER PROCEDURE dbo.APP24_Q_PROVEEDORES_LISTAR
 @Filtro VARCHAR(250)=NULL,@Pagina INT=1,@Tamano INT=20,@Total BIGINT OUTPUT
AS
BEGIN
 SET NOCOUNT ON;
 IF @Pagina<1 OR @Tamano<1 OR @Tamano>100 THROW 50062,'Parámetros de paginación inválidos.',1;
 SET @Filtro=NULLIF(LTRIM(RTRIM(@Filtro)),'');
 SELECT @Total=COUNT_BIG(1) FROM dbo.Proveedores WHERE @Filtro IS NULL OR Clave LIKE '%'+@Filtro+'%' OR Nombre LIKE '%'+@Filtro+'%' OR Idfiscal LIKE '%'+@Filtro+'%';
 SELECT CONVERT(BIGINT,proveedorkey) proveedorkey,LTRIM(RTRIM(Clave)) clave,LTRIM(RTRIM(Nombre)) nombre,LTRIM(RTRIM(Idfiscal)) idfiscal,LTRIM(RTRIM(Pais)) pais,LTRIM(RTRIM(Correo)) correo
 FROM dbo.Proveedores WHERE @Filtro IS NULL OR Clave LIKE '%'+@Filtro+'%' OR Nombre LIKE '%'+@Filtro+'%' OR Idfiscal LIKE '%'+@Filtro+'%'
 ORDER BY Clave OFFSET (CAST(@Pagina AS BIGINT)-1)*CAST(@Tamano AS BIGINT) ROWS FETCH NEXT @Tamano ROWS ONLY;
END;
GO
