USE [CALE_IMMEX];
GO
SET ANSI_NULLS ON;
GO
SET QUOTED_IDENTIFIER ON;
GO

-- Confirmación técnica de una carga de materiales mediante el staging legacy existente.
-- El catálogo autoritativo continúa siendo dbo.MATERIAL/FACTORESMP y sólo dbo.CARGA_MATERIALES
-- aplica sus reglas de negocio. No crea tablas ni lógica paralela de catálogo.
CREATE OR ALTER PROCEDURE dbo.APP24_C_MATERIAL_CARGA_CONFIRMAR
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
        carga_material_key BIGINT NOT NULL,
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

        -- Coordina tanto inserciones directas como ejecuciones no coordinadas de CARGA_MATERIALES.
        SELECT TOP (1) @Bloqueo = CARGAMATERIALKEY
        FROM dbo.CARGAMATERIAL WITH (TABLOCKX, HOLDLOCK);
        SELECT TOP (1) @Bloqueo = ECARGAMATERIALKEY
        FROM dbo.ECARGAMATERIAL WITH (TABLOCKX, HOLDLOCK);

        IF EXISTS (SELECT 1 FROM dbo.CARGAMATERIAL)
           OR EXISTS (SELECT 1 FROM dbo.ECARGAMATERIAL)
            THROW 51502, 'LEGACY_STAGE_BUSY', 1;

        SELECT @Estado = estado,
               @UsuarioId = usuario_id,
               @CorrelacionId = correlation_id,
               @ConfirmadaEn = fecha_confirmacion
        FROM ANEXO24_DEV.app24.CargaCatalogoMaterial WITH (UPDLOCK, HOLDLOCK)
        WHERE id = @CargaId;

        IF @Estado IS NULL
            THROW 51503, 'CARGA_NO_ENCONTRADA', 1;
        IF @Estado = 'CONFIRMADA'
            THROW 51504, 'ALREADY_CONFIRMED', 1;
        IF @Estado <> 'PREVISUALIZADA'
            THROW 51505, 'CARGA_NO_CONFIRMABLE', 1;
        IF EXISTS (SELECT 1 FROM ANEXO24_DEV.app24.ErrorCargaMaterial WHERE carga_id = @CargaId)
            THROW 51506, 'CARGA_CON_ERRORES', 1;
        IF NOT EXISTS (SELECT 1 FROM ANEXO24_DEV.app24.CargaCatalogoMaterialFila WHERE carga_id = @CargaId)
            THROW 51507, 'CARGA_SIN_FILAS', 1;

        -- MERGE ON 1=0 conserva el vínculo entre identidad legacy y fila de origen.
        MERGE dbo.CARGAMATERIAL AS destino
        USING (
            SELECT f.hoja,
                   f.fila,
                   JSON_VALUE(f.datos_json, '$.ClaveMaterial') AS clave_material,
                   JSON_VALUE(f.datos_json, '$.ClaveMaterialProveedor') AS clave_material_proveedor,
                   JSON_VALUE(f.datos_json, '$.DescripcionComercial') AS descripcion_comercial,
                   JSON_VALUE(f.datos_json, '$.UnidadComercial') AS unidad_comercial,
                   JSON_VALUE(f.datos_json, '$.UnidadTarifa') AS unidad_tarifa,
                   JSON_VALUE(f.datos_json, '$.Fraccion') AS fraccion,
                   CONVERT(NUMERIC(18, 6), NULLIF(JSON_VALUE(f.datos_json, '$.KG'), '')) AS kg,
                   CONVERT(NUMERIC(18, 6), NULLIF(JSON_VALUE(f.datos_json, '$.GR'), '')) AS gr,
                   CONVERT(NUMERIC(18, 6), NULLIF(JSON_VALUE(f.datos_json, '$.ML'), '')) AS ml,
                   CONVERT(NUMERIC(18, 6), NULLIF(JSON_VALUE(f.datos_json, '$.MCUA'), '')) AS mcua,
                   CONVERT(NUMERIC(18, 6), NULLIF(JSON_VALUE(f.datos_json, '$.MCUB'), '')) AS mcub,
                   CONVERT(NUMERIC(18, 6), NULLIF(JSON_VALUE(f.datos_json, '$.PZA'), '')) AS pza,
                   CONVERT(NUMERIC(18, 6), NULLIF(JSON_VALUE(f.datos_json, '$.LT'), '')) AS lt,
                   CONVERT(NUMERIC(18, 6), NULLIF(JSON_VALUE(f.datos_json, '$.PAR'), '')) AS par,
                   CONVERT(NUMERIC(18, 6), NULLIF(JSON_VALUE(f.datos_json, '$.MI'), '')) AS mi,
                   CONVERT(NUMERIC(18, 6), NULLIF(JSON_VALUE(f.datos_json, '$.JGO'), '')) AS jgo,
                   CONVERT(NUMERIC(18, 6), NULLIF(JSON_VALUE(f.datos_json, '$.TON'), '')) AS ton,
                   CONVERT(NUMERIC(18, 6), NULLIF(JSON_VALUE(f.datos_json, '$.BAR'), '')) AS bar,
                   CONVERT(NUMERIC(18, 6), NULLIF(JSON_VALUE(f.datos_json, '$.GRN'), '')) AS grn,
                   CONVERT(NUMERIC(18, 6), NULLIF(JSON_VALUE(f.datos_json, '$.DECE'), '')) AS dece,
                   CONVERT(NUMERIC(18, 6), NULLIF(JSON_VALUE(f.datos_json, '$.CIEN'), '')) AS cien,
                   CONVERT(NUMERIC(18, 6), NULLIF(JSON_VALUE(f.datos_json, '$.DOCE'), '')) AS doce,
                   CONVERT(NUMERIC(18, 6), NULLIF(JSON_VALUE(f.datos_json, '$.CAJA'), '')) AS caja,
                   CONVERT(NUMERIC(18, 6), NULLIF(JSON_VALUE(f.datos_json, '$.BOTELLA'), '')) AS botella,
                   JSON_VALUE(f.datos_json, '$.DIVISION') AS division,
                   JSON_VALUE(f.datos_json, '$.ENTIDAD') AS entidad,
                   JSON_VALUE(f.datos_json, '$.TIPOM') AS tipom,
                   JSON_VALUE(f.datos_json, '$.NumeroSerie') AS numero_serie,
                   JSON_VALUE(f.datos_json, '$.MARCA') AS marca,
                   JSON_VALUE(f.datos_json, '$.MODELO') AS modelo
            FROM ANEXO24_DEV.app24.CargaCatalogoMaterialFila f
            WHERE f.carga_id = @CargaId
        ) AS origen
        ON 1 = 0
        WHEN NOT MATCHED THEN
            INSERT (CLAVEMATERIAL, CLAVEMATERIALPROVEEDOR, DESCRIPCIONCOMERCIAL,
                    UNIDADCOMERCIAL, UNIDADTARIFA, FRACCION,
                    KG, GR, ML, MCUA, MCUB, PZA, LT, PAR, MI, JGO, TON, BAR, GRN, DECE,
                    CIEN, DOCE, CAJA, BOTELLA, DIVISION, ENTIDAD, TIPOM, NumeroSerie, MARCA, MODELO)
            VALUES (origen.clave_material, origen.clave_material_proveedor, origen.descripcion_comercial,
                    origen.unidad_comercial, origen.unidad_tarifa, origen.fraccion,
                    origen.kg, origen.gr, origen.ml, origen.mcua, origen.mcub, origen.pza, origen.lt,
                    origen.par, origen.mi, origen.jgo, origen.ton, origen.bar, origen.grn, origen.dece,
                    origen.cien, origen.doce, origen.caja, origen.botella, origen.division, origen.entidad,
                    origen.tipom, origen.numero_serie, origen.marca, origen.modelo)
        OUTPUT inserted.CARGAMATERIALKEY, origen.hoja, origen.fila
            INTO @Mapa (carga_material_key, hoja, fila);

        SELECT @TotalFilas = COUNT(*) FROM @Mapa;

        EXEC dbo.CARGA_MATERIALES;

        INSERT INTO @Errores (hoja, fila, codigo, mensaje)
        SELECT mapa.hoja, mapa.fila, 'LEGACY_VALIDATION', error_legacy.ERROR
        FROM dbo.ECARGAMATERIAL error_legacy
        JOIN @Mapa mapa ON mapa.carga_material_key = error_legacy.CARGAMATERIALKEY;

        IF EXISTS (SELECT 1 FROM @Errores)
        BEGIN
            -- Los cambios autoritativos y el staging legacy sólo sobreviven si todas las filas son válidas.
            ROLLBACK TRANSACTION;

            SELECT @FilasConError = COUNT(*)
            FROM (SELECT DISTINCT hoja, fila FROM @Errores) AS filas_error;

            BEGIN TRANSACTION;
            INSERT INTO ANEXO24_DEV.app24.ErrorCargaMaterial
                (carga_id, hoja, fila, columna, valor_enmascarado, codigo, mensaje)
            SELECT @CargaId,
                   hoja,
                   fila,
                   'CargaMaterial',
                   'no almacenado',
                   'LEGACY_VALIDATION',
                   LEFT(STRING_AGG(mensaje, ' | '), 500)
            FROM @Errores
            GROUP BY hoja, fila;

            UPDATE ANEXO24_DEV.app24.CargaCatalogoMaterial
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
                @Modulo = 'CATALOGOS_MATERIALES',
                @Accion = 'MATERIAL_CARGA_CONFIRMACION_ERROR',
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
        FROM dbo.ECARGAMATERIAL error_legacy
        JOIN @Mapa mapa ON mapa.carga_material_key = error_legacy.CARGAMATERIALKEY;
        DELETE carga_legacy
        FROM dbo.CARGAMATERIAL carga_legacy
        JOIN @Mapa mapa ON mapa.carga_material_key = carga_legacy.CARGAMATERIALKEY;

        SET @ConfirmadaEn = SYSUTCDATETIME();
        UPDATE ANEXO24_DEV.app24.CargaCatalogoMaterial
        SET estado = 'CONFIRMADA',
            total_filas = @TotalFilas,
            filas_validas = @TotalFilas,
            filas_invalidas = 0,
            fecha_confirmacion = @ConfirmadaEn
        WHERE id = @CargaId;

        SET @DetalleBitacora = CONCAT('cargaId=', @CargaId, ';filas=', @TotalFilas);
        EXEC ANEXO24_DEV.app24.APP24_C_BITACORA_REGISTRAR
            @UsuarioId = @UsuarioId,
            @Modulo = 'CATALOGOS_MATERIALES',
            @Accion = 'MATERIAL_CARGA_CONFIRMADA',
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
