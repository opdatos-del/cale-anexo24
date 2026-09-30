USE ANEXO24_DEV;
GO
CREATE OR ALTER PROCEDURE app24.APP24_C_CATALOGO_MATERIAL_CARGA_CREAR
 @Archivo VARCHAR(255),@Hash VARCHAR(64),@UsuarioId BIGINT,@Estado VARCHAR(20),@TotalFilas INT,@FilasValidas INT,
 @VersionContrato VARCHAR(60),@CorrelationId VARCHAR(40),@FilasJson NVARCHAR(MAX),@ErroresJson NVARCHAR(MAX),@CargaId BIGINT OUTPUT
AS
BEGIN
 SET NOCOUNT ON; SET XACT_ABORT ON;
 IF @Archivo IS NULL OR @Hash IS NULL OR LEN(@Hash)<>64 OR @UsuarioId IS NULL OR @Estado NOT IN('PREVISUALIZADA','CON_ERRORES') OR @TotalFilas<0 OR @FilasValidas<0 OR @FilasValidas>@TotalFilas OR @VersionContrato IS NULL OR @CorrelationId IS NULL OR ISJSON(@FilasJson)<>1 OR ISJSON(@ErroresJson)<>1 THROW 51301,'PARAMETRO_INVALIDO',1;
 BEGIN TRY
  BEGIN TRANSACTION;
  INSERT INTO app24.CargaCatalogoMaterial(archivo,hash,usuario_id,estado,total_filas,filas_validas,filas_invalidas,version_contrato,correlation_id) VALUES(@Archivo,@Hash,@UsuarioId,@Estado,@TotalFilas,@FilasValidas,@TotalFilas-@FilasValidas,@VersionContrato,@CorrelationId);
  SET @CargaId=CONVERT(BIGINT,SCOPE_IDENTITY());
  INSERT INTO app24.CargaCatalogoMaterialFila(carga_id,hoja,fila,datos_json) SELECT @CargaId,hoja,fila,datos FROM OPENJSON(@FilasJson) WITH(hoja VARCHAR(80) '$.hoja',fila INT '$.fila',datos NVARCHAR(MAX) '$.datos' AS JSON);
  INSERT INTO app24.ErrorCargaMaterial(carga_id,hoja,fila,columna,valor_enmascarado,codigo,mensaje) SELECT @CargaId,hoja,fila,columna,valor_enmascarado,codigo,mensaje FROM OPENJSON(@ErroresJson) WITH(hoja VARCHAR(80) '$.hoja',fila INT '$.fila',columna VARCHAR(80) '$.columna',valor_enmascarado VARCHAR(80) '$.valorEnmascarado',codigo VARCHAR(120) '$.codigo',mensaje VARCHAR(500) '$.mensaje');
  DECLARE @Accion VARCHAR(40)=CASE WHEN @Estado='PREVISUALIZADA' THEN 'CARGA_VALIDADA' ELSE 'CARGA_CON_ERRORES' END,@Resultado VARCHAR(20)=CASE WHEN @Estado='PREVISUALIZADA' THEN 'EXITO' ELSE 'FALLO' END,@Detalle VARCHAR(500)=CONCAT('materiales carga=',@CargaId),@EventoId BIGINT;
  EXEC app24.APP24_C_BITACORA_REGISTRAR @UsuarioId=@UsuarioId,@Modulo='CATALOGOS',@Accion=@Accion,@Detalle=@Detalle,@CorrelacionId=@CorrelationId,@Resultado=@Resultado,@EventoId=@EventoId OUTPUT;
  COMMIT TRANSACTION;
 END TRY BEGIN CATCH IF XACT_STATE()<>0 ROLLBACK TRANSACTION; THROW; END CATCH;
END;
GO
GRANT EXECUTE ON OBJECT::app24.APP24_C_CATALOGO_MATERIAL_CARGA_CREAR TO app24_runtime;
GO
