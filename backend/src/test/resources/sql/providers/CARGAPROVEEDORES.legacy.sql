/*
 * Réplica textual del SP legacy dbo.CARGAPROVEEDORES auditado READ-ONLY
 * contra CALE_IMMEX LIVE el 2026-10-03. Se carga en SQL Testcontainers
 * para preservar el comportamiento real durante la prueba de aislamiento.
 *
 * Notas preservadas:
 *   * TRUNCATE TABLE ECARGAPROVEEDORES al inicio.
 *   * INSERT-only en dbo.Proveedores: omite silenciosamente proveedores cuya
 *     CLAVE ya existe y/o aparece en ECARGAPROVEEDORES.
 *   * Validaciones: claves vacías, duplicados internos, IDFiscal vacío.
 *   * ECARGAPROVEEDORES es la SHARED_LEGACY_STAGE: dbo.CARGACLIENTES también
 *     escribe ahí (bug legacy) y el módulo Proveedores debe aislarla.
 *   * Sin NOLOCK ni READ UNCOMMITTED; sin SET TRANSACTION ISOLATION LEVEL custom.
 */

USE [CALE_IMMEX];
GO

CREATE PROCEDURE dbo.CARGAPROVEEDORES
AS
BEGIN
    SET NOCOUNT ON;

    TRUNCATE TABLE ECARGAPROVEEDORES;

    DECLARE @PKEY BIGINT;
    SELECT @PKEY = ISNULL((SELECT MAX(PROVEEDORKEY) FROM PROVEEDORES),0) + 1;

    INSERT INTO ECARGAPROVEEDORES (TMPKEY, ERROR, CLAVE)
    SELECT TMPPROVEEDORKEY, 'EXISTEN CLAVES VACIAS', ''
    FROM TMPPROVEEDORES
    WHERE ISNULL(Clave,'') = '';

    INSERT INTO ECARGAPROVEEDORES (TMPKEY, ERROR, CLAVE)
    SELECT TMPPROVEEDORKEY, 'EXISTEN CLAVES DUPLICADAS', CLAVE
    FROM TMPPROVEEDORES
    WHERE CLAVE IN (SELECT CLAVE FROM TMPPROVEEDORES GROUP BY CLAVE HAVING COUNT(*) > 1);

    INSERT INTO ECARGAPROVEEDORES (TMPKEY, ERROR, CLAVE)
    SELECT TMPPROVEEDORKEY, 'EL ID FISCAL NO PUEDE IR VACIO', CLAVE
    FROM TMPPROVEEDORES
    WHERE ISNULL(Idfiscal,'') = '';

    INSERT INTO Proveedores (proveedorkey, Clave, Nombre, Idfiscal, Tipone, Programa, CalleNumero, Codigo, Colonia,
                             Entidad, Pais, Telefono, Correo, Fax, ALMACENKEY, ApellidoPaterno,
                             ApellidoMaterno, calle, callenumerointerior,
                             localidad, referencia, municipio,
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
           ALMACENKEY,
           ApellidoPaterno,
           ApellidoMaterno,
           calle,
           callenumerointerior,
           localidad,
           referencia,
           municipio,
           tipoidentificador,
           codigopostal
    FROM TMPPROVEEDORES
    WHERE NOT EXISTS (SELECT 1 FROM ECARGAPROVEEDORES E WHERE E.TMPKEY = TMPPROVEEDORES.TMPPROVEEDORKEY)
      AND NOT EXISTS (SELECT 1 FROM PROVEEDORES WHERE PROVEEDORES.CLAVE = TMPPROVEEDORES.CLAVE);
END;
GO