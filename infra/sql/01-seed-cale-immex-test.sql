-- ============================================
-- CALE_IMMEX: Seed de datos de prueba
-- Datos ficticios de maquila (IMMEX) para desarrollo/QA
-- Re-ejecutable: borra y reinserta secciones
-- ============================================
USE CALE_IMMEX;
GO

SET NOCOUNT ON;
PRINT '=== SEED CALE_IMMEX INICIO ===';
GO

-- ============================================
-- 1. CATALOGOS BASE
-- ============================================

-- 1.1 DatosGenerales (empresa anexo)
IF EXISTS (SELECT 1 FROM DatosGenerales) TRUNCATE TABLE DatosGenerales;
GO
INSERT INTO DatosGenerales
(Denominacion, Rfc, RegistroIMMEX, Sector, RegistroAltex, CalleNumero, Codigopostal, Colonia, Entidad, Municipio, Localidad, Telefono, Fax, Correo, Actividad, pais, fechaVigencia)
VALUES
('EMPRESA DE PRUEBA MAQUILA SA DE CV', 'EMA960101XXX', 'IMMEX202400123', '01', 'ALTEX-00123', 'AV INDUSTRIAL 1234', '88620', 'PARQUE INDUSTRIAL', 'TAMAULIPAS', 'REYNOSA', 'REYNOSA', '899 123 4567', '899 123 4568', 'prueba@ejemplo.mx', 'MAQUILA DE MUEBLES', 'MEXICO', '2026-12-31');
GO

-- 1.2 Almacenes
IF EXISTS (SELECT 1 FROM almacen) TRUNCATE TABLE almacen;
GO
INSERT INTO almacen (ALMACENKEY, ALMACEN, DESCRIPCION)
VALUES
(1, 'ALM01', 'ALMACEN PRINCIPAL'),
(2, 'ALM02', 'ALMACEN MATERIA PRIMA');
GO

-- 1.3 Unidades
IF EXISTS (SELECT 1 FROM unidad) TRUNCATE TABLE unidad;
GO
INSERT INTO unidad (CVE_UNIDAD, NOMBRE, ALIAS)
VALUES
('PZA', 'PIEZA', 'PZ'),
('KG', 'KILOGRAMO', 'KG'),
('MT', 'METRO', 'M'),
('M2', 'METRO CUADRADO', 'M2'),
('M3', 'METRO CUBICO', 'M3'),
('LT', 'LITRO', 'L'),
('JGO', 'JUEGO', 'JG'),
('PAR', 'PAR', 'PA'),
('MIL', 'MILLAR', 'MIL'),
('CIEN', 'CIENTO', 'CIEN'),
('GT', 'GRAN TOTAL', 'GT');
GO

-- 1.4 Categorias
IF EXISTS (SELECT 1 FROM categorias) TRUNCATE TABLE categorias;
GO
INSERT INTO categorias (categoria, descripcion, Dias_Validos, meses)
VALUES
('A', 'MERCANCIA PARA MAQUILA', 365, 12),
('B', 'RETORNO DE MERCANCIA', 90, 3),
('C', 'IMPORTACION TEMPORAL', 180, 6);
GO

-- 1.5 TipoMaterial
IF EXISTS (SELECT 1 FROM TipoMaterial) TRUNCATE TABLE TipoMaterial;
GO
INSERT INTO TipoMaterial (TipoMaterial)
VALUES
('INSUMOS'), ('ENVASES'), ('MAQUINARIA'), ('MATERIAL PRIMA');
GO

-- 1.6 Agentes aduanales
IF EXISTS (SELECT 1 FROM agentes) TRUNCATE TABLE agentes;
GO
INSERT INTO agentes (Clave, Nombre, Rfc, Patente, AgenciaAduanal, Domicilio, Telefono, Contacto, RFCAgencia, AltaImportacion, AltaExportacion)
VALUES
('AG001', 'AGENTE ADUANAL PRUEBA UNO', 'AAP920101ABC', '1234', 'AGENCIA ADUANAL UNO SC', 'CALLE 1 100 NUEVO LAREDO TAMAULIPAS', '867 100 2000', 'LIC JUAN PEREZ', 'AAG920101ABC', 'S', 'S'),
('AG002', 'AGENTE ADUANAL PRUEBA DOS', 'AAP930202DEF', '5678', 'AGENCIA ADUANAL DOS SC', 'BLVD 2 200 REYNOSA TAMAULIPAS', '899 200 3000', 'LIC MARIA LOPEZ', 'AAG930202DEF', 'S', 'S');
GO

-- 1.7 Clientes
IF EXISTS (SELECT 1 FROM clientes) TRUNCATE TABLE clientes;
GO
INSERT INTO clientes (Clave, Nombre, Idfiscal, Tipone, Programa, Callenumero, Codigo, Colonia, Entidad, Pais, Telefono, Correo, clientekey, calle, callenumerointerior, localidad, municipio, tipoidentificador, codigopostal)
VALUES
('CL001', 'MUEBLES FINOS DE EXPORTACION SA DE CV', 'MFE940101XXX', '01', 'IMMEX', 'AV LAZARO CARDENAS 500', 'CP001', 'CENTRO', 'NUEVO LEON', 'MEXICO', '818 300 4000', 'ventas@mueblesfinos.mx', 1, 'AV LAZARO CARDENAS', 500, 'MONTERREY', 'MONTERREY', 'RFC', '64000'),
('CL002', 'INDUSTRIAS MADERERAS DEL NORTE SA', 'IMN950202YYY', '01', 'IMMEX', 'CARR NACIONAL KM 12 800', 'CP002', 'INDUSTRIAL', 'COAHUILA', 'MEXICO', '844 500 6000', 'compras@madnorte.mx', 2, 'CARR NACIONAL', 800, 'SALTILLO', 'SALTILLO', 'RFC', '25280'),
('CL003', 'TEXTILES UNIDOS SA DE CV', 'TXU960303ZZZ', '02', 'IMMEX', 'CALZ DE LAS AMERICAS 300', 'CP003', 'MODERNA', 'JALISCO', 'MEXICO', '333 700 8000', 'contacto@textunidos.mx', 3, 'CALZ DE LAS AMERICAS', 300, 'GUADALAJARA', 'GUADALAJARA', 'RFC', '44100');
GO

-- 1.8 Proveedores
IF EXISTS (SELECT 1 FROM Proveedores) TRUNCATE TABLE Proveedores;
GO
INSERT INTO Proveedores (Clave, Nombre, Idfiscal, Tipone, Programa, Callenumero, Codigo, Colonia, Entidad, Pais, Telefono, Correo, proveedorkey, ALMACENKEY, calle, localidad, municipio, tipoidentificador, codigopostal)
VALUES
('PR001', 'SUPPLIER ONE INC', 'US-88-1234567', '01', 'PROVEE', '1000 INDUSTRIAL PARKWAY', 'ZZ001', 'INDUSTRIAL', 'TEXAS', 'USA', '512 100 2000', 'sales@supone.com', 1, 2, '1000 INDUSTRIAL PKWY', 'AUSTIN', 'AUSTIN', 'EIN', '78701'),
('PR002', 'CHINA MATERIALS EXPORT LTD', 'CN-912345678', '01', 'PROVEE', '88 SHENZHEN RD', 'ZZ002', 'FUJIAN', 'FUJIAN', 'CHINA', '86 755 100 2000', 'export@cmex.cn', 2, 2, '88 SHENZHEN RD', 'SHENZHEN', 'SHENZHEN', 'EIN', '518000'),
('PR003', 'MADERAS TRATADAS SA DE CV', 'MTT970404WWW', '01', 'PROVEE', 'KM 5 CARR SAN LUIS', 'CP004', 'EJIDAL', 'SAN LUIS POTOSI', 'MEXICO', '444 800 9000', 'ventas@maderast.mx', 3, 2, 'KM 5 CARR SAN LUIS', 'SLP', 'SLP', 'RFC', '78390');
GO

-- 1.9 Materiales (materia prima importada)
IF EXISTS (SELECT 1 FROM material) TRUNCATE TABLE material;
GO
INSERT INTO material (materialkey, clave, descripcion, fraccion, unidad, unidadt, tipomaterial, tipo, FactorUM, IGIE, claveProveedor, ALMACENKEY, TIPOM, FAMILIA, numero_serie, MARCA, MODELO, NICO)
VALUES
(1, 'MAD-PINO-001', 'MADERA DE PINO TRATADA CLASE A', '4407.11', 'PZA', 'M3', 'MADERA', 'M', 1.5, 0, 'CLAV-PR001', 2, 'MP', 'MADERAS', NULL, 'PINERA', 'PINO-A', 'N'),
(2, 'MAD-ROBLE-002', 'MADERA DE ROBLE CLASE B', '4407.91', 'PZA', 'M3', 'MADERA', 'M', 1.0, 0, 'CLAV-PR003', 2, 'MP', 'MADERAS', NULL, 'ROBLESA', 'ROBLE-B', 'N'),
(3, 'TRI-MDP-003', 'TRIPLAY MDP 18MM', '4410.11', 'PZA', 'M3', 'TRIPLAY', 'M', 1.0, 0, 'CLAV-PR001', 2, 'MP', 'TABLEROS', NULL, 'TRIFLEX', 'MDP18', 'N'),
(4, 'ESP-POLI-004', 'ESPUMA DE POLIURETANO 25KG/M3', '3921.13', 'PZA', 'M3', 'ESPUMA', 'M', 1.0, 0, 'CLAV-PR002', 2, 'MP', 'ESPUMAS', NULL, 'FLEXPUR', 'EC-25', 'N'),
(5, 'TEL-TAPIZ-005', 'TELA PARA TAPIZAR ANTIBACTERIAL', '5407.61', 'MT', 'M2', 'TELA', 'M', 0.5, 0, 'CLAV-PR001', 2, 'MP', 'TEXTILES', NULL, 'TEXTILNA', 'TA-1000', 'N'),
(6, 'TORN-3-8-006', 'TORNILLO HEXAGONAL 3/8 X 1.5', '7318.15', 'MIL', 'KG', 'TORNILLERIA', 'M', 22.0, 0, 'CLAV-PR002', 2, 'MP', 'FERRETERIA', NULL, 'TORNEX', '6H-38X15', 'N'),
(7, 'BIS-STD-007', 'BISAGRA STANDARD 3 PULGADAS', '8302.10', 'CIEN', 'KG', 'HERRERIA', 'M', 10.0, 0, 'CLAV-PR002', 2, 'MP', 'FERRETERIA', NULL, 'HERRAJESMX', 'BS-03', 'N'),
(8, 'PEG-COLA-008', 'COLA PARA MADERA CLASE D3', '3506.10', 'LT', 'KG', 'QUIMICO', 'M', 1.0, 0, 'CLAV-PR003', 2, 'MP', 'ADHESIVOS', NULL, 'QUIMICA UNO', 'D3-500', 'N');
GO

-- 1.10 Factores de conversion material
IF EXISTS (SELECT 1 FROM FactoresMP) TRUNCATE TABLE FactoresMP;
GO
INSERT INTO FactoresMP (materialkey, clave, Unidad, UnidadConvertir, Factor)
VALUES
(1, 'MAD-PINO-001', 'PZA', 'M3', 0.03),
(2, 'MAD-ROBLE-002', 'PZA', 'M3', 0.04),
(3, 'TRI-MDP-003', 'PZA', 'M3', 0.018),
(5, 'TEL-TAPIZ-005', 'MT', 'M2', 1.4),
(6, 'TORN-3-8-006', 'MIL', 'KG', 22.0),
(7, 'BIS-STD-007', 'CIEN', 'KG', 10.0),
(8, 'PEG-COLA-008', 'LT', 'KG', 0.9);
GO

-- 1.11 Productos (producto terminado de exportacion)
IF EXISTS (SELECT 1 FROM productos) TRUNCATE TABLE productos;
GO
INSERT INTO productos (PRODUCTOKEY, CVE_PRODUCTO, NOMBRE, UNIDAD, fraccion, CVE_PRODUCTO_CLIENTE, ALMACENKEY, TIPO, UNIDADT, NICO)
VALUES
(101, 'SILL-2026-01', 'SILLA EJECUTIVA MADERA Y TAPIZ', 'PZA', '9401.71', 'SILL-CL001', 1, 'PT', 'PZA', 'N'),
(102, 'MESA-2026-02', 'MESA DE CENTRO ROBLE 110CM', 'PZA', '9403.60', 'MESA-CL002', 1, 'PT', 'PZA', 'N'),
(103, 'SOFA-2026-03', 'SOFA DE 3 PLAZAS TAPIZADO', 'PZA', '9401.61', 'SOFA-CL001', 1, 'PT', 'PZA', 'N'),
(104, 'BUR-2026-04', 'BUREAU 2 CAJONES PINO', 'PZA', '9403.50', 'BUR-CL003', 1, 'PT', 'PZA', 'N');
GO

-- 1.12 Factores de conversion producto
IF EXISTS (SELECT 1 FROM Factoresprod) TRUNCATE TABLE Factoresprod;
GO
INSERT INTO Factoresprod (productokey, clave, Unidad, UnidadConvertir, Factor)
VALUES
(101, 'SILL-2026-01', 'PZA', 'PZA', 1.0),
(102, 'MESA-2026-02', 'PZA', 'PZA', 1.0),
(103, 'SOFA-2026-03', 'PZA', 'PZA', 1.0),
(104, 'BUR-2026-04', 'PZA', 'PZA', 1.0);
GO

-- 1.13 Estructuras (BOM) cabecera
IF EXISTS (SELECT 1 FROM estructuras) TRUNCATE TABLE estructuras;
GO
INSERT INTO estructuras (estructurakey, inicio, productolink)
VALUES
(1, '2026-01-01', 101),
(2, '2026-01-01', 102),
(3, '2026-01-01', 103),
(4, '2026-01-01', 104);
GO

-- 1.14 Estructuras (BOM) lineas
IF EXISTS (SELECT 1 FROM estructurasML) TRUNCATE TABLE estructurasML;
GO
INSERT INTO estructurasML (LINEA, PRODUCTO, PADRE, CVE_MATERIAL, CANTIDAD, UNIDAD, NIVEL, LINEAPADRE, IMPORTE, MONEDA, CVEPRODUCTO, estructuralink, productolink, revisado, merma, desperdicio)
VALUES
(1,  'SILL-2026-01', 'SILL-2026-01', 'MAD-PINO-001', 1.5, 'PZA', 1, 0, 20.00, 'USD', 'SILL-2026-01', 1, 101, 1, 0.05, 0.02),
(2,  'SILL-2026-01', 'SILL-2026-01', 'TEL-TAPIZ-005', 2.0, 'MT',  1, 0, 4.00,  'USD', 'SILL-2026-01', 1, 101, 1, 0.03, 0.01),
(3,  'SILL-2026-01', 'SILL-2026-01', 'TORN-3-8-006', 0.022, 'MIL', 1, 0, 1.50, 'USD', 'SILL-2026-01', 1, 101, 1, 0.02, 0.01),
(4,  'SILL-2026-01', 'SILL-2026-01', 'PEG-COLA-008', 0.1, 'LT', 1, 0, 0.40, 'USD', 'SILL-2026-01', 1, 101, 1, 0.05, 0.02),
(5,  'MESA-2026-02', 'MESA-2026-02', 'MAD-ROBLE-002', 2.0, 'PZA', 1, 0, 60.00, 'USD', 'MESA-2026-02', 2, 102, 1, 0.04, 0.02),
(6,  'MESA-2026-02', 'MESA-2026-02', 'PEG-COLA-008', 0.15, 'LT', 1, 0, 0.60, 'USD', 'MESA-2026-02', 2, 102, 1, 0.05, 0.02),
(7,  'SOFA-2026-03', 'SOFA-2026-03', 'MAD-PINO-001', 2.5, 'PZA', 1, 0, 33.00, 'USD', 'SOFA-2026-03', 3, 103, 1, 0.05, 0.02),
(8,  'SOFA-2026-03', 'SOFA-2026-03', 'ESP-POLI-004', 0.25, 'PZA', 1, 0, 12.00, 'USD', 'SOFA-2026-03', 3, 103, 1, 0.04, 0.02),
(9,  'SOFA-2026-03', 'SOFA-2026-03', 'TEL-TAPIZ-005', 3.5, 'MT',  1, 0, 7.00,  'USD', 'SOFA-2026-03', 3, 103, 1, 0.03, 0.01),
(10, 'BUR-2026-04', 'BUR-2026-04', 'TRI-MDP-003', 1.2, 'PZA', 1, 0, 18.00, 'USD', 'BUR-2026-04', 4, 104, 1, 0.05, 0.02),
(11, 'BUR-2026-04', 'BUR-2026-04', 'BIS-STD-007', 0.02, 'CIEN', 1, 0, 1.20, 'USD', 'BUR-2026-04', 4, 104, 1, 0.02, 0.01),
(12, 'BUR-2026-04', 'BUR-2026-04', 'PEG-COLA-008', 0.08, 'LT', 1, 0, 0.32, 'USD', 'BUR-2026-04', 4, 104, 1, 0.05, 0.02);
GO

-- 1.15 Producto - material (relacion BOM plana)
IF EXISTS (SELECT 1 FROM productomaterial) TRUNCATE TABLE productomaterial;
GO
INSERT INTO productomaterial (PRODMATKEY, CVE_MATERIAL, CANT_UTILIZADA, CANT_MERMADA, CANT_DESPERDICIADA, PRODUCTOLINK, DESCRIPCION, UNIDAD, VMax, VMin, estructuralink, TIPOM)
VALUES
(1,  'MAD-PINO-001', 1.5, 0.075, 0.03, 101, 'MADERA DE PINO TRATADA CLASE A', 'PZA', 1.6, 1.4, 1, 'MP'),
(2,  'TEL-TAPIZ-005', 2.0, 0.06,  0.02, 101, 'TELA PARA TAPIZAR ANTIBACTERIAL', 'MT', 2.1, 1.9, 1, 'MP'),
(3,  'TORN-3-8-006', 0.022, 0.00044, 0.00022, 101, 'TORNILLO HEXAGONAL 3/8 X 1.5', 'MIL', 0.025, 0.02, 1, 'MP'),
(4,  'PEG-COLA-008', 0.1, 0.005, 0.002, 101, 'COLA PARA MADERA CLASE D3', 'LT', 0.11, 0.09, 1, 'MP');
GO

-- 1.16 Plantas
IF EXISTS (SELECT 1 FROM plantas) TRUNCATE TABLE plantas;
GO
INSERT INTO plantas (PlantaID, nombre, ubicacion, Folio, Direccion1, Direccion2, Direccion3)
VALUES
('PL01', 'PLANTA REYNOSA', 'REYNOSA TAMAULIPAS', 1, 'AV INDUSTRIAL 1234', 'PARQUE INDUSTRIAL', 'REYNOSA TAMAULIPAS CP 88620'),
('PL02', 'PLANTA MONTERREY', 'MONTERREY NUEVO LEON', 2, 'AV LAZARO CARDENAS 500', 'CENTRO', 'MONTERREY NL CP 64000');
GO

-- 1.17 Usuarios
IF EXISTS (SELECT 1 FROM usuarios) TRUNCATE TABLE usuarios;
GO
INSERT INTO usuarios (UserID, clave, nombre, plantalink, privilegios, Conectados, SendMessage, NuevoMensaje)
VALUES
('USER01', 'admin', 'ADMINISTRADOR PRUEBA', 'PL01', 3, 0, '', 0),
('USER02', 'captura', 'CAPTURISTA PRUEBA', 'PL01', 1, 0, '', 0),
('USER03', 'consulta', 'CONSULTA PRUEBA', 'PL02', 0, 0, '', 0);
GO

-- 1.18 Periodos
IF EXISTS (SELECT 1 FROM Periodo) TRUNCATE TABLE Periodo;
GO
INSERT INTO Periodo (clave, descripcion, Hasta, Desde)
VALUES
('E1', 'ENERO', '31', '01'),
('F1', 'FEBRERO', '28', '01'),
('M1', 'MARZO', '31', '01'),
('A1', 'ABRIL', '30', '01'),
('MY1', 'MAYO', '31', '01'),
('J1', 'JUNIO', '30', '01');
GO

-- 1.19 Settings
IF EXISTS (SELECT 1 FROM settings) TRUNCATE TABLE settings;
GO
INSERT INTO settings (settings, value)
VALUES
('TIPO_CAMBIO_DEFAULT', '17.50'),
('EMPRESA_ANEXO', 'ANEXO 24 TEST'),
('SKIN', 'default');
GO

-- 1.20 Generadores (consecutivos)
IF EXISTS (SELECT 1 FROM generadores) TRUNCATE TABLE generadores;
GO
INSERT INTO generadores (tabla, consecutivo)
VALUES
('descarga', 1), ('trazo', 1), ('FacturasCreadas', 1), ('glosa', 1), ('comprobante', 1);
GO

-- 1.21 Mensajes de inicio
IF EXISTS (SELECT 1 FROM MensajesInicio) TRUNCATE TABLE MensajesInicio;
GO
INSERT INTO MensajesInicio (Mensaje, Clave, Importancia)
VALUES
('BASE DE DATOS DE PRUEBA - DATOS FICTICIOS', 'SISTEMA', 'ALTA');
GO

-- ============================================
-- 2. TABLAS STAGE (entrada de carga)
-- ============================================

-- 2.1 CargaMaterial (insumos)
IF EXISTS (SELECT 1 FROM CargaMaterial) TRUNCATE TABLE CargaMaterial;
GO
INSERT INTO CargaMaterial (ClaveMaterial, ClaveMaterialProveedor, DescripcionComercial, UnidadComercial, UnidadTarifa, Fraccion, PZA, KG, TON, DIVISION, ENTIDAD, TIPOM)
VALUES
('MAD-PINO-001', 'CLAV-PR001', 'MADERA DE PINO TRATADA CLASE A', 'PZA', 'M3',  '4407.11', 100, 3000, 0, 'MAD', 'TAMPS', 'MP'),
('MAD-ROBLE-002', 'CLAV-PR003', 'MADERA DE ROBLE CLASE B', 'PZA', 'M3',      '4407.91', 80, 2400, 0, 'MAD', 'TAMPS', 'MP'),
('TRI-MDP-003', 'CLAV-PR001', 'TRIPLAY MDP 18MM', 'PZA', 'M3', '4410.11', 120, 2000, 0, 'TAB', 'TAMPS', 'MP'),
('ESP-POLI-004', 'CLAV-PR002', 'ESPUMA DE POLIURETANO 25KG/M3', 'PZA', 'M3', '3921.13', 40, 500, 0, 'ESP', 'TAMPS', 'MP'),
('TEL-TAPIZ-005', 'CLAV-PR001', 'TELA PARA TAPIZAR ANTIBACTERIAL', 'MT', 'M2', '5407.61', 500, 900, 0, 'TEX', 'TAMPS', 'MP');
GO

-- 2.2 CargaPedimentosIE (pedimentos de importacion)
IF EXISTS (SELECT 1 FROM CargaPedimentosIE) TRUNCATE TABLE CargaPedimentosIE;
GO
INSERT INTO CargaPedimentosIE (Aduana, Patente, NumeroPedimento, ClavePedimento, TipoOperacion, TipoPedimento, tc, FechaPago, ClaveCP, NombreCP, IGIE, IVA, DTA, PREV, Sec, Clave, Descripcion, Fraccion, CantidadComercial, UnidadComercial, CantidadTarifa, UnidadTarifa, PaisOD, PaisCV, ValorDolares, ValorComercial, ValorAduanal, ValorME, Factura, FechaFactura, PedimentoOriginal, DescargaDirigida, TASAIGIE, FPIGIE, TASAIVA, FPIVA, CNT, COVE, FACTORINC, FME, ESACTIVO, PESOBRUTO, APARTADO, Descarga, INCOTERM, FECHAENTRADA, NICO, CargaKey)
VALUES
('15', '8010', '0001234', 'A1', 1, 1, 17.5, '2026-08-10', 'CL001', 'MUEBLES FINOS DE EXPORTACION SA DE CV', 0, 16.0, 0, 0, 1, 'MAD-PINO-001', 'MADERA DE PINO TRATADA CLASE A', '4407.11', 100, 'PZA', 3.0, 'M3', 'USA', 'MEX', 1500.00, 26250.00, 26000.00, 1485.71, 'FAC-0001', '2026-08-05', NULL, 'N', 0, 1, 16.0, 1, 0, 'COVE-0001', 1.0, 0.056, 'S', 400.00, 'C', 'N', 'FOB', '2026-08-10', 'N', 1),
('15', '8010', '0001235', 'A1', 1, 1, 17.5, '2026-08-12', 'CL002', 'INDUSTRIAS MADERERAS DEL NORTE SA', 0, 16.0, 0, 0, 1, 'MAD-ROBLE-002', 'MADERA DE ROBLE CLASE B', '4407.91', 50, 'PZA', 2.0, 'M3', 'CAN', 'MEX', 2000.00, 35000.00, 34800.00, 2000.00, 'FAC-0002', '2026-08-06', NULL, 'N', 0, 1, 16.0, 1, 0, 'COVE-0002', 1.0, 0.056, 'S', 350.00, 'C', 'N', 'FOB', '2026-08-12', 'N', 2);
GO

-- 2.3 cargafactura (facturas de exportacion)
IF EXISTS (SELECT 1 FROM cargafactura) TRUNCATE TABLE cargafactura;
GO
INSERT INTO cargafactura (NumeroFactura, FechaFactura, CodigoCliente, NombreCliente, CodigoProducto, DescripcionProducto, Cantidad, Unidad, ValorDolares, ValorPesos, PaisDestino, PO, Fraccion, CantidadTarifa, UnidadTarifa, Partida)
VALUES
('EXP-1001', '2026-09-01', 'CL001', 'MUEBLES FINOS DE EXPORTACION SA DE CV', 'SILL-2026-01', 'SILLA EJECUTIVA MADERA Y TAPIZ', 40, 'PZA', 8000.00, 140000.00, 'USA', 'PO-1001', '9401.71', 40, 'PZA', 1),
('EXP-1002', '2026-09-02', 'CL002', 'INDUSTRIAS MADERERAS DEL NORTE SA', 'MESA-2026-02', 'MESA DE CENTRO ROBLE 110CM', 25, 'PZA', 8750.00, 153125.00, 'USA', 'PO-1002', '9403.60', 25, 'PZA', 1),
('EXP-1003', '2026-09-03', 'CL001', 'MUEBLES FINOS DE EXPORTACION SA DE CV', 'SOFA-2026-03', 'SOFA DE 3 PLAZAS TAPIZADO', 15, 'PZA', 6000.00, 105000.00, 'CAN', 'PO-1003', '9401.61', 15, 'PZA', 1);
GO

-- 2.4 Constanciatransf (constancias de transferencia)
IF EXISTS (SELECT 1 FROM Constanciatransf) TRUNCATE TABLE Constanciatransf;
GO
INSERT INTO Constanciatransf (NUMERODEFOLIO, PERIODO, SEC, FECHACREACION, PROV, LIN, NOPARTE, DESCRIPCION, CANTIDAD, PEDIMENTO, ADUANA, FECHAPEDIMENTO, PAI, Val_dolares, Val_Comercial)
VALUES
('CT-2026-0001', '2026-08-01', '1', '2026-08-15', 'PR001', 1, 'MAD-PINO-001', 'MADERA DE PINO TRATADA CLASE A', 60, '15 80 1000 00001234', '15', '2026-08-10', 'USA', 900.00, 15750.00),
('CT-2026-0002', '2026-08-01', '1', '2026-08-16', 'PR003', 1, 'MAD-ROBLE-002', 'MADERA DE ROBLE CLASE B', 30, '15 80 1000 00001235', '15', '2026-08-12', 'CAN', 1200.00, 21000.00);
GO

-- 2.5 Cargaboom (ordenes de produccion)
IF EXISTS (SELECT 1 FROM Cargaboom) TRUNCATE TABLE Cargaboom;
GO
INSERT INTO Cargaboom (Boomkey, Ordendeproduccion, CostElement, CostElementtext, Origin, Origintext, Totaltargetqty, TotalActualqty, Targetactualqtyvar, Percentagetargetactqty, Currency)
VALUES
(1, 'OP-2026-0001', '2400', 'MATERIAL PRIMA', 'PL01', 'PLANTA REYNOSA', 100, 85, -15, 85.0, 'USD'),
(2, 'OP-2026-0002', '2400', 'MATERIAL PRIMA', 'PL01', 'PLANTA REYNOSA', 60, 60, 0, 100.0, 'USD');
GO

-- 2.6 cargaclave (claves de pedimento)
IF EXISTS (SELECT 1 FROM cargaclave) TRUNCATE TABLE cargaclave;
GO
INSERT INTO cargaclave (Clave, tipopedimento, tipooperacion)
VALUES
('A1', 1, 1),
('A4', 4, 1),
('F4', 4, 2),
('V1', 1, 6);
GO

-- ============================================
-- 3. OPERACIONALES
-- ============================================

-- 3.1 Importaciones (cabeceras de pedimento importacion)
IF EXISTS (SELECT 1 FROM Importaciones) TRUNCATE TABLE Importaciones;
GO
INSERT INTO Importaciones (Ipedimentokey, Aduana, Patente, Numero_ped, Fecha, TipoOper, CveProveedor, Cve_pedimento, Recibe, Tratado, programa, igipagado, FactorInc, tc, iva, dta, advalorem, porcentaje, FactorME, almacenkey, prevalidacion, TipoPed, TipoOperacion, TOper, FechaPresentacion, ValorComercialtotalDlls, Otrosincrementables, Fletes, Seguros, Embalajes, VComTotalPrecioPagado, VAduanatotal, DESCARGA, CNT, NombreProveedor, TaxID, ValidadoVu, IVA_PRE, MULTAS, RECARGOS, Fecha_ad, PesoTotalBruto)
VALUES
(1001, '15', '8010', '0001234', '2026-08-10', '01', 'PR001', 'A1', 'PL01', 'TLCAN', 'IMMEX', 0, 1.0, 17.5, 16.0, 0, 0, 0, 0.056, 2, 0, 1, '01', 1, '2026-08-10', 1500.00, 0, 50.00, 10.00, 5.00, 1565.00, 1560.00, 'S', 1, 'SUPPLIER ONE INC', 'US-88-1234567', 'S', 0, 0, 0, '2026-08-10', 400.00),
(1002, '15', '8010', '0001235', '2026-08-12', '01', 'PR003', 'A1', 'PL01', 'TLCAN', 'IMMEX', 0, 1.0, 17.5, 16.0, 0, 0, 0, 0.056, 2, 0, 1, '01', 1, '2026-08-12', 2000.00, 0, 60.00, 15.00, 8.00, 2083.00, 2080.00, 'S', 1, 'MADERAS TRATADAS SA DE CV', 'MTT970404WWW', 'S', 0, 0, 0, '2026-08-12', 350.00);
GO

-- 3.2 partidas (lineas de pedimento importacion)
IF EXISTS (SELECT 1 FROM partidas) TRUNCATE TABLE partidas;
GO
INSERT INTO partidas (Partidakey, Clave, Descripcion, Fraccion, Cantidad, Unidad, Val_aduanal, Val_dolares, Origen, Arancel, Importacionlink, Saldo, Categoria, Proveedor, CantidadT, UnidadT, TipoMat, Val_comercial, Lote, Factura, Cantotala, Cantotalb, Cantotalc, FacInc, isprosec, ValorME, partida, Montoigi, Fpigi, Montoiva, Fpiva, TasaCCompen, MontoCCompen, FpCCompen, Tasaiva, ALMACENKEY, FechaFactura, TipoTasaIGIE, llave, pesobruto, paisvendedor, taxid, PNUMERO_PED, PTC, PDESCARGA, PFECHAIMPO, PFECHAVENCE, ORDENDESCARGA, EsActivo, FECHAIMPO, FECHAVENCE, PEDIMENTO, tc, fme, CantidadCOriginal, MermaD, DesperdicioD, COVE, USUARIO, unidadPedimento, CantidadPedimento, INCOTERM, NICO, fpIEPS, montoIEPS)
VALUES
(2001, 'MAD-PINO-001', 'MADERA DE PINO TRATADA CLASE A', '4407.11', 100, 'PZA', 1560.00, 1500.00, 'USA', 0, 1001, 100, 'A', 'PR001', 3.0, 'M3', 'MP', 26250.00, 'LOTE-001', 'FAC-0001', 100, 100, 0, 1.0, 0, 1485.71, 1, 0, 1, 249.60, 1, 0, 0, 0, 16.0, 2, '2026-08-05', 'AD-VALOREM', '1580100001234', 400.00, 'USA', 'US-88-1234567', '0001234', 17.5, 'N', '2026-08-10', '2027-08-10', 10, 'S', '2026-08-10', '2027-08-10', '15 8010 0001234', 17.5, 0.056, 100, 0, 0, 'COVE-0001', 'USER01', 'PZA', 100, 'FOB', 'N', 0, 0),
(2002, 'MAD-ROBLE-002', 'MADERA DE ROBLE CLASE B', '4407.91', 50, 'PZA', 2080.00, 2000.00, 'CAN', 0, 1002, 50, 'A', 'PR003', 2.0, 'M3', 'MP', 35000.00, 'LOTE-002', 'FAC-0002', 50, 50, 0, 1.0, 0, 2000.00, 1, 0, 1, 332.80, 1, 0, 0, 0, 16.0, 2, '2026-08-06', 'AD-VALOREM', '1580100001235', 350.00, 'CAN', 'MTT970404WWW', '0001235', 17.5, 'N', '2026-08-12', '2027-08-12', 10, 'S', '2026-08-12', '2027-08-12', '15 8010 0001235', 17.5, 0.056, 50, 0, 0, 'COVE-0002', 'USER01', 'PZA', 50, 'FOB', 'N', 0, 0);
GO

-- 3.3 salidas (pedimento de exportacion)
IF EXISTS (SELECT 1 FROM salidas) TRUNCATE TABLE salidas;
GO
INSERT INTO salidas (SalidaKey, Tipo_operacion, Aduana, Agente, Documento, Fecha, Cve_cliente, Cve_pedimento, Pais, Origen, DocAduanero, FechaDocA, FechaEntrada, PruebaSuf, TotalMonto, TCDocAduanero, Tc, dta, FactorME, Almacenkey, bloqueado, AduanaDespacho, PesoTotalBruto, ValorComercialTotalDlls, VComTotalPrecioPagado, VAduanatotal, Otrosincrementables, Fletes, Seguros, Embalajes, DESCARGA, TipoPedimento, TipoOperacion, CNT, TIPO_F4, USUARIO, IVA, IGIE, MULTAS, RECARGOS, IDENTIFICADOR)
VALUES
(3001, 'EXPORTACION', '15', 'AG001', 'EXP-1001', '2026-09-05', 'CL001', 'F4', 'USA', 'PL01', '15 8010 0005678', '2026-09-05', '2026-09-03', 'RETORNO-001', 140000.00, 17.5, 17.5, 0, 0.056, 1, 0, '15', 2000.00, 8000.00, 8000.00, 7800.00, 0, 100.00, 50.00, 20.00, 'S', 4, 2, 1, 'F4', 'USER01', 16.0, 0, 0, 0, 'X'),
(3002, 'EXPORTACION', '15', 'AG002', 'EXP-1002', '2026-09-06', 'CL002', 'F4', 'USA', 'PL01', '15 8010 0005679', '2026-09-06', '2026-09-04', 'RETORNO-002', 153125.00, 17.5, 17.5, 0, 0.056, 1, 0, '15', 1800.00, 8750.00, 8750.00, 8500.00, 0, 120.00, 60.00, 25.00, 'S', 4, 2, 1, 'F4', 'USER01', 16.0, 0, 0, 0, 'X');
GO

-- 3.4 psalidas (lineas de salida de exportacion)
IF EXISTS (SELECT 1 FROM psalidas) TRUNCATE TABLE psalidas;
GO
INSERT INTO psalidas (Psalidakey, Factura, Fecha, Fraccion, Descripcion, Cantidad, Unidad, Val_pesos, Val_dolares, Salidalink, ArancelTLCAN, Clave, Factura2, ValManoObra, ValMatPrima, ValorMOU, Anexo, montoigi, fpigi, montoiva, fpiva, valorcomercial, valoragregado, partida, paisd, paisc, pesobruto, unidadt, cantidadt, CodProveedor, bloqueado, descargaDesp, Folio, pu, cve_cliente, ESTRUCTURAKEY, No_ParteCli, INCOTERM, FCreacion, NICO)
VALUES
(4001, 'EXP-1001', '2026-09-05', '9401.71', 'SILLA EJECUTIVA MADERA Y TAPIZ', 40, 'PZA', 140000.00, 8000.00, 3001, 0, 'SILL-2026-01', 'EXP-1001', 20000.00, 50000.00, 30000.00, 'A', 0, 1, 0, 1, 8000.00, 30000.00, 1, 'USA', 'USA', 2000.00, 'PZA', 40, 'PR001', 0, 0, 'F-000001', 350.00, 'CL001', 1, 'SILL-CL001', 'FOB', '2026-09-05', 'N'),
(4002, 'EXP-1002', '2026-09-06', '9403.60', 'MESA DE CENTRO ROBLE 110CM', 25, 'PZA', 153125.00, 8750.00, 3002, 0, 'MESA-2026-02', 'EXP-1002', 15000.00, 40000.00, 25000.00, 'A', 0, 1, 0, 1, 8750.00, 25000.00, 1, 'USA', 'USA', 1800.00, 'PZA', 25, 'PR003', 0, 0, 'F-000002', 612.50, 'CL002', 2, 'MESA-CL002', 'FOB', '2026-09-06', 'N');
GO

-- 3.5 descarga (vinculo importacion -> exportacion)
IF EXISTS (SELECT 1 FROM descarga) TRUNCATE TABLE descarga;
GO
INSERT INTO descarga (Entradalink, Pentradalink, Salidalink, Psalidalink, CantUtil, Unidad, Valor_pesos, Valor_Dolares, Origen, Impuestos_causados, Desperdicio, Val_desperdicio, Merma, Val_merma, Tasa, CantUtilT, UnidadT, MermaT, DesperdicioT, linea, orden, Importacion, PT, Salida, Clave, PUA, VAD, IVAD)
VALUES
(1001, 2001, 3001, 4001, 60, 'PZA', 900.00, 1560.00, 'D', 0, 0, 0, 3, 45.00, 1.0, 1.8, 'M3', 0.09, 0.06, 1, 10, '15 8010 0001234', 'SILL-2026-01', 'EXP-1001', 'MAD-PINO-001', 15.00, 900.00, 144.00),
(1001, 2001, 3001, 4001, 40, 'PZA', 600.00, 1040.00, 'D', 0, 0, 0, 2, 30.00, 1.0, 1.2, 'M3', 0.06, 0.04, 2, 10, '15 8010 0001234', 'SILL-2026-01', 'EXP-1001', 'MAD-PINO-001', 15.00, 600.00, 96.00),
(1002, 2002, 3002, 4002, 40, 'PZA', 1600.00, 2080.00, 'D', 0, 0, 0, 2, 40.00, 1.0, 1.6, 'M3', 0.08, 0.04, 3, 10, '15 8010 0001235', 'MESA-2026-02', 'EXP-1002', 'MAD-ROBLE-002', 40.00, 1600.00, 256.00);
GO

-- 3.6 glosa (pedimentos glosa de aduana)
IF EXISTS (SELECT 1 FROM glosa) TRUNCATE TABLE glosa;
GO
INSERT INTO glosa (NumPedimento, NumLinea, PedimentoArmado, Patente, Pedimento, S_Adu, T_Oper, Cve_Docto, R_F_C, Contribuyente, Fec_Pago_Real, Tipo_Ped, Fraccion, Sec, Descripcion, Pais_OD, Pais_CV, Valor_en_Aduana, Valor_Comercial, CantU_M_T, Llave, c2fp0, c3fp9, c6fp3, CantU_M_C, ClaveUMC, PesoBruto, TipoCambio, ValorAgrMaquila, ExisteVinculacion, MetodoValoracion, FormaPagoRecargos, ImporteRecargos, ClaveUMT, ClaveMTransporte)
VALUES
(158010001234, 1, '15 8010 0001234', '8010', '0001234', '15', 1, 'CAP', 'MFE940101XXX', 'MUEBLES FINOS DE EXPORTACION SA DE CV', '2026-08-10', 1, '4407.11', 1, 'MADERA DE PINO TRATADA CLASE A', 'USA', 'MEX', 1560.00, 15750.00, 100, '1580100001234-440711-1', 0, 0, 0, 100, 'PZA', 400.00, 17.5, 0, 0, 1, 0, 0, 'PZA', 0),
(158010001235, 1, '15 8010 0001235', '8010', '0001235', '15', 1, 'CAP', 'IMN950202YYY', 'INDUSTRIAS MADERERAS DEL NORTE SA', '2026-08-12', 1, '4407.91', 1, 'MADERA DE ROBLE CLASE B', 'CAN', 'MEX', 2080.00, 21000.00, 50, '1580100001235-440791-1', 0, 0, 0, 50, 'PZA', 350.00, 17.5, 0, 0, 1, 0, 0, 'PZA', 0);
GO

-- 3.7 CTM (control de transferencia de materiales)
IF EXISTS (SELECT 1 FROM CTM) TRUNCATE TABLE CTM;
GO
INSERT INTO CTM (CTMKEY, FOLIO, PERIODO, SEC, No_PARTECLIctm, No_PARTESAPctm, DESCRIPCIONctm, APARTADO, FRACCION, CANTIDAD, UNIDAD, VAL_USD, PUctm, No_CLIENTE, NOMBRECLI)
VALUES
(1, 'CTM-2026-0001', '2026-08', '1', 'SILL-CL001', 'SILL-2026-01', 'SILLA EJECUTIVA MADERA Y TAPIZ', 'C', '9401.71', 40, 'PZA', 8000.00, 200.00, 'CL001', 'MUEBLES FINOS DE EXPORTACION SA DE CV'),
(2, 'CTM-2026-0002', '2026-08', '1', 'MESA-CL002', 'MESA-2026-02', 'MESA DE CENTRO ROBLE 110CM', 'C', '9403.60', 25, 'PZA', 8750.00, 350.00, 'CL002', 'INDUSTRIAS MADERERAS DEL NORTE SA');
GO

-- 3.8 dirigido (descarga dirigida)
IF EXISTS (SELECT 1 FROM dirigido) TRUNCATE TABLE dirigido;
GO
INSERT INTO dirigido (dirigidokey, documento, clave, incorporado, desperdicio, merma, salidakey, psalidakey, valorcomercial, PU, Fraccion, saldo, Factura)
VALUES
(1, 'EXP-1001', 'MAD-PINO-001', 60, 0, 3, 3001, 4001, 900.00, 15.00, '4407.11', 40, 'EXP-1001'),
(2, 'EXP-1002', 'MAD-ROBLE-002', 40, 0, 2, 3002, 4002, 1600.00, 40.00, '4407.91', 10, 'EXP-1002');
GO

-- 3.9 trazo
IF EXISTS (SELECT 1 FROM trazo) TRUNCATE TABLE trazo;
GO
INSERT INTO trazo (psalidakey, producto, cantidad, linea, descargo, falto, padre, salidakey, PRODMATKEY, CLAVEORIGINAL)
VALUES
(4001, 'SILL-2026-01', 40, 1, 40, 0, 'SILL-2026-01', 3001, 1, 'SILL-2026-01'),
(4002, 'MESA-2026-02', 25, 1, 25, 0, 'MESA-2026-02', 3002, 4, 'MESA-2026-02');
GO

-- 3.10 FACTURA (facturas creadas)
IF EXISTS (SELECT 1 FROM FACTURA) TRUNCATE TABLE FACTURA;
GO
INSERT INTO FACTURA (FACTURAKEY, CODIGO, DESCRIPCION, TIPOVTA, FACTURA, FECHA, CANT, UM, PEDIMENTO, CLIENTE, TIPOPROD, REFERENCIA)
VALUES
(5001, 'SILL-2026-01', 'SILLA EJECUTIVA MADERA Y TAPIZ', 'E', 'EXP-1001', '2026-09-05', 40, 'PZA', '15 8010 0001234', 'CL001', 'PT', 'F-000001'),
(5002, 'MESA-2026-02', 'MESA DE CENTRO ROBLE 110CM', 'E', 'EXP-1002', '2026-09-06', 25, 'PZA', '15 8010 0001235', 'CL002', 'PT', 'F-000002');
GO

-- 3.11 DescargaDesp (desperdicios)
IF EXISTS (SELECT 1 FROM DescargaDesp) TRUNCATE TABLE DescargaDesp;
GO
INSERT INTO DescargaDesp (DescargaDespKey, Descargalink, Cantidad, Importacionlink, partidalink, salidalink, psalidalink, ValorDesperdicio, DocDestlink)
VALUES
(1, 1, 0.06, 1001, 2001, 3001, 4001, 45.00, 1);
GO

-- 3.12 Perfil de salidas de CTM (CTMDESCARGA)
IF EXISTS (SELECT 1 FROM CTMDESCARGA) TRUNCATE TABLE CTMDESCARGA;
GO
INSERT INTO CTMDESCARGA (CTM, [FECHA CTM], PRODUCTO, [DESCRIPCION PRODUCTO], MATERIAL, [DESCRIPCION MATERIAL], DESCARGADO, IMPORTACION, [FECHA IMPORTACION], DOCUMENTOCR, [PEDIMENTOSEXPORT], [FECHA PAGO CTM], ANEXO, [CLAVE CLIENTE], CVE_PEDIMENTO)
VALUES
('CTM-2026-0001', '2026-08-31', 'SILL-2026-01', 'SILLA EJECUTIVA MADERA Y TAPIZ', 'MAD-PINO-001', 'MADERA DE PINO TRATADA CLASE A', 60, '15 8010 0001234', '2026-08-10', 'EXP-1001', '15 8010 0005678', '2026-09-05', 'A', 'CL001', 'F4');
GO

-- ============================================
-- 4. HISTORIAL Y COMPLEMENTOS
-- ============================================

-- 4.1 HistoriaDescarga (historial de descargas por material)
IF EXISTS (SELECT 1 FROM HistoriaDescarga) TRUNCATE TABLE HistoriaDescarga;
GO
INSERT INTO HistoriaDescarga (HistoriaDescargakey, PedimentoEntrada, FechaEntrada, ClavePedimentoEntrada, NoParte, Descripcion, Cantidad, saldo, origen, Incorporado, Desperdiciado, Mermado, fraccion, TipoOperacion, NumeroPedimento, FechaSalida, ClavePedimentoExpo, CodigoSalida, CantidadExportada, Factura, FechaFactura, TipoColumna, PedimentoES, ClavePedimentoES, NumeroParteES, componente, mes, anio, fechaES, DescripcionSalida, Partida, ValorComercialImportacion, ValorAduanalImportacion, ValorComercialExportacion, lote, FraccionExportacion, PartidaExportacion, PaisDestinoExportacion, PaisOrigenImportacion, Anexo, INCORPORADOT, UMT, Unidad, Aduana, Patente, Pu, AduanaE, PatenteE, claveUMCE, PuE, ValorComercialE, ClavePaisDE, CantidadDescargadaE, ValoragregadoE, CANTIDADT, UNIDADT, FACTURAIMPO, FECHAPAGOI, VALORCOMERCIALDESCARGADO)
VALUES
(1, '15 8010 0001234', '2026-08-10', 'A1', 'MAD-PINO-001', 'MADERA DE PINO TRATADA CLASE A', 100, 40, 'D', 60, 0, 3, '4407.11', 'IMPORTACION', '15 8010 0005678', '2026-09-05', 'F4', 'EXP-1001', 40, 'EXP-1001', '2026-09-05', 'DESCARGA', '15 8010 0005678', 'F4', 'SILL-2026-01', 'MAD-PINO-001', 9, 2026, '2026-09-05', 'SILLA EJECUTIVA MADERA Y TAPIZ', 1, 900.00, 1560.00, 8000.00, 'LOTE-001', '9401.71', 1, 'USA', 'USA', 'A', 60, 'M3', 'PZA', '15', '8010', 15.00, '15', '8010', 'PZA', 200.00, 8000.00, 'USA', 60, 500.00, 1.8, 'M3', 'FAC-0001', '2026-08-10', 900.00),
(2, '15 8010 0001235', '2026-08-12', 'A1', 'MAD-ROBLE-002', 'MADERA DE ROBLE CLASE B', 50, 10, 'D', 40, 0, 2, '4407.91', 'IMPORTACION', '15 8010 0005679', '2026-09-06', 'F4', 'EXP-1002', 25, 'EXP-1002', '2026-09-06', 'DESCARGA', '15 8010 0005679', 'F4', 'MESA-2026-02', 'MAD-ROBLE-002', 9, 2026, '2026-09-06', 'MESA DE CENTRO ROBLE 110CM', 1, 1600.00, 2080.00, 8750.00, 'LOTE-002', '9403.60', 1, 'USA', 'CAN', 'A', 40, 'M3', 'PZA', '15', '8010', 40.00, '15', '8010', 'PZA', 350.00, 8750.00, 'USA', 40, 500.00, 1.6, 'M3', 'FAC-0002', '2026-08-12', 1600.00);
GO

-- 4.2 HistoriaDescargaSalida (historial por salida)
IF EXISTS (SELECT 1 FROM HistoriaDescargaSalida) TRUNCATE TABLE HistoriaDescargaSalida;
GO
INSERT INTO HistoriaDescargaSalida (TipoColumna, TipoOperacion, PedimentoSalida, FechaSalida, ClavePedimentoSalida, CodigoProducto, PartidaSalida, FraccionSalida, Descripcion, CantidadSalida, destino, Incorporado, Desperdiciado, Mermado, TotalDescargado, NumeroPedimentoEntrada, FechaEntrada, ClavePedimentoEntrada, CodigoEntrada, CantidadImportada, FacturaImportacion, FechaFacturaImportacion, Salida, DescripcionEntrada, DescripcionSalida, lote, FacturaExportacion, FechaFacturaExportacion, FALTO, APARTADO, VALORDELADESCARGA, Fpigi, Tasaigi, VALORDESCARGAIGI, fechaVigencia, VALORCOMERCIALDELADESCARGA, PartidaEntrada, FraccionEntrada, UMCEntrada, ValorAduanaEntrada, UMCSALIDA, ValorComercialSalida, ValorComercialentrada, PU, ValorAduanaSalida, ORIGEN)
VALUES
('DESCARGA', 'EXPORTACION', '15 8010 0005678', '2026-09-05', 'F4', 'SILL-2026-01', 1, '9401.71', 'SILLA EJECUTIVA MADERA Y TAPIZ', 40, 'USA', 60, 0, 3, 60, '15 8010 0001234', '2026-08-10', 'A1', 'MAD-PINO-001', 100, 'FAC-0001', '2026-08-05', 'EXP-1001', 'MADERA DE PINO TRATADA CLASE A', 'SILLA EJECUTIVA MADERA Y TAPIZ', 'LOTE-001', 'EXP-1001', '2026-09-05', 0, 'C', 900.00, 1, 16.0, 144.00, '2027-08-10', 900.00, 1, '4407.11', 'PZA', 1560.00, 'PZA', 8000.00, 26250.00, 15.00, 7800.00, 'D'),
('DESCARGA', 'EXPORTACION', '15 8010 0005679', '2026-09-06', 'F4', 'MESA-2026-02', 1, '9403.60', 'MESA DE CENTRO ROBLE 110CM', 25, 'USA', 40, 0, 2, 40, '15 8010 0001235', '2026-08-12', 'A1', 'MAD-ROBLE-002', 50, 'FAC-0002', '2026-08-06', 'EXP-1002', 'MADERA DE ROBLE CLASE B', 'MESA DE CENTRO ROBLE 110CM', 'LOTE-002', 'EXP-1002', '2026-09-06', 0, 'C', 1600.00, 1, 16.0, 256.00, '2027-08-12', 1600.00, 1, '4407.91', 'PZA', 2080.00, 'PZA', 8750.00, 35000.00, 40.00, 8500.00, 'D');
GO

-- 4.3 Comprobante (validacion VUCEM)
IF EXISTS (SELECT 1 FROM comprobante) TRUNCATE TABLE comprobante;
GO
INSERT INTO comprobante (comprobante_tipoidentificador, comprobante_identificacion, comprobante_fechaexpedicion, comprobante_certificadoorigen, comprobante_numeroexportadorconfiable, comprobante_observaciones, comprobante_rfcconsulta, comprobante_subdivision, comprobante_tipofiguraaduanal, comprobante_correoelectronico, comprobante_rfcregistro, comprobante_numerofacturaoriginal, emisor_key, emisor_tipoidentificador, emisor_identificacion, emisor_apellidopaterno, emisor_nombre, emisor_domiciliocalle, emisor_domicilionumeroexterior, emisor_domiciliocolonia, emisor_domiciliomunicipio, emisor_domicilioentidadfederativa, emisor_domiciliopais, emisor_domiciliocodigopostal, destinatario_key, destinatario_tipoidentificador, destinatario_identificacion, destinatario_nombre, destinatario_domiciliocalle, destinatario_domicilionumeroexterior, destinatario_domiciliomunicipio, destinatario_domicilioentidadfederativa, destinatario_domiciliopais, destinatario_domiciliocodigopostal, comprobante_status, comprobante_tipooperacion, comprobante_patente, usuario, fechaCreacion, UltimaModificacion, comprobante_Pedimento, comprobante_ClavePedimento, comprobante_RegistroImmex, comprobante_Regimen, comprobante_Transportista, comprobante_ScacCode, comprobante_Trailer, comprobante_INCOTERM, comprobante_RegistroProsec, comprobante_comentarios, comprobante_moneda, tc, fme, comprobante_po, referencia, comprobante_sellos)
VALUES
('RFC', 'EMA960101XXX', '2026-08-15', 'CERT-2026-001', NULL, 'COMPROBANTE DE VALIDACION PRUEBA', 'EMA960101XXX', 'SUB-01', 'TRANSPORTISTA', 'prueba@ejemplo.mx', 'EMA960101XXX', 'FAC-0001', 1, 'RFC', 'SUP123456789', 'SUPPLIER', 'SUPPLIER ONE INC', '1000 INDUSTRIAL PKWY', '1000', 'INDUSTRIAL', 'AUSTIN', 'TEXAS', 'USA', '78701', 1, 'RFC', 'MFE940101XXX', 'MUEBLES FINOS DE EXPORTACION SA DE CV', 'AV LAZARO CARDENAS', '500', 'MONTERREY', 'NUEVO LEON', 'MEXICO', '64000', 'VALIDADO', 'IMPORTACION', '8010', 'USER01', '2026-08-15', '2026-08-15', '15 8010 0001234', 'A1', 'IMMEX202400123', 'IMMEX', 'TRANSPORTES DE PRUEBA SA', 'SCAC-TEST', 'TRAILER-01', 'FOB', NULL, 'COMPROBANTE GENERADO EN PRUEBAS', 'USD', 17.5, 0.056, 'PO-1001', 'REF-0001', 'SELLO-0001');
GO

-- 4.4 FacturasCreadas (facturas generadas por el sistema)
IF EXISTS (SELECT 1 FROM FacturasCreadas) TRUNCATE TABLE FacturasCreadas;
GO
INSERT INTO FacturasCreadas (FolioFact, TipoOperacion, TC, Transporte, Patente, PedimentoArmado, RegistroIMMEX, EmisorNombre, EmisorTAXID, Folio, Aduana, CVEDOCTO, Fecha, DestinatarioNombre, DestinatarioTAXID, Origen, ClaveMercancia, Cantidad, Unidad, Importe, Moneda, Fraccion, Peso, Serial, Marca, Modelo, ClaveEmisor, ProgramaEmisor, CalleNumeroEmisor, ColoniaEmisor, NumicipioEmisor, EntidadEmisor, CodigoPostalEmiso, PaisEmisor, ClaveDestinatario, ProgramaDestinatario, CalleNumeroDestinatario, ColoniaDestinatario, NumicipioDestinatario, EntidadDestinatario, CodigoPostalDestinatario, PaisDestinatario, LocalidadEmisor, LocalidadDestinatario, Descripcion, Subdivision, Comentarios, PackingSlip, Observaciones, Vinculacion, INCOTERM, BULTOS, SEGUROS, FLETES, EMBALAJES, OTROS, CANTIDADT, UNIDADT, VALORACION, VALOR_AGREGADO, MONEDA_VALOR_AGREGADO, SUBMODELO, PO)
VALUES
('F-000001', 'EXPORTACION', 17.5, 'CARRETERO', '8010', '15 8010 0005678', 'IMMEX202400123', 'MUEBLES FINOS DE EXPORTACION SA DE CV', 'MFE940101XXX', 'EXP-1001', '15', 'CAP', '2026-09-05', 'BUYER USA LLC', 'US-55-9876543', 'PL01', 'SILL-2026-01', 40, 'PZA', 140000.00, 'MXN', '9401.71', 2000.00, 'SER-0001', 'MADERAMAQ', 'SILL-2026', 'MFE940101XXX', 'IMMEX', 'AV LAZARO CARDENAS 500', 'CENTRO', 'MONTERREY', 'NUEVO LEON', '64000', 'MEXICO', 'CL001', 'IMMEX', '100 MAIN ST', 'DOWNTOWN', 'DALLAS', 'TEXAS', '75001', 'USA', 'MONTERREY', 'DALLAS', 'SILLA EJECUTIVA MADERA Y TAPIZ', 'SUB-01', 'FACTURA DE EXPORTACION PRUEBA', 1, 'PEDIMENTO ORIGINAL', 'A1', 'FOB', 40, 100.00, 50.00, 20.00, 0, 40, 'PZA', 'VALOR TRANSACCION', 30000.00, 'MXN', 'SUB-01', 'PO-1001'),
('F-000002', 'EXPORTACION', 17.5, 'CARRETERO', '8010', '15 8010 0005679', 'IMMEX202400123', 'INDUSTRIAS MADERERAS DEL NORTE SA', 'IMN950202YYY', 'EXP-1002', '15', 'CAP', '2026-09-06', 'CANADIAN BUYER LTD', 'CA-88-1112223', 'PL01', 'MESA-2026-02', 25, 'PZA', 153125.00, 'MXN', '9403.60', 1800.00, 'SER-0002', 'MADERAMAQ', 'MESA-2026', 'IMN950202YYY', 'IMMEX', 'CARR NACIONAL KM 12 800', 'INDUSTRIAL', 'SALTILLO', 'COAHUILA', '25280', 'MEXICO', 'CL002', 'IMMEX', '2000 BAY ST', 'HARBOUR', 'TORONTO', 'ONTARIO', 'M5V', 'CANADA', 'SALTILLO', 'TORONTO', 'MESA DE CENTRO ROBLE 110CM', 'SUB-01', 'FACTURA DE EXPORTACION PRUEBA', 1, 'PEDIMENTO ORIGINAL', 'A1', 'FOB', 25, 120.00, 60.00, 25.00, 0, 25, 'PZA', 'VALOR TRANSACCION', 25000.00, 'MXN', 'SUB-01', 'PO-1002');
GO

-- 4.5 partidasglosa (partidas glosa por pedimento)
IF EXISTS (SELECT 1 FROM partidasglosa) TRUNCATE TABLE partidasglosa;
GO
INSERT INTO partidasglosa (partidasglosakey, patente, pedimento, sadu, toper, cvedocto, rfc, contribuyente, fecpagoreal, tipoped, fraccion, sec, descripcion, paisod, paiscv, valorenaduana, valorcomercial, cantidadenumt, cantidadenumc)
VALUES
(1, '8010', '0001234', '15', 1, 'CAP', 'MFE940101XXX', 'MUEBLES FINOS DE EXPORTACION SA DE CV', '2026-08-10', 1, '4407.11', 1, 'MADERA DE PINO TRATADA CLASE A', 'USA', 'MEX', 1560.00, 15750.00, 100, 100),
(2, '8010', '0001235', '15', 1, 'CAP', 'IMN950202YYY', 'INDUSTRIAS MADERERAS DEL NORTE SA', '2026-08-12', 1, '4407.91', 1, 'MADERA DE ROBLE CLASE B', 'CAN', 'MEX', 2080.00, 21000.00, 50, 50);
GO

-- 4.6 MensajeDescarga (mensajes de descarga)
IF EXISTS (SELECT 1 FROM MensajeDescarga) TRUNCATE TABLE MensajeDescarga;
GO
INSERT INTO MensajeDescarga (Pedimento, Fecha, Mensaje, salidakey, psalidakey, entradakey, partidakey)
VALUES
('15 8010 0001234', '2026-09-05', 'DESCARGA COMPLETA DE LA PARTIDA', 3001, 4001, 1001, 2001),
('15 8010 0001235', '2026-09-06', 'DESCARGA COMPLETA DE LA PARTIDA', 3002, 4002, 1002, 2002);
GO

-- 4.7 Desperdicios y pdesperdicios
IF EXISTS (SELECT 1 FROM Desperdicios) TRUNCATE TABLE Desperdicios;
GO
INSERT INTO Desperdicios (Desperdiciokey, Destino, Documento, Fecha)
VALUES
(1, 'EXP-1001', 'EXP-1001', '2026-09-05'),
(2, 'EXP-1002', 'EXP-1002', '2026-09-06');
GO
IF EXISTS (SELECT 1 FROM pdesperdicios) TRUNCATE TABLE pdesperdicios;
GO
INSERT INTO pdesperdicios (Pdesperdiciokey, Desperdiciolink, Fraccion, ValorAduana, Cantidad)
VALUES
(1, 1, '4407.11', 45.00, 0.06),
(2, 2, '4407.91', 40.00, 0.04);
GO

-- 4.8 Descarga dirigida CTM y desperdicios
IF EXISTS (SELECT 1 FROM cDescargaDirigida) TRUNCATE TABLE cDescargaDirigida;
GO
INSERT INTO cDescargaDirigida (DocumentoSalida, Sec, Fraccion, ClaveSalida, cantidadDescargar, Unidad, DocumentoEntrada, ClaveEntrada, Incorporado, Desperdico, Merma)
VALUES
('EXP-1001', '1', '4407.11', 'SILL-2026-01', 60, 'PZA', '15 8010 0001234', 'MAD-PINO-001', 60, 0, 3);
GO
IF EXISTS (SELECT 1 FROM DescDirctma) TRUNCATE TABLE DescDirctma;
GO
INSERT INTO DescDirctma (dirigidokey, descargakey, cantidad)
VALUES
(1, 1, 60);
GO
IF EXISTS (SELECT 1 FROM DescDirDesperdicios) TRUNCATE TABLE DescDirDesperdicios;
GO
INSERT INTO DescDirDesperdicios (dirigidokey, descargakey, cantidad)
VALUES
(1, 1, 0.06);
GO

-- 4.9 Errores de carga y validacion
IF EXISTS (SELECT 1 FROM ECargaMaterial) TRUNCATE TABLE ECargaMaterial;
GO
INSERT INTO ECargaMaterial (CargaMaterialKey, error, CargaProductoKey)
VALUES
(3, 'FRACCION ARANCELARIA NO ENCONTRADA EN CATALOGO', NULL);
GO
IF EXISTS (SELECT 1 FROM errCargaFactura) TRUNCATE TABLE errCargaFactura;
GO
INSERT INTO errCargaFactura (error, pedimento, factura, cargaFacturakey)
VALUES
('CANTIDAD TARIFA NO COINCIDE CON UNIDAD', '15 8010 0001234', 'EXP-1001', 1);
GO
IF EXISTS (SELECT 1 FROM errorGlosa) TRUNCATE TABLE errorGlosa;
GO
INSERT INTO errorGlosa (glosakey, error, VA24, VGlosa, Diferencia)
VALUES
(1, 'DIFERENCIA EN VALOR COMERCIAL', 15750.00, 15700.00, 50.00);
GO

-- 4.10 Validacion inventario inicial
IF EXISTS (SELECT 1 FROM ValidacionInventarioInicial) TRUNCATE TABLE ValidacionInventarioInicial;
GO
INSERT INTO ValidacionInventarioInicial (Tipo, patente, pedimento, aduana, fechaEntrada, fraccion, valorComercial, IndentifiacadorAF)
VALUES
(1, '8010', '0001234', '15', '2026-08-10', '4407.11', 15750.00, 'N'),
(1, '8010', '0001235', '15', '2026-08-12', '4407.91', 21000.00, 'N');
GO
IF EXISTS (SELECT 1 FROM ErroresValidacionInventarioInicial) TRUNCATE TABLE ErroresValidacionInventarioInicial;
GO
INSERT INTO ErroresValidacionInventarioInicial (descripcionError, Pedimentoarmado, Validacionkey)
VALUES
('INVENTARIO INICIAL NO VALIDADO POR VU', '15 8010 0001234', 1);
GO

PRINT '=== SEED CALE_IMMEX COMPLETO ===';
GO