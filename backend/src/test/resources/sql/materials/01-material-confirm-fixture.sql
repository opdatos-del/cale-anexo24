/*
 * Fixture exclusivamente sintético para MaterialLegacyStageConcurrencyTest.
 * Se ejecuta sobre las bases efímeras CALE_IMMEX y ANEXO24_DEV del contenedor.
 */

/* ============================== CALE_IMMEX ============================== */
USE [CALE_IMMEX];
GO

CREATE TABLE dbo.UNIDAD (
    CVE_UNIDAD VARCHAR(20) NOT NULL PRIMARY KEY
);
GO

INSERT INTO dbo.UNIDAD (CVE_UNIDAD) VALUES ('KG');
GO

CREATE TABLE dbo.CARGAMATERIAL (
    CARGAMATERIALKEY BIGINT IDENTITY(1,1) NOT NULL PRIMARY KEY,
    CLAVEMATERIAL VARCHAR(50) NULL,
    CLAVEMATERIALPROVEEDOR VARCHAR(40) NULL,
    DESCRIPCIONCOMERCIAL VARCHAR(250) NULL,
    UNIDADCOMERCIAL VARCHAR(20) NULL,
    UNIDADTARIFA VARCHAR(20) NULL,
    FRACCION VARCHAR(20) NULL,
    KG DECIMAL(18,6) NULL,
    GR DECIMAL(18,6) NULL,
    ML DECIMAL(18,6) NULL,
    MCUA DECIMAL(18,6) NULL,
    MCUB DECIMAL(18,6) NULL,
    PZA DECIMAL(18,6) NULL,
    LT DECIMAL(18,6) NULL,
    PAR DECIMAL(18,6) NULL,
    MI DECIMAL(18,6) NULL,
    JGO DECIMAL(18,6) NULL,
    TON DECIMAL(18,6) NULL,
    BAR DECIMAL(18,6) NULL,
    GRN DECIMAL(18,6) NULL,
    DECE DECIMAL(18,6) NULL,
    CIEN DECIMAL(18,6) NULL,
    DOCE DECIMAL(18,6) NULL,
    CAJA DECIMAL(18,6) NULL,
    BOTELLA DECIMAL(18,6) NULL,
    DIVISION VARCHAR(30) NULL,
    ENTIDAD VARCHAR(30) NULL,
    TIPOM VARCHAR(20) NULL,
    NumeroSerie VARCHAR(100) NULL,
    MARCA VARCHAR(100) NULL,
    MODELO VARCHAR(100) NULL
);
GO

CREATE TABLE dbo.ECARGAMATERIAL (
    ECARGAMATERIALKEY BIGINT IDENTITY(1,1) NOT NULL PRIMARY KEY,
    CARGAMATERIALKEY BIGINT NOT NULL,
    ERROR VARCHAR(500) NOT NULL
);
GO

CREATE TABLE dbo.MATERIAL (
    MATERIALKEY BIGINT NOT NULL PRIMARY KEY,
    CLAVE VARCHAR(50) NOT NULL,
    CLAVEPROVEEDOR VARCHAR(40) NULL,
    DESCRIPCION VARCHAR(250) NULL,
    FRACCION VARCHAR(20) NULL,
    UNIDAD VARCHAR(20) NULL,
    UNIDADT VARCHAR(20) NULL,
    TIPOMATERIAL VARCHAR(50) NULL,
    TIPO VARCHAR(20) NULL,
    TIPOM VARCHAR(20) NULL,
    ALMACENKEY BIGINT NULL,
    numero_serie VARCHAR(100) NULL,
    marca VARCHAR(100) NULL,
    modelo VARCHAR(100) NULL
);
GO

CREATE TABLE dbo.FACTORESMP (
    FACTOR DECIMAL(18,6) NULL,
    UNIDAD VARCHAR(20) NULL,
    MATERIALKEY BIGINT NOT NULL,
    UNIDADCONVERTIR VARCHAR(20) NULL,
    CLAVE VARCHAR(50) NULL
);
GO

CREATE TABLE dbo.APP24_MaterialLegacyExecution (
    id BIGINT IDENTITY(1,1) NOT NULL PRIMARY KEY,
    carga_id BIGINT NOT NULL,
    fecha DATETIME2 NOT NULL CONSTRAINT DF_APP24_MaterialLegacyExecution_fecha DEFAULT SYSUTCDATETIME()
);
GO

CREATE FUNCTION dbo.VALIDUNIT (@unidad VARCHAR(20))
RETURNS VARCHAR(20)
AS
BEGIN
    RETURN @unidad;
END;
GO

CREATE FUNCTION dbo.ENTIDAD (@division VARCHAR(30))
RETURNS BIGINT
AS
BEGIN
    RETURN 1;
END;
GO

/* ============================== ANEXO24_DEV ============================= */
USE [ANEXO24_DEV];
GO

CREATE SCHEMA app24;
GO

CREATE TABLE app24.CargaCatalogoMaterial (
    id BIGINT IDENTITY(1,1) NOT NULL PRIMARY KEY,
    archivo VARCHAR(255) NOT NULL,
    hash CHAR(64) NOT NULL,
    usuario_id BIGINT NOT NULL,
    estado VARCHAR(20) NOT NULL CONSTRAINT CK_CargaCatalogoMaterial_estado
        CHECK (estado IN ('PREVISUALIZADA', 'CONFIRMADA', 'CON_ERRORES')),
    total_filas INT NOT NULL,
    filas_validas INT NOT NULL,
    filas_invalidas INT NOT NULL,
    version_contrato VARCHAR(100) NOT NULL,
    correlation_id VARCHAR(100) NOT NULL,
    fecha_confirmacion DATETIME2 NULL
);
GO

CREATE TABLE app24.CargaCatalogoMaterialFila (
    carga_id BIGINT NOT NULL,
    hoja VARCHAR(100) NOT NULL,
    fila INT NOT NULL,
    datos_json NVARCHAR(MAX) NOT NULL,
    CONSTRAINT PK_CargaCatalogoMaterialFila PRIMARY KEY (carga_id, hoja, fila),
    CONSTRAINT FK_CargaCatalogoMaterialFila_Carga FOREIGN KEY (carga_id) REFERENCES app24.CargaCatalogoMaterial(id)
);
GO

CREATE TABLE app24.ErrorCargaMaterial (
    carga_id BIGINT NOT NULL,
    hoja VARCHAR(100) NOT NULL,
    fila INT NOT NULL,
    columna VARCHAR(100) NOT NULL,
    valor_enmascarado VARCHAR(500) NULL,
    codigo VARCHAR(50) NOT NULL,
    mensaje VARCHAR(500) NOT NULL,
    CONSTRAINT FK_ErrorCargaMaterial_Carga FOREIGN KEY (carga_id) REFERENCES app24.CargaCatalogoMaterial(id)
);
GO

CREATE TABLE app24.BitacoraEvento (
    id BIGINT IDENTITY(1,1) NOT NULL PRIMARY KEY,
    usuario_id BIGINT NOT NULL,
    modulo VARCHAR(100) NOT NULL,
    accion VARCHAR(80) NOT NULL,
    detalle VARCHAR(500) NOT NULL,
    correlation_id VARCHAR(100) NOT NULL,
    resultado VARCHAR(50) NOT NULL,
    fecha DATETIME2 NOT NULL CONSTRAINT DF_BitacoraEvento_fecha DEFAULT SYSUTCDATETIME()
);
GO

CREATE PROCEDURE app24.APP24_C_BITACORA_REGISTRAR
    @UsuarioId BIGINT,
    @Modulo VARCHAR(100),
    @Accion VARCHAR(80),
    @Detalle VARCHAR(500),
    @CorrelacionId VARCHAR(100),
    @Resultado VARCHAR(50),
    @EventoId BIGINT OUTPUT
AS
BEGIN
    SET NOCOUNT ON;
    INSERT INTO app24.BitacoraEvento (usuario_id, modulo, accion, detalle, correlation_id, resultado)
    VALUES (@UsuarioId, @Modulo, @Accion, @Detalle, @CorrelacionId, @Resultado);
    SET @EventoId = SCOPE_IDENTITY();
END;
GO

/* ============================ wrapper sintético ========================= */
USE [CALE_IMMEX];
GO

CREATE PROCEDURE dbo.APP24_C_MATERIAL_CARGA_CONFIRMAR
    @CargaId BIGINT,
    @ForceRollback BIT = 0
AS
BEGIN
    SET NOCOUNT ON;
    SET XACT_ABORT ON;

    DECLARE @Estado VARCHAR(20);
    DECLARE @UsuarioId BIGINT;
    DECLARE @CorrelacionId VARCHAR(100);
    DECLARE @Bloqueo BIGINT;
    DECLARE @FilasConfirmadas INT;
    DECLARE @FilasInvalidas INT;
    DECLARE @DetalleBitacora VARCHAR(500);
    DECLARE @EventoId BIGINT;
    DECLARE @Mapa TABLE (
        CargaMaterialKey BIGINT NOT NULL,
        hoja VARCHAR(100) NOT NULL,
        fila INT NOT NULL
    );
    DECLARE @Errores TABLE (
        hoja VARCHAR(100) NOT NULL,
        fila INT NOT NULL,
        codigo VARCHAR(50) NOT NULL,
        mensaje VARCHAR(500) NOT NULL
    );

    BEGIN TRY
        BEGIN TRANSACTION;

        /* Bloqueo compatible con los escritores y el EXEC legacy no coordinado. */
        SELECT TOP (1) @Bloqueo = CARGAMATERIALKEY
        FROM dbo.CARGAMATERIAL WITH (TABLOCKX, HOLDLOCK);
        SELECT TOP (1) @Bloqueo = ECARGAMATERIALKEY
        FROM dbo.ECARGAMATERIAL WITH (TABLOCKX, HOLDLOCK);

        IF EXISTS (SELECT 1 FROM dbo.CARGAMATERIAL)
           OR EXISTS (SELECT 1 FROM dbo.ECARGAMATERIAL)
            THROW 51001, 'LEGACY_STAGE_BUSY', 1;

        SELECT @Estado = estado, @UsuarioId = usuario_id, @CorrelacionId = correlation_id
        FROM ANEXO24_DEV.app24.CargaCatalogoMaterial WITH (UPDLOCK, HOLDLOCK)
        WHERE id = @CargaId;

        IF @Estado IS NULL
            THROW 51002, 'CARGA_NO_ENCONTRADA', 1;
        IF @Estado = 'CONFIRMADA'
            THROW 51003, 'ALREADY_CONFIRMED', 1;
        IF @Estado = 'CON_ERRORES'
            THROW 51004, 'CARGA_CON_ERRORES', 1;
        IF NOT EXISTS (SELECT 1 FROM ANEXO24_DEV.app24.CargaCatalogoMaterialFila WHERE carga_id = @CargaId)
            THROW 51005, 'CARGA_SIN_FILAS', 1;

        /* MERGE ON 1=0 permite que OUTPUT conserve ambas identidades sin alterar el staging legacy. */
        MERGE dbo.CARGAMATERIAL AS destino
        USING (
            SELECT
                f.hoja,
                f.fila,
                JSON_VALUE(f.datos_json, '$.ClaveMaterial') AS clave_material,
                JSON_VALUE(f.datos_json, '$.ClaveMaterialProveedor') AS clave_material_proveedor,
                JSON_VALUE(f.datos_json, '$.DescripcionComercial') AS descripcion_comercial,
                JSON_VALUE(f.datos_json, '$.UnidadComercial') AS unidad_comercial,
                JSON_VALUE(f.datos_json, '$.UnidadTarifa') AS unidad_tarifa,
                JSON_VALUE(f.datos_json, '$.Fraccion') AS fraccion,
                TRY_CONVERT(DECIMAL(18,6), JSON_VALUE(f.datos_json, '$.KG')) AS kg,
                TRY_CONVERT(DECIMAL(18,6), JSON_VALUE(f.datos_json, '$.GR')) AS gr,
                TRY_CONVERT(DECIMAL(18,6), JSON_VALUE(f.datos_json, '$.ML')) AS ml,
                TRY_CONVERT(DECIMAL(18,6), JSON_VALUE(f.datos_json, '$.MCUA')) AS mcua,
                TRY_CONVERT(DECIMAL(18,6), JSON_VALUE(f.datos_json, '$.MCUB')) AS mcub,
                TRY_CONVERT(DECIMAL(18,6), JSON_VALUE(f.datos_json, '$.PZA')) AS pza,
                TRY_CONVERT(DECIMAL(18,6), JSON_VALUE(f.datos_json, '$.LT')) AS lt,
                TRY_CONVERT(DECIMAL(18,6), JSON_VALUE(f.datos_json, '$.PAR')) AS par,
                TRY_CONVERT(DECIMAL(18,6), JSON_VALUE(f.datos_json, '$.MI')) AS mi,
                TRY_CONVERT(DECIMAL(18,6), JSON_VALUE(f.datos_json, '$.JGO')) AS jgo,
                TRY_CONVERT(DECIMAL(18,6), JSON_VALUE(f.datos_json, '$.TON')) AS ton,
                TRY_CONVERT(DECIMAL(18,6), JSON_VALUE(f.datos_json, '$.BAR')) AS bar,
                TRY_CONVERT(DECIMAL(18,6), JSON_VALUE(f.datos_json, '$.GRN')) AS grn,
                TRY_CONVERT(DECIMAL(18,6), JSON_VALUE(f.datos_json, '$.DECE')) AS dece,
                TRY_CONVERT(DECIMAL(18,6), JSON_VALUE(f.datos_json, '$.CIEN')) AS cien,
                TRY_CONVERT(DECIMAL(18,6), JSON_VALUE(f.datos_json, '$.DOCE')) AS doce,
                TRY_CONVERT(DECIMAL(18,6), JSON_VALUE(f.datos_json, '$.CAJA')) AS caja,
                TRY_CONVERT(DECIMAL(18,6), JSON_VALUE(f.datos_json, '$.BOTELLA')) AS botella,
                JSON_VALUE(f.datos_json, '$.DIVISION') AS division,
                JSON_VALUE(f.datos_json, '$.ENTIDAD') AS entidad,
                JSON_VALUE(f.datos_json, '$.TIPOM') AS tipom,
                JSON_VALUE(f.datos_json, '$.NumeroSerie') AS numero_serie,
                JSON_VALUE(f.datos_json, '$.MARCA') AS marca,
                JSON_VALUE(f.datos_json, '$.MODELO') AS modelo
            FROM ANEXO24_DEV.app24.CargaCatalogoMaterialFila f
            WHERE f.carga_id = @CargaId
        ) AS f
        ON 1 = 0
        WHEN NOT MATCHED THEN INSERT (
            CLAVEMATERIAL, CLAVEMATERIALPROVEEDOR, DESCRIPCIONCOMERCIAL,
            UNIDADCOMERCIAL, UNIDADTARIFA, FRACCION,
            KG, GR, ML, MCUA, MCUB, PZA, LT, PAR, MI, JGO, TON, BAR, GRN, DECE, CIEN, DOCE, CAJA, BOTELLA,
            DIVISION, ENTIDAD, TIPOM, NumeroSerie, MARCA, MODELO
        ) VALUES (
            f.clave_material, f.clave_material_proveedor, f.descripcion_comercial,
            f.unidad_comercial, f.unidad_tarifa, f.fraccion,
            f.kg, f.gr, f.ml, f.mcua, f.mcub, f.pza, f.lt, f.par, f.mi, f.jgo, f.ton, f.bar, f.grn, f.dece, f.cien, f.doce, f.caja, f.botella,
            f.division, f.entidad, f.tipom, f.numero_serie, f.marca, f.modelo
        )
        OUTPUT inserted.CARGAMATERIALKEY, f.hoja, f.fila
            INTO @Mapa (CargaMaterialKey, hoja, fila);

        SELECT @FilasConfirmadas = COUNT(*) FROM @Mapa;
        INSERT INTO dbo.APP24_MaterialLegacyExecution (carga_id) VALUES (@CargaId);
        EXEC dbo.CARGA_MATERIALES;

        INSERT INTO @Errores (hoja, fila, codigo, mensaje)
        SELECT m.hoja, m.fila, 'LEGACY_VALIDATION', e.ERROR
        FROM dbo.ECARGAMATERIAL e
        JOIN @Mapa m ON m.CargaMaterialKey = e.CARGAMATERIALKEY;

        IF EXISTS (SELECT 1 FROM @Errores)
        BEGIN
            /* La mutación de MATERIAL/FACTORESMP y el staging legacy no sobreviven. */
            ROLLBACK TRANSACTION;

            BEGIN TRANSACTION;
            SELECT @FilasInvalidas = COUNT(*)
            FROM (SELECT DISTINCT hoja, fila FROM @Errores) AS filas_con_error;
            INSERT INTO ANEXO24_DEV.app24.ErrorCargaMaterial
                (carga_id, hoja, fila, columna, valor_enmascarado, codigo, mensaje)
            SELECT @CargaId, hoja, fila, 'CargaMaterial', 'no almacenado', codigo, mensaje
            FROM @Errores;
            UPDATE ANEXO24_DEV.app24.CargaCatalogoMaterial
            SET estado = 'CON_ERRORES',
                total_filas = @FilasConfirmadas,
                filas_validas = @FilasConfirmadas - @FilasInvalidas,
                filas_invalidas = @FilasInvalidas,
                fecha_confirmacion = NULL
            WHERE id = @CargaId;
            SET @DetalleBitacora = CONCAT('carga=', @CargaId, ';filas=', @FilasConfirmadas,
                ';invalidas=', @FilasInvalidas, ';errores=', (SELECT COUNT(*) FROM @Errores));
            EXEC [ANEXO24_DEV].[app24].[APP24_C_BITACORA_REGISTRAR]
                @UsuarioId = @UsuarioId,
                @Modulo = 'CATALOGOS_MATERIALES',
                @Accion = 'MATERIAL_CARGA_CONFIRMACION_ERROR',
                @Detalle = @DetalleBitacora,
                @CorrelacionId = @CorrelacionId,
                @Resultado = 'FALLO',
                @EventoId = @EventoId OUTPUT;
            COMMIT TRANSACTION;

            SELECT 'BUSINESS_ERRORS' AS Resultado, 'CON_ERRORES' AS Estado,
                   CAST(NULL AS INT) AS FilasConfirmadas,
                   (SELECT COUNT(*) FROM @Errores) AS Errores;
            RETURN;
        END;

        IF @ForceRollback = 1
            THROW 51006, 'FORCED_ROLLBACK_TEST_ONLY', 1;

        /* El precheck garantiza etapa ajena vacía; aun así el cleanup sólo toca el lote mapeado. */
        DELETE e
        FROM dbo.ECARGAMATERIAL e
        JOIN @Mapa m ON m.CargaMaterialKey = e.CARGAMATERIALKEY;
        DELETE c
        FROM dbo.CARGAMATERIAL c
        JOIN @Mapa m ON m.CargaMaterialKey = c.CARGAMATERIALKEY;
        UPDATE ANEXO24_DEV.app24.CargaCatalogoMaterial
        SET estado = 'CONFIRMADA',
            total_filas = @FilasConfirmadas,
            filas_validas = @FilasConfirmadas,
            filas_invalidas = 0,
            fecha_confirmacion = SYSUTCDATETIME()
        WHERE id = @CargaId;
        SET @DetalleBitacora = CONCAT('carga=', @CargaId, ';filas=', @FilasConfirmadas);
        EXEC [ANEXO24_DEV].[app24].[APP24_C_BITACORA_REGISTRAR]
            @UsuarioId = @UsuarioId,
            @Modulo = 'CATALOGOS_MATERIALES',
            @Accion = 'MATERIAL_CARGA_CONFIRMADA',
            @Detalle = @DetalleBitacora,
            @CorrelacionId = @CorrelacionId,
            @Resultado = 'CONFIRMADA',
            @EventoId = @EventoId OUTPUT;
        COMMIT TRANSACTION;

        SELECT 'CONFIRMED' AS Resultado, 'CONFIRMADA' AS Estado,
               @FilasConfirmadas AS FilasConfirmadas,
               CAST(NULL AS INT) AS Errores;
    END TRY
    BEGIN CATCH
        IF XACT_STATE() <> 0 ROLLBACK TRANSACTION;
        THROW;
    END CATCH
END;
GO
