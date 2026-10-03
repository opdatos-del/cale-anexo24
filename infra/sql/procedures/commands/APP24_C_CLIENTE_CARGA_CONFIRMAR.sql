USE [CALE_IMMEX];
GO
SET ANSI_NULLS ON;
GO
SET QUOTED_IDENTIFIER ON;
GO

-- Confirmación técnica de una carga de clientes mediante el staging legacy existente.
-- El catálogo autoritativo continúa siendo dbo.clientes y sólo dbo.CARGACLIENTES
-- aplica sus reglas de negocio. No crea tablas ni lógica paralela de catálogo.
--
-- Comportamiento confirmado del legacy (audit READ ONLY 2026-10-03):
--   * TRUNCATE TABLE ECARGACLIENTES al inicio (sin transacción, sin TRY/CATCH).
--   * INSERT-only en dbo.clientes: omite silenciosamente clientes cuya CLAVE
--     ya existe y/o aparece en ECARGACLIENTES.
--   * Validaciones: claves vacías, duplicados internos, IDFiscal vacío.
--   * Sin NOLOCK ni READ UNCOMMITTED; sin SET TRANSACTION ISOLATION LEVEL custom.
--   * Bug documentado — inserta errores también en ECARGAPROVEEDORES cuando
--     el IDFiscal está vacío. No se corrige: el wrapper sólo replica.
CREATE OR ALTER PROCEDURE dbo.APP24_C_CLIENTE_CARGA_CONFIRMAR
    @CargaId BIGINT
AS
BEGIN
    SET NOCOUNT ON;
    SET XACT_ABORT ON;

    IF @CargaId IS NULL OR @CargaId < 1
        THROW 51501, 'PARAMETRO_INVALIDO', 1;

    DECLARE @Estado VARCHAR(20),
            @UsuarioId BIGINT,
            @CorrelacionId VARCHAR(40),
            @ConfirmadaEn DATETIME2(3),
            @Bloqueo BIGINT,
            @TotalFilas INT,
            @FilasConError INT,
            @DetalleBitacora VARCHAR(500),
            @EventoId BIGINT;
    DECLARE @Mapa TABLE (
        carga_cliente_key BIGINT NOT NULL,
        hoja VARCHAR(80) NOT NULL,
        fila INT NOT NULL
    );
    DECLARE @Errores TABLE (
        hoja VARCHAR(80) NOT NULL,
        fila INT NOT NULL,
        codigo VARCHAR(120) NOT NULL,
        mensaje VARCHAR(500) NOT NULL
    );

    BEGIN TRY
        BEGIN TRANSACTION;

        -- Coordina tanto inserciones directas como ejecuciones no coordinadas de CARGACLIENTES.
        SELECT TOP (1) @Bloqueo = TMPCLIENTEKEY
        FROM dbo.TMPCLIENTES WITH (TABLOCKX, HOLDLOCK);
        SELECT TOP (1) @Bloqueo = EPKEY
        FROM dbo.ECARGACLIENTES WITH (TABLOCKX, HOLDLOCK);

        IF EXISTS (SELECT 1 FROM dbo.TMPCLIENTES)
           OR EXISTS (SELECT 1 FROM dbo.ECARGACLIENTES)
            THROW 51502, 'LEGACY_STAGE_BUSY', 1;

        SELECT @Estado = estado,
               @UsuarioId = usuario_id,
               @CorrelacionId = correlation_id,
               @ConfirmadaEn = fecha_confirmacion
        FROM ANEXO24_DEV.app24.CargaCatalogoCliente WITH (UPDLOCK, HOLDLOCK)
        WHERE id = @CargaId;

        IF @Estado IS NULL
            THROW 51503, 'CARGA_NO_ENCONTRADA', 1;
        IF @Estado = 'CONFIRMADA'
            THROW 51504, 'ALREADY_CONFIRMED', 1;
        IF @Estado <> 'PREVISUALIZADA'
            THROW 51505, 'CARGA_NO_CONFIRMABLE', 1;
        IF EXISTS (SELECT 1 FROM ANEXO24_DEV.app24.ErrorCargaCliente WHERE carga_id = @CargaId)
            THROW 51506, 'CARGA_CON_ERRORES', 1;
        IF NOT EXISTS (SELECT 1 FROM ANEXO24_DEV.app24.CargaCatalogoClienteFila WHERE carga_id = @CargaId)
            THROW 51507, 'CARGA_SIN_FILAS', 1;

        -- MERGE ON 1=0 conserva el vínculo entre identidad legacy y fila de origen.
        MERGE dbo.TMPCLIENTES AS destino
        USING (
            SELECT f.hoja,
                   f.fila,
                   JSON_VALUE(f.datos_json, '$.Clave') AS clave,
                   JSON_VALUE(f.datos_json, '$.Nombre') AS nombre,
                   JSON_VALUE(f.datos_json, '$.IdFiscal') AS id_fiscal,
                   JSON_VALUE(f.datos_json, '$.TipoNE') AS tipo_ne,
                   JSON_VALUE(f.datos_json, '$.Programa') AS programa,
                   JSON_VALUE(f.datos_json, '$.CalleNumero') AS calle_numero,
                   JSON_VALUE(f.datos_json, '$.Codigo') AS codigo,
                   JSON_VALUE(f.datos_json, '$.Colonia') AS colonia,
                   JSON_VALUE(f.datos_json, '$.Entidad') AS entidad,
                   JSON_VALUE(f.datos_json, '$.Pais') AS pais,
                   JSON_VALUE(f.datos_json, '$.Telefono') AS telefono,
                   JSON_VALUE(f.datos_json, '$.Correo') AS correo,
                   JSON_VALUE(f.datos_json, '$.Fax') AS fax,
                   JSON_VALUE(f.datos_json, '$.ApellidoPaterno') AS apellido_paterno,
                   JSON_VALUE(f.datos_json, '$.ApellidoMaterno') AS apellido_materno,
                   JSON_VALUE(f.datos_json, '$.Calle') AS calle,
                   CONVERT(INT, NULLIF(JSON_VALUE(f.datos_json, '$.CalleNumeroInterior'), '')) AS calle_numero_interior,
                   JSON_VALUE(f.datos_json, '$.Localidad') AS localidad,
                   JSON_VALUE(f.datos_json, '$.Referencia') AS referencia,
                   JSON_VALUE(f.datos_json, '$.Municipio') AS municipio,
                   JSON_VALUE(f.datos_json, '$.TipoIdentificador') AS tipo_identificador,
                   JSON_VALUE(f.datos_json, '$.CodigoPostal') AS codigo_postal
            FROM ANEXO24_DEV.app24.CargaCatalogoClienteFila f
            WHERE f.carga_id = @CargaId
        ) AS origen
        ON 1 = 0
        WHEN NOT MATCHED THEN
            INSERT (Clave, Nombre, Idfiscal, Tipone, Programa, CalleNumero, Codigo, Colonia,
                    Entidad, Pais, Telefono, Correo, Fax,
                    ApellidoPaterno, ApellidoMaterno, calle, callenumerointerior,
                    localidad, referencia, municipio, tipoidentificador, codigopostal)
            VALUES (LEFT(origen.clave, 50),
                    origen.nombre,
                    LEFT(origen.id_fiscal, 50),
                    origen.tipo_ne,
                    origen.programa,
                    LEFT(origen.calle_numero, 150),
                    origen.codigo,
                    origen.colonia,
                    origen.entidad,
                    origen.pais,
                    origen.telefono,
                    origen.correo,
                    origen.fax,
                    origen.apellido_paterno,
                    origen.apellido_materno,
                    origen.calle,
                    ISNULL(origen.calle_numero_interior, 0),
                    origen.localidad,
                    origen.referencia,
                    origen.municipio,
                    origen.tipo_identificador,
                    origen.codigo_postal)
        OUTPUT inserted.TMPCLIENTEKEY, origen.hoja, origen.fila
            INTO @Mapa (carga_cliente_key, hoja, fila);

        SELECT @TotalFilas = COUNT(*) FROM @Mapa;

        EXEC dbo.CARGACLIENTES;

        INSERT INTO @Errores (hoja, fila, codigo, mensaje)
        SELECT mapa.hoja, mapa.fila, 'LEGACY_VALIDATION', error_legacy.ERROR
        FROM dbo.ECARGACLIENTES error_legacy
        JOIN @Mapa mapa ON mapa.carga_cliente_key = error_legacy.TMPKEY;

        IF EXISTS (SELECT 1 FROM @Errores)
        BEGIN
            ROLLBACK TRANSACTION;

            SELECT @FilasConError = COUNT(*)
            FROM (SELECT DISTINCT hoja, fila FROM @Errores) AS filas_error;

            BEGIN TRANSACTION;
            INSERT INTO ANEXO24_DEV.app24.ErrorCargaCliente
                (carga_id, hoja, fila, columna, valor_enmascarado, codigo, mensaje)
            SELECT @CargaId,
                   hoja,
                   fila,
                   'CargaCliente',
                   'no almacenado',
                   'LEGACY_VALIDATION',
                   LEFT(STRING_AGG(mensaje, ' | '), 500)
            FROM @Errores
            GROUP BY hoja, fila;

            UPDATE ANEXO24_DEV.app24.CargaCatalogoCliente
            SET estado = 'CON_ERRORES',
                total_filas = @TotalFilas,
                filas_validas = @TotalFilas - @FilasConError,
                filas_invalidas = @FilasConError,
                fecha_confirmacion = NULL
            WHERE id = @CargaId;

            SET @DetalleBitacora = CONCAT('cargaId=', @CargaId, ';filas=', @TotalFilas,
                ';filasConError=', @FilasConError);
            EXEC ANEXO24_DEV.app24.APP24_C_BITACORA_REGISTRAR
                @UsuarioId = @UsuarioId,
                @Modulo = 'CATALOGOS_CLIENTES',
                @Accion = 'CLIENTE_CARGA_CONFIRMACION_ERROR',
                @Detalle = @DetalleBitacora,
                @CorrelacionId = @CorrelacionId,
                @Resultado = 'FALLO',
                @EventoId = @EventoId OUTPUT;
            COMMIT TRANSACTION;

            SELECT @CargaId AS CargaId,
                   'CON_ERRORES' AS Estado,
                   @TotalFilas AS TotalFilas,
                   @TotalFilas - @FilasConError AS FilasValidas,
                   @FilasConError AS FilasConError,
                   CAST(NULL AS DATETIME2(3)) AS ConfirmadaEn,
                   'BUSINESS_ERRORS' AS Resultado;
            RETURN;
        END;

        -- El precheck garantiza una etapa ajena vacía; sólo se eliminan identidades de este lote.
        DELETE error_legacy
        FROM dbo.ECARGACLIENTES error_legacy
        JOIN @Mapa mapa ON mapa.carga_cliente_key = error_legacy.TMPKEY;
        DELETE carga_legacy
        FROM dbo.TMPCLIENTES carga_legacy
        JOIN @Mapa mapa ON mapa.carga_cliente_key = carga_legacy.TMPCLIENTEKEY;

        SET @ConfirmadaEn = SYSUTCDATETIME();
        UPDATE ANEXO24_DEV.app24.CargaCatalogoCliente
        SET estado = 'CONFIRMADA',
            total_filas = @TotalFilas,
            filas_validas = @TotalFilas,
            filas_invalidas = 0,
            fecha_confirmacion = @ConfirmadaEn
        WHERE id = @CargaId;

        SET @DetalleBitacora = CONCAT('cargaId=', @CargaId, ';filas=', @TotalFilas);
        EXEC ANEXO24_DEV.app24.APP24_C_BITACORA_REGISTRAR
            @UsuarioId = @UsuarioId,
            @Modulo = 'CATALOGOS_CLIENTES',
            @Accion = 'CLIENTE_CARGA_CONFIRMADA',
            @Detalle = @DetalleBitacora,
            @CorrelacionId = @CorrelacionId,
            @Resultado = 'EXITO',
            @EventoId = @EventoId OUTPUT;
        COMMIT TRANSACTION;

        SELECT @CargaId AS CargaId,
               'CONFIRMADA' AS Estado,
               @TotalFilas AS TotalFilas,
               @TotalFilas AS FilasValidas,
               0 AS FilasConError,
               @ConfirmadaEn AS ConfirmadaEn,
               'CONFIRMED' AS Resultado;
    END TRY
    BEGIN CATCH
        IF XACT_STATE() <> 0 ROLLBACK TRANSACTION;
        THROW;
    END CATCH;
END;
GO