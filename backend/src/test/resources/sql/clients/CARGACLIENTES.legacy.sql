/*
 * Réplica textual del SP legacy dbo.CARGACLIENTES auditado READ-ONLY
 * contra CALE_IMMEX LIVE el 2026-10-03. Se carga en SQL Testcontainers
 * para preservar el comportamiento real durante la prueba de aislamiento.
 *
 * Notas preservadas:
 *   * TRUNCATE TABLE ECARGACLIENTES al inicio.
 *   * INSERT-only en dbo.clientes: omite silenciosamente clientes cuya CLAVE
 *     ya existe y/o aparece en ECARGACLIENTES.
 *   * Validaciones: claves vacías, duplicados internos, IDFiscal vacío.
 *   * Sin NOLOCK ni READ UNCOMMITTED; sin SET TRANSACTION ISOLATION LEVEL custom.
 *   * Bug preservado: también inserta errores en ECARGAPROVEEDORES cuando el
 *     IDFiscal está vacío. El wrapper sólo replica la lógica legacy.
 */

USE [CALE_IMMEX];
GO

CREATE PROCEDURE dbo.CARGACLIENTES
AS
BEGIN
    SET NOCOUNT ON;

    TRUNCATE TABLE ECARGACLIENTES;

    DECLARE @PKEY BIGINT;
    SELECT @PKEY = ISNULL((SELECT MAX(CLIENTEKEY) FROM clientes),0) + 1;

    INSERT INTO ECARGACLIENTES (TMPKEY, ERROR, CLAVE)
    SELECT TMPCLIENTEKEY, 'EXISTEN CLAVES VACIAS', ''
    FROM TMPCLIENTES
    WHERE ISNULL(Clave,'') = '';

    INSERT INTO ECARGACLIENTES (TMPKEY, ERROR, CLAVE)
    SELECT TMPCLIENTEKEY, 'EXISTEN CLAVES DUPLICADAS', CLAVE
    FROM TMPCLIENTES
    WHERE CLAVE IN (SELECT CLAVE FROM TMPCLIENTES GROUP BY CLAVE HAVING COUNT(*) > 1);

    INSERT INTO ECARGACLIENTES (TMPKEY, ERROR, CLAVE)
    SELECT TMPCLIENTEKEY, 'EL ID FISCAL NO PUEDE IR VACIO', CLAVE
    FROM TMPCLIENTES
    WHERE ISNULL(Idfiscal,'') = '';

    INSERT INTO clientes (clientekey, Clave, Nombre, Idfiscal, Tipone, Programa, CalleNumero, Codigo, Colonia,
                           Entidad, Pais, Telefono, Correo, Fax, ApellidoPaterno, ApellidoMaterno,
                           calle, callenumerointerior, localidad, referencia, municipio,
                           tipoidentificador, codigopostal)
    SELECT ROW_NUMBER() OVER (ORDER BY CLAVE) + @PKEY,
           LEFT(Clave, 15),
           Nombre,
           Idfiscal,
           Tipone,
           Programa,
           CalleNumero,
           Codigo,
           Colonia,
           Entidad,
           Pais,
           Telefono,
           Correo,
           Fax,
           ApellidoPaterno,
           ApellidoMaterno,
           calle,
           callenumerointerior,
           localidad,
           referencia,
           municipio,
           tipoidentificador,
           codigopostal
    FROM TMPCLIENTES
    WHERE NOT EXISTS (SELECT 1 FROM ECARGACLIENTES E WHERE E.TMPKEY = TMPCLIENTES.TMPCLIENTEKEY)
      AND NOT EXISTS (SELECT 1 FROM clientes WHERE clientes.CLAVE = TMPCLIENTES.CLAVE);
END;
GO