# Procedimientos almacenados - BD CALE_IMMEX

Documentacion generada automaticamente a partir de las definiciones almacenadas en la base de datos.

- **Fecha de generacion**: 15/09/2026 07:13

## Indice por familia

- **A** (1 procs)
- **BLOQUEA** (1 procs)
- **CARGA** (13 procs)
- **COMPULSA/COMPARATIVA** (6 procs)
- **CREA/CONVIERTE** (4 procs)
- **DESCARGA** (12 procs)
- **ESTRUCTURAS** (1 procs)
- **GUARDADESCARGACTMA** (1 procs)
- **HISTORIA** (3 procs)
- **HOJATRABAJOCTMDESPA** (1 procs)
- **HOJATRABAJODESCARGOSA** (1 procs)
- **INDICAOPERACION** (1 procs)
- **INFORME** (1 procs)
- **INSERT** (5 procs)
- **LIGA** (3 procs)
- **LLENAHISTORIADESCARGOSF** (1 procs)
- **NUEVOFOLIOB** (1 procs)
- **PHISTORIAR** (1 procs)
- **PR_COMPULSACANTIDADES** (1 procs)
- **PR_CompulsaDSA** (1 procs)
- **PR_INFORME_ESTRUCTURAS** (1 procs)
- **PR_INFORME_EXPORTACIONES** (1 procs)
- **PR_INFORME_IMPORTACIONES** (1 procs)
- **PR_INFORME_SALDOS** (1 procs)
- **PROC_DESCARGASF** (1 procs)
- **PROC_HISTORIADESCARGASALIDA** (2 procs)
- **PROC_HISTORIADESCARGASALIDAFALTANTES** (1 procs)
- **RETORNO** (1 procs)
- **SALDOS** (5 procs)
- **SP_DESENSAMBLE** (1 procs)
- **SP_DirigidoBOrrados** (1 procs)
- **SP_G** (3 procs)
- **SP_GENERA_TXT_COMPLETO** (1 procs)
- **SP_ImportacionesBorradas** (1 procs)
- **SP_MensajesInicio** (1 procs)
- **SP_PartidasBorradas** (1 procs)
- **SP_PsalidasBorradas** (1 procs)
- **SP_SalidasBorradas** (1 procs)
- **Trazo** (1 procs)
- **VALIDA** (4 procs)

## Resumen

| Procedimiento | Descripcion | Tablas principales |
|---|---|---|
| `A31_SALDOS` | - | A31_DESCARGAS, A31_TRAZO |
| `BLOQUEA_DOCUMENTO` | - | DESCARGOSBLOQUEADOS |
| `CARGA_ENCABEZADOS` | INSERTA IMPORTACIONES. | IMPORTACIONES, I_PEDIMENTO, SALIDAS, I_DETALLENP |
| `CARGA_ESTRUCTURADESENSAMBLE` | Proceso de carga y validacion de datos desde tablas stage. | ESTDESENSAMBLE, MATERIALDESENSAMBLE |
| `CARGA_FACTURAS` | CREA LOS PRODUCTOS QUE FALTEN | productos, TFACTURA, TERRORFACTURA, clientes |
| `CARGA_MATERIALES` | PRIMERO REVISO LAS UNIDADES DE MEDIDA | CARGAMATERIAL, ECARGAMATERIAL, FACTORESMP, MATERIAL |
| `CARGA_PRODUCTOS` | PRIMERO REVISO LAS UNIDADES DE MEDIDA | ECARGAPRODUCTO, tmpproductos |
| `CARGA_SUBMAQUILA` | Proceso de carga y validacion de datos desde tablas stage. |  |
| `CARGAACTAS` | DELETE FROM ERRORACTA | ACTA |
| `CARGAAgentes` | Proceso de carga y validacion de datos desde tablas stage. | ECARGAagentes, TMPagentes, agentes |
| `CARGACLIENTES` | Proceso de carga y validacion de datos desde tablas stage. | ECARGACLIENTES, TMPCLIENTES, ECARGAPROVEEDORES, clientes |
| `CARGACONSTANCIAS` | BORRO LOS ERRORES | ERRORCARGA, CONSTANCIATRANSF, productos, settings |
| `CARGAFACTURASENPSALIDAS` | PRIMERO SELECCIONA LA FACTURA DE LA TABLA DE FACTURAS. | ERRCARGAFACTURA, CARGAFACTURA, PRODUCTOS |
| `CARGAPEDIMENTOS` | BORRO LOS ERRORES | ERRORCARGA, CARGAPEDIMENTOSIE, MATERIAL, PRODUCTOS |
| `CARGAPROVEEDORES` | Proceso de carga y validacion de datos desde tablas stage. | ECARGAPROVEEDORES, TMPPROVEEDORES, Proveedores |
| `COMPARADESCARGAA31` | Compulsa / conciliacion entre fuentes de pedimentos. | A31_COMPARATIVADESCARGA |
| `COMPARATIVADESCARGA31` | Compulsa / conciliacion entre fuentes de pedimentos. | descarga31, comparativa31, salidas, descarga |
| `COMPARATIVADESCARGA31_DETALLE` | Compulsa / conciliacion entre fuentes de pedimentos. | descarga31_DETALLE, comparativa31_DETALLE, salidas, descarga |
| `COMPULSAGLOSA` | GLOSA.PEDIMENTOARMADO = '2430098003953' |  |
| `COMPULSAGLOSA2` | REVISA FRACCION |  |
| `COMPULSAGLOSAFINAL` | VALIDA PARA CADA OPERACION SI ESTA CAPTURADA O NO EN EL SISTEMA | ERRORGLOSA |
| `CONVIERTEDIRIGIDO` | SET NOCOUNT ON ADDED TO PREVENT EXTRA RESULT SETS FROM |  |
| `CONVIERTEDIRIGIDOPED` | SET NOCOUNT ON ADDED TO PREVENT EXTRA RESULT SETS FROM |  |
| `CREAESTRUCTURAS` | - DATOS DEL PRODUCTO | cartademateriales, material, ESTRUCTURAS, PRODUCTOMATERIAL |
| `CREAPRODUCTOSCARGAFACTURA` | - | PRODUCTOS, CARGAFACTURA |
| `DESCAGARA31` | Gestion de descargas de material (aplicacion de importaciones a salidas). | A31_HISTORIADESCARGAS |
| `DESCARGAINICIAL` | Gestion de descargas de material (aplicacion de importaciones a salidas). |  |
| `DESCARGAS_A31` | Gestion de descargas de material (aplicacion de importaciones a salidas). |  |
| `DESCARGASALIDAPEPS` | DECLARACION DE VARIABLES EN EL SISTEMA |  |
| `DESCARGASCTM` | Gestion de descargas de material (aplicacion de importaciones a salidas). |  |
| `DESCARGASCTMF` | Gestion de descargas de material (aplicacion de importaciones a salidas). | CTMDESCARGA |
| `DESCARGATODOSDIRIGIDOS` | BORRA LOS NO BLOQUEADOS |  |
| `DESCARGATSALIDA` | Gestion de descargas de material (aplicacion de importaciones a salidas). |  |
| `DESCARGATSALIDA1` | BORRO EL TRAZO | SETTINGS |
| `DESCARGATSALIDAFECHA` | BORRO EL TRAZO | SETTINGS |
| `DESCARGAXFECHA51` | actualizo el tipo descarga | alternativo, SETTINGS, descargaRespaldo, MensajeDescarga |
| `DESCDIRIGIDA` | PRIMERO CREO EL CURSOR QUE HACE LAS OPERACIONES DIRIGIDAS. |  |
| `ESTRUCTURAS1A1` | - | cartademateriales |
| `GUARDADESCARGACTMA` | UPDATE PARTIDAS SET SALDO = (SELECT SUM(CANTUTIL) FROM DESCARGA D WHERE D.PENTRADALINK ... | descargaCTMBACK, descarga |
| `HISTORIADESCARGAS` | - INSERTA LAS DESCARGAS |  |
| `HISTORIADESCARGASF` | EXEC [dbo].[LIGADESPERDICIOS] | DescargaDesp, DescDirDesperdicios |
| `HISTORIADESCARGASP` | EXEC [dbo].[LIGADESPERDICIOS] | DescargaDesp, DescDirDesperdicios |
| `HOJATRABAJOCTMDESPA31` | - |  |
| `HOJATRABAJODESCARGOSA31` | - |  |
| `INDICAOPERACION` | VERIFICO SI EL PEDIMENTO ES UNA RECTIFICACION. |  |
| `INFORME_CONCENTRADOSALDOS` | - | CONCENTRADOSALDOS |
| `INSERTADIRIGIDOS` | ,dbo.salidas.salidakey | dirigido, cdescargadirigida |
| `INSERTAFALTANTESA31` | ACTUALIZO LAS PARTIDAS QUE SE DESCARGARON CON ERROR PARA QUE AL PONER COMPLETO NO SE CO... | A31_ENTRADAS |
| `INSERTAPEDIMENTO` | VARIABLES DE ENCABEZADO DE SALIDA | ERRORVALIDACION, PEDIMENTOS, RECTIFICACIONEXPORT, SALIDAS |
| `INSERTERROR` | PROCEDURE BODY |  |
| `INSTERTAFACTURASFC` | - | TMPFCERROR, FacturasCreadas, TmpFC |
| `LIGACTMA` | ------------------------------------------------- | DESCARGA_CTMA, V_F4CTMA, DescDirctma |
| `LIGACTMFACTURA` | Vinculacion de facturas / desperdicios con descargas. | FACTURASCTM |
| `LIGADESPERDICIOS` | ESTE EXISTS ES PARA LIMITAR EL PROCESO A AQUELLOS QUE SI TIENEN CAMBIOS DE REGIMEN | DESCARGA_DESPERDICIO, V_F4DESP, DescDirDesperdicios |
| `LLENAHISTORIADESCARGOSF4CTMA` | - | HISTORIAF4CTMA, DescDirctma |
| `NUEVOFOLIOB` | - |  |
| `PHISTORIAR8` | ------- CURSOR DE IMPORTACIONES EN DONDE SE UTILIZO EL PERMISO | HISTORIAR8 |
| `PR_COMPULSACANTIDADES` | INSERTO LAS IMPORTACIONES DEL SACI | COMPULSACANTIDADES |
| `PR_CompulsaDSA24` | PROCESO DE COMPULSA 2020 | CONRIBUCIONES_GENERALES_DS, CONRIBUCIONES_PARTIDA_DS |
| `PR_INFORME_ESTRUCTURAS` | DECLARE @temp TABLE | salidas, psalidas, material, productomaterial |
| `PR_INFORME_EXPORTACIONES` | CREATE PROCEDURE [dbo].[PR_INFORME_EXPORTACIONES] | psalidas, salidas, clientes |
| `PR_INFORME_IMPORTACIONES` | CREATE PROCEDURE [dbo].[PR_INFORME_IMPORTACIONES] | Importaciones, partidas, Proveedores |
| `PR_INFORME_SALDOS` | CREATE PROCEDURE [dbo].[PR_INFORME_SALDOS] | Importaciones, partidas, categorias |
| `PROC_DESCARGASF4CTMA` | - | DESCARGASF4CTMA |
| `PROC_HISTORIADESCARGASALIDA` | AND SALIDAS.CVE_PEDIMENTO='CT' | HISTORIADESCARGASALIDA |
| `PROC_HISTORIADESCARGASALIDA;1` | - | HISTORIADESCARGASALIDA |
| `PROC_HISTORIADESCARGASALIDAFALTANTES` | - | HISTORIADESCARGASALIDA |
| `RETORNO_SUBMAQUINA` | - | partidas |
| `SALDOS` | DATOS PARA LA DESCARGA | productomaterial |
| `SALDOS_FAMILIA` | DATOS PARA LA DESCARGA | productomaterial |
| `SALDOS2` | DATOS PARA LA DESCARGA | GENERADORES |
| `SALDOSCTM` | Calculo de saldos de materiales o partidas. | DESCARGACTMF, TRAZOCTM |
| `SALDOSDIRIGIDOS` | DATOS PARA LA DESCARGA |  |
| `SP_DESENSAMBLE` | - | partidas |
| `SP_DirigidoBOrrados` | - | dirigidoBorrados |
| `SP_G5` | declare @res as varchar(MAX) | G5, v_g5 |
| `SP_G6` | VARIABLES DEL CURSOR                                --VARIABLES DE DATOS | SalidasA31Key, V_G6, Destinatario, G6 |
| `SP_G6_F4` | - | G6 |
| `SP_GENERA_TXT_COMPLETO` | ---------------------------- |  |
| `SP_ImportacionesBorradas` | - | ImportacionesBorradas, PartidasBorradas |
| `SP_MensajesInicio` | Se insertan los materiales | MensajesInicio |
| `SP_PartidasBorradas` | - | PartidasBorradas |
| `SP_PsalidasBorradas` | - | PsalidasBorradas |
| `SP_SalidasBorradas` | - | SalidasBorradas, PsalidasBorradas |
| `Trazo_report` | SELECT @material = '0A4000900-00' |  |
| `VALIDA_I_DETALLENP` | VERIFICO PARTIDA POR PARTIDA. | I_ERROR, I_PEDIMENTO, I_DETALLENP, MATERIAL |
| `VALIDACIONESVU` | INSERT INTO [dbo].[VALIDACIONVU] | VALIDACIONVU |
| `VALIDAF4` | Validacion de datos (pedimentos, descargas, detalle). |  |
| `VALIDAPEDIMENTO` | PRIMERO VALIDO QUE LAS PARTIDAS EXISTAN |  |

## Detalle por procedimiento

### dbo.A31_SALDOS

- **Descripcion**: -
- **Creado**: 2026-09-14 12:54:33
- **Parametros**: @FKEY BIGINT; @FRACCION VARCHAR(10); @FECHA DATETIME; @ESAF VARCHAR(5); @VALOR NUMERIC(18,4)
- **Tablas utilizadas**: A31_DESCARGAS, A31_TRAZO

### dbo.BLOQUEA_DOCUMENTO

- **Descripcion**: -
- **Creado**: 2026-09-14 12:54:33
- **Parametros**: ninguno
- **Tablas utilizadas**: DESCARGOSBLOQUEADOS

### dbo.CARGA_ENCABEZADOS

- **Descripcion**: INSERTA IMPORTACIONES.
- **Creado**: 2026-09-14 12:54:33
- **Parametros**: ninguno
- **Tablas utilizadas**: IMPORTACIONES, I_PEDIMENTO, SALIDAS, I_DETALLENP, NP, UNIDAD

### dbo.CARGA_ESTRUCTURADESENSAMBLE

- **Descripcion**: Proceso de carga y validacion de datos desde tablas stage.
- **Creado**: 2026-09-14 12:54:33
- **Parametros**: ninguno
- **Tablas utilizadas**: ESTDESENSAMBLE, MATERIALDESENSAMBLE

### dbo.CARGA_FACTURAS

- **Descripcion**: CREA LOS PRODUCTOS QUE FALTEN
- **Creado**: 2026-09-14 12:54:33
- **Parametros**: ninguno
- **Tablas utilizadas**: productos, TFACTURA, TERRORFACTURA, clientes, FACTURA

### dbo.CARGA_MATERIALES

- **Descripcion**: PRIMERO REVISO LAS UNIDADES DE MEDIDA
- **Creado**: 2026-09-14 12:54:33
- **Parametros**: ninguno
- **Tablas utilizadas**: CARGAMATERIAL, ECARGAMATERIAL, FACTORESMP, MATERIAL

### dbo.CARGA_PRODUCTOS

- **Descripcion**: PRIMERO REVISO LAS UNIDADES DE MEDIDA
- **Creado**: 2026-09-14 12:54:33
- **Parametros**: ninguno
- **Tablas utilizadas**: ECARGAPRODUCTO, tmpproductos

### dbo.CARGA_SUBMAQUILA

- **Descripcion**: Proceso de carga y validacion de datos desde tablas stage.
- **Creado**: 2026-09-14 12:54:33
- **Parametros**: ninguno
- **Tablas utilizadas**: -

### dbo.CARGAACTAS

- **Descripcion**: DELETE FROM ERRORACTA
- **Creado**: 2026-09-14 12:54:33
- **Parametros**: ninguno
- **Tablas utilizadas**: ACTA

### dbo.CARGAAgentes

- **Descripcion**: Proceso de carga y validacion de datos desde tablas stage.
- **Creado**: 2026-09-14 12:54:33
- **Parametros**: ninguno
- **Tablas utilizadas**: ECARGAagentes, TMPagentes, agentes

### dbo.CARGACLIENTES

- **Descripcion**: Proceso de carga y validacion de datos desde tablas stage.
- **Creado**: 2026-09-14 12:54:33
- **Parametros**: ninguno
- **Tablas utilizadas**: ECARGACLIENTES, TMPCLIENTES, ECARGAPROVEEDORES, clientes

### dbo.CARGACONSTANCIAS

- **Descripcion**: BORRO LOS ERRORES
- **Creado**: 2026-09-14 12:54:33
- **Parametros**: ninguno
- **Tablas utilizadas**: ERRORCARGA, CONSTANCIATRANSF, productos, settings

### dbo.CARGAFACTURASENPSALIDAS

- **Descripcion**: PRIMERO SELECCIONA LA FACTURA DE LA TABLA DE FACTURAS.
- **Creado**: 2026-09-14 12:54:33
- **Parametros**: ninguno
- **Tablas utilizadas**: ERRCARGAFACTURA, CARGAFACTURA, PRODUCTOS

### dbo.CARGAPEDIMENTOS

- **Descripcion**: BORRO LOS ERRORES
- **Creado**: 2026-09-14 12:54:33
- **Parametros**: ninguno
- **Tablas utilizadas**: ERRORCARGA, CARGAPEDIMENTOSIE, MATERIAL, PRODUCTOS, IMPORTACIONES

### dbo.CARGAPROVEEDORES

- **Descripcion**: Proceso de carga y validacion de datos desde tablas stage.
- **Creado**: 2026-09-14 12:54:33
- **Parametros**: ninguno
- **Tablas utilizadas**: ECARGAPROVEEDORES, TMPPROVEEDORES, Proveedores

### dbo.COMPARADESCARGAA31

- **Descripcion**: Compulsa / conciliacion entre fuentes de pedimentos.
- **Creado**: 2026-09-14 12:54:33
- **Parametros**: ninguno
- **Tablas utilizadas**: A31_COMPARATIVADESCARGA

### dbo.COMPARATIVADESCARGA31

- **Descripcion**: Compulsa / conciliacion entre fuentes de pedimentos.
- **Creado**: 2026-09-14 12:54:33
- **Parametros**: ninguno
- **Tablas utilizadas**: descarga31, comparativa31, salidas, descarga, partidas, psalidas, DIFERENCIASA31

### dbo.COMPARATIVADESCARGA31_DETALLE

- **Descripcion**: Compulsa / conciliacion entre fuentes de pedimentos.
- **Creado**: 2026-09-14 12:54:33
- **Parametros**: @PERIODO VARCHAR(10); @ANIO VARCHAR(10)
- **Tablas utilizadas**: descarga31_DETALLE, comparativa31_DETALLE, salidas, descarga, partidas, psalidas, DIFERENCIASA31_DETALLE

### dbo.COMPULSAGLOSA

- **Descripcion**: GLOSA.PEDIMENTOARMADO = '2430098003953'
- **Creado**: 2026-09-14 12:54:33
- **Parametros**: ninguno
- **Tablas utilizadas**: -

### dbo.COMPULSAGLOSA2

- **Descripcion**: REVISA FRACCION
- **Creado**: 2026-09-14 12:54:33
- **Parametros**: ninguno
- **Tablas utilizadas**: -

### dbo.COMPULSAGLOSAFINAL

- **Descripcion**: VALIDA PARA CADA OPERACION SI ESTA CAPTURADA O NO EN EL SISTEMA
- **Creado**: 2026-09-14 12:54:33
- **Parametros**: ninguno
- **Tablas utilizadas**: ERRORGLOSA

### dbo.CONVIERTEDIRIGIDO

- **Descripcion**: SET NOCOUNT ON ADDED TO PREVENT EXTRA RESULT SETS FROM
- **Creado**: 2026-09-14 12:54:33
- **Parametros**: ninguno
- **Tablas utilizadas**: -

### dbo.CONVIERTEDIRIGIDOPED

- **Descripcion**: SET NOCOUNT ON ADDED TO PREVENT EXTRA RESULT SETS FROM
- **Creado**: 2026-09-14 12:54:33
- **Parametros**: ninguno
- **Tablas utilizadas**: -

### dbo.CREAESTRUCTURAS

- **Descripcion**: - DATOS DEL PRODUCTO
- **Creado**: 2026-09-14 12:54:33
- **Parametros**: ninguno
- **Tablas utilizadas**: cartademateriales, material, ESTRUCTURAS, PRODUCTOMATERIAL

### dbo.CREAPRODUCTOSCARGAFACTURA

- **Descripcion**: -
- **Creado**: 2026-09-14 12:54:33
- **Parametros**: ninguno
- **Tablas utilizadas**: PRODUCTOS, CARGAFACTURA

### dbo.DESCAGARA31

- **Descripcion**: Gestion de descargas de material (aplicacion de importaciones a salidas).
- **Creado**: 2026-09-14 12:54:33
- **Parametros**: ninguno
- **Tablas utilizadas**: A31_HISTORIADESCARGAS

### dbo.DESCARGAINICIAL

- **Descripcion**: Gestion de descargas de material (aplicacion de importaciones a salidas).
- **Creado**: 2026-09-14 12:54:33
- **Parametros**: ninguno
- **Tablas utilizadas**: -

### dbo.DESCARGAS_A31

- **Descripcion**: Gestion de descargas de material (aplicacion de importaciones a salidas).
- **Creado**: 2026-09-14 12:54:33
- **Parametros**: ninguno
- **Tablas utilizadas**: -

### dbo.DESCARGASALIDAPEPS

- **Descripcion**: DECLARACION DE VARIABLES EN EL SISTEMA
- **Creado**: 2026-09-14 12:54:33
- **Parametros**: @SALIDAKEY INTEGER
- **Tablas utilizadas**: -

### dbo.DESCARGASCTM

- **Descripcion**: Gestion de descargas de material (aplicacion de importaciones a salidas).
- **Creado**: 2026-09-14 12:54:33
- **Parametros**: ninguno
- **Tablas utilizadas**: -

### dbo.DESCARGASCTMF

- **Descripcion**: Gestion de descargas de material (aplicacion de importaciones a salidas).
- **Creado**: 2026-09-14 12:54:33
- **Parametros**: ninguno
- **Tablas utilizadas**: CTMDESCARGA

### dbo.DESCARGATODOSDIRIGIDOS

- **Descripcion**: BORRA LOS NO BLOQUEADOS
- **Creado**: 2026-09-14 12:54:33
- **Parametros**: ninguno
- **Tablas utilizadas**: -

### dbo.DESCARGATSALIDA

- **Descripcion**: Gestion de descargas de material (aplicacion de importaciones a salidas).
- **Creado**: 2026-09-14 12:54:33
- **Parametros**: ninguno
- **Tablas utilizadas**: -

### dbo.DESCARGATSALIDA1

- **Descripcion**: BORRO EL TRAZO
- **Creado**: 2026-09-14 12:54:33
- **Parametros**: ninguno
- **Tablas utilizadas**: SETTINGS

### dbo.DESCARGATSALIDAFECHA

- **Descripcion**: BORRO EL TRAZO
- **Creado**: 2026-09-14 12:54:33
- **Parametros**: @HASTA DATETIME
- **Tablas utilizadas**: SETTINGS

### dbo.DESCARGAXFECHA51

- **Descripcion**: actualizo el tipo descarga
- **Creado**: 2026-09-14 12:54:33
- **Parametros**: @hasta datetime
- **Tablas utilizadas**: alternativo, SETTINGS, descargaRespaldo, MensajeDescarga, estructurasML

### dbo.DESCDIRIGIDA

- **Descripcion**: PRIMERO CREO EL CURSOR QUE HACE LAS OPERACIONES DIRIGIDAS.
- **Creado**: 2026-09-14 12:54:33
- **Parametros**: @SALIDAKEY BIGINT; @PSALIDAKEY BIGINT
- **Tablas utilizadas**: -

### dbo.ESTRUCTURAS1A1

- **Descripcion**: -
- **Creado**: 2026-09-14 12:54:33
- **Parametros**: ninguno
- **Tablas utilizadas**: cartademateriales

### dbo.GUARDADESCARGACTMA

- **Descripcion**: UPDATE PARTIDAS SET SALDO = (SELECT SUM(CANTUTIL) FROM DESCARGA D WHERE D.PENTRADALINK = PARTIDAKEY
- **Creado**: 2026-09-14 12:54:33
- **Parametros**: @PSALIDAKEY BIGINT
- **Tablas utilizadas**: descargaCTMBACK, descarga

### dbo.HISTORIADESCARGAS

- **Descripcion**: - INSERTA LAS DESCARGAS
- **Creado**: 2026-09-14 12:54:33
- **Parametros**: ninguno
- **Tablas utilizadas**: -

### dbo.HISTORIADESCARGASF

- **Descripcion**: EXEC [dbo].[LIGADESPERDICIOS]
- **Creado**: 2026-09-14 12:54:33
- **Parametros**: ninguno
- **Tablas utilizadas**: DescargaDesp, DescDirDesperdicios

### dbo.HISTORIADESCARGASP

- **Descripcion**: EXEC [dbo].[LIGADESPERDICIOS]
- **Creado**: 2026-09-14 12:54:33
- **Parametros**: @DOCUMENTO VARCHAR(25)
- **Tablas utilizadas**: DescargaDesp, DescDirDesperdicios

### dbo.HOJATRABAJOCTMDESPA31

- **Descripcion**: -
- **Creado**: 2026-09-14 12:54:33
- **Parametros**: @ANIO VARCHAR(4); @PERIODO VARCHAR(10)
- **Tablas utilizadas**: -

### dbo.HOJATRABAJODESCARGOSA31

- **Descripcion**: -
- **Creado**: 2026-09-14 12:54:33
- **Parametros**: @ANIO VARCHAR(4); @PERIODO VARCHAR(10); @CLAVE VARCHAR(5)
- **Tablas utilizadas**: -

### dbo.INDICAOPERACION

- **Descripcion**: VERIFICO SI EL PEDIMENTO ES UNA RECTIFICACION.
- **Creado**: 2026-09-14 12:54:33
- **Parametros**: ninguno
- **Tablas utilizadas**: -

### dbo.INFORME_CONCENTRADOSALDOS

- **Descripcion**: -
- **Creado**: 2026-09-14 12:54:33
- **Parametros**: @DESDE DATE; @HASTA DATE
- **Tablas utilizadas**: CONCENTRADOSALDOS

### dbo.INSERTADIRIGIDOS

- **Descripcion**: ,dbo.salidas.salidakey
- **Creado**: 2026-09-14 12:54:33
- **Parametros**: ninguno
- **Tablas utilizadas**: dirigido, cdescargadirigida

### dbo.INSERTAFALTANTESA31

- **Descripcion**: ACTUALIZO LAS PARTIDAS QUE SE DESCARGARON CON ERROR PARA QUE AL PONER COMPLETO NO SE CONSIDEREN
- **Creado**: 2026-09-14 12:54:33
- **Parametros**: ninguno
- **Tablas utilizadas**: A31_ENTRADAS
- **Referencias cross-DB**: DATA_STAGE_CALE

### dbo.INSERTAPEDIMENTO

- **Descripcion**: VARIABLES DE ENCABEZADO DE SALIDA
- **Creado**: 2026-09-14 12:54:33
- **Parametros**: @ITEM CHAR(30)
- **Tablas utilizadas**: ERRORVALIDACION, PEDIMENTOS, RECTIFICACIONEXPORT, SALIDAS, INVENTARIO, PSALIDAS, RECTIFICACIONIMPORT, IMPORTACIONES, PARTIDAS

### dbo.INSERTERROR

- **Descripcion**: PROCEDURE BODY
- **Creado**: 2026-09-14 12:54:33
- **Parametros**: @ERROR VARCHAR(300); @PEDIMENTO VARCHAR(30); @TIPO VARCHAR(50); @PARTIDA BIGINT
- **Tablas utilizadas**: -

### dbo.INSTERTAFACTURASFC

- **Descripcion**: -
- **Creado**: 2026-09-14 12:54:33
- **Parametros**: ninguno
- **Tablas utilizadas**: TMPFCERROR, FacturasCreadas, TmpFC

### dbo.LIGACTMA

- **Descripcion**: -------------------------------------------------
- **Creado**: 2026-09-14 12:54:33
- **Parametros**: ninguno
- **Tablas utilizadas**: DESCARGA_CTMA, V_F4CTMA, DescDirctma

### dbo.LIGACTMFACTURA

- **Descripcion**: Vinculacion de facturas / desperdicios con descargas.
- **Creado**: 2026-09-14 12:54:33
- **Parametros**: ninguno
- **Tablas utilizadas**: FACTURASCTM

### dbo.LIGADESPERDICIOS

- **Descripcion**: ESTE EXISTS ES PARA LIMITAR EL PROCESO A AQUELLOS QUE SI TIENEN CAMBIOS DE REGIMEN
- **Creado**: 2026-09-14 12:54:33
- **Parametros**: ninguno
- **Tablas utilizadas**: DESCARGA_DESPERDICIO, V_F4DESP, DescDirDesperdicios

### dbo.LLENAHISTORIADESCARGOSF4CTMA

- **Descripcion**: -
- **Creado**: 2026-09-14 12:54:33
- **Parametros**: ninguno
- **Tablas utilizadas**: HISTORIAF4CTMA, DescDirctma

### dbo.NUEVOFOLIOB

- **Descripcion**: -
- **Creado**: 2026-09-14 12:54:33
- **Parametros**: ninguno
- **Tablas utilizadas**: -

### dbo.PHISTORIAR8

- **Descripcion**: ------- CURSOR DE IMPORTACIONES EN DONDE SE UTILIZO EL PERMISO
- **Creado**: 2026-09-14 12:54:33
- **Parametros**: ninguno
- **Tablas utilizadas**: HISTORIAR8

### dbo.PR_COMPULSACANTIDADES

- **Descripcion**: INSERTO LAS IMPORTACIONES DEL SACI
- **Creado**: 2026-09-14 12:54:33
- **Parametros**: @AÑO BIGINT; @MES BIGINT; @PEDIMENTO VARCHAR(50)
- **Tablas utilizadas**: COMPULSACANTIDADES
- **Referencias cross-DB**: CALE_PVU_FS

### dbo.PR_CompulsaDSA24

- **Descripcion**: PROCESO DE COMPULSA 2020
- **Creado**: 2026-09-14 12:54:33
- **Parametros**: @DESDE DATETIME; @HASTA DATETIME; @PEDIMENTOARMADO VARCHAR(50); @TIPO VARCHAR(10)
- **Tablas utilizadas**: CONRIBUCIONES_GENERALES_DS, CONRIBUCIONES_PARTIDA_DS

### dbo.PR_INFORME_ESTRUCTURAS

- **Descripcion**: DECLARE @temp TABLE
- **Creado**: 2026-09-14 12:54:33
- **Parametros**: @PRODUCTO VARCHAR(50); @MATERIAL VARCHAR(50); @DESDE DATETIME; @HASTA DATETIME
- **Tablas utilizadas**: salidas, psalidas, material, productomaterial, estructuras, productos

### dbo.PR_INFORME_EXPORTACIONES

- **Descripcion**: CREATE PROCEDURE [dbo].[PR_INFORME_EXPORTACIONES]
- **Creado**: 2026-09-14 12:54:33
- **Parametros**: @DESDE DATETIME; @HASTA DATETIME; @documento VARCHAR(50)
- **Tablas utilizadas**: psalidas, salidas, clientes

### dbo.PR_INFORME_IMPORTACIONES

- **Descripcion**: CREATE PROCEDURE [dbo].[PR_INFORME_IMPORTACIONES]
- **Creado**: 2026-09-14 12:54:33
- **Parametros**: @DESDE DATETIME; @HASTA DATETIME; @documento VARCHAR(50)
- **Tablas utilizadas**: Importaciones, partidas, Proveedores

### dbo.PR_INFORME_SALDOS

- **Descripcion**: CREATE PROCEDURE [dbo].[PR_INFORME_SALDOS]
- **Creado**: 2026-09-14 12:54:33
- **Parametros**: @DESDE DATETIME; @HASTA DATETIME; @documento VARCHAR(50)
- **Tablas utilizadas**: Importaciones, partidas, categorias

### dbo.PROC_DESCARGASF4CTMA

- **Descripcion**: -
- **Creado**: 2026-09-14 12:54:33
- **Parametros**: @DESDE DATETIME; @HASTA DATETIME; @DOCUMENTO VARCHAR(20)
- **Tablas utilizadas**: DESCARGASF4CTMA

### dbo.PROC_HISTORIADESCARGASALIDA

- **Descripcion**: AND SALIDAS.CVE_PEDIMENTO='CT'
- **Creado**: 2026-09-14 12:54:33
- **Parametros**: @DESDE DATETIME; @HASTA DATETIME; @PROD VARCHAR(50); @CLAVE VARCHAR(10); @DOCUMENTO VARCHAR(20)
- **Tablas utilizadas**: HISTORIADESCARGASALIDA

### dbo.PROC_HISTORIADESCARGASALIDA;1

- **Descripcion**: -
- **Creado**: 2026-09-14 12:54:33
- **Parametros**: @DESDE DATETIME; @HASTA DATETIME; @PROD VARCHAR(50); @CLAVE VARCHAR(10); @DOCUMENTO VARCHAR(20)
- **Tablas utilizadas**: HISTORIADESCARGASALIDA

### dbo.PROC_HISTORIADESCARGASALIDAFALTANTES

- **Descripcion**: -
- **Creado**: 2026-09-14 12:54:33
- **Parametros**: @DESDE DATETIME; @HASTA DATETIME; @PROD VARCHAR(50); @CLAVE VARCHAR(10); @DOCUMENTO VARCHAR(20)
- **Tablas utilizadas**: HISTORIADESCARGASALIDA

### dbo.RETORNO_SUBMAQUINA

- **Descripcion**: -
- **Creado**: 2026-09-14 12:54:33
- **Parametros**: @PSKEY BIGINT; @CANTIDAD NUMERIC(18, 4)
- **Tablas utilizadas**: partidas

### dbo.SALDOS

- **Descripcion**: DATOS PARA LA DESCARGA
- **Creado**: 2026-09-14 12:54:33
- **Parametros**: @ITEM VARCHAR(50); @TOTINCORPORADO NUMERIC(18, 6); @TOTDESPERDICIADO NUMERIC(18, 6); @TOTMERMADO NUMERIC(18, 6); @SALIDALINK BIGINT; @PSALIDALINK BIGINT; @TIPO CHAR(10); @FECHAEXPORT DATETIME; @LINEA BIGINT; @PRODMATKEY BIGINT; @faltodescarga NUMERIC(18, 6)
- **Tablas utilizadas**: productomaterial

### dbo.SALDOS_FAMILIA

- **Descripcion**: DATOS PARA LA DESCARGA
- **Creado**: 2026-09-14 12:54:33
- **Parametros**: @ITEM VARCHAR(50); @TOTINCORPORADO NUMERIC(18, 6); @TOTDESPERDICIADO NUMERIC(18, 6); @TOTMERMADO NUMERIC(18, 6); @SALIDALINK BIGINT; @PSALIDALINK BIGINT; @TIPO CHAR(10); @FECHAEXPORT DATETIME; @LINEA BIGINT; @PRODMATKEY BIGINT; @faltodescarga NUMERIC(18, 6)
- **Tablas utilizadas**: productomaterial

### dbo.SALDOS2

- **Descripcion**: DATOS PARA LA DESCARGA
- **Creado**: 2026-09-14 12:54:33
- **Parametros**: @ITEM VARCHAR(50); @TOTINCORPORADO FLOAT; @TOTDESPERDICIADO FLOAT; @TOTMERMADO FLOAT; @SALIDALINK FLOAT; @PSALIDALINK FLOAT; @TIPO CHAR(10); @FECHAEXPORT DATETIME
- **Tablas utilizadas**: GENERADORES

### dbo.SALDOSCTM

- **Descripcion**: Calculo de saldos de materiales o partidas.
- **Creado**: 2026-09-14 12:54:33
- **Parametros**: @FECHA DATETIME; @CLAVE VARCHAR(50); @CANTIDAD NUMERIC(18,4); @CTMKEY BIGINT; @PSKEY BIGINT; @CLIENTE VARCHAR(50)
- **Tablas utilizadas**: DESCARGACTMF, TRAZOCTM

### dbo.SALDOSDIRIGIDOS

- **Descripcion**: DATOS PARA LA DESCARGA
- **Creado**: 2026-09-14 12:54:33
- **Parametros**: @ITEM VARCHAR(50); @TOTINCORPORADO FLOAT; @TOTDESPERDICIADO FLOAT; @TOTMERMADO FLOAT; @SALIDALINK FLOAT; @PSALIDALINK FLOAT; @PEDIMENTO VARCHAR(30); @FACTURAI VARCHAR(50)
- **Tablas utilizadas**: -

### dbo.SP_DESENSAMBLE

- **Descripcion**: -
- **Creado**: 2026-09-14 12:54:33
- **Parametros**: @MATERIAL VARCHAR(50); @PKEY BIGINT
- **Tablas utilizadas**: partidas

### dbo.SP_DirigidoBOrrados

- **Descripcion**: -
- **Creado**: 2026-09-14 12:54:33
- **Parametros**: @dirigidokey bigint; @usuario varchar(30)
- **Tablas utilizadas**: dirigidoBorrados

### dbo.SP_G5

- **Descripcion**: declare @res as varchar(MAX)
- **Creado**: 2026-09-14 12:54:34
- **Parametros**: ninguno
- **Tablas utilizadas**: G5, v_g5

### dbo.SP_G6

- **Descripcion**: VARIABLES DEL CURSOR                                --VARIABLES DE DATOS
- **Creado**: 2026-09-14 12:54:34
- **Parametros**: @ANIO VARCHAR(4); @PERIODO VARCHAR(10); @SUSTITUYE VARCHAR(60); @clave_destino VARCHAR(20)
- **Tablas utilizadas**: SalidasA31Key, V_G6, Destinatario, G6

### dbo.SP_G6_F4

- **Descripcion**: -
- **Creado**: 2026-09-14 12:54:34
- **Parametros**: @ANIO VARCHAR(4); @PERIODO VARCHAR(10); @SUSTITUYE VARCHAR(20); @clave_destino VARCHAR(20)
- **Tablas utilizadas**: G6

### dbo.SP_GENERA_TXT_COMPLETO

- **Descripcion**: ----------------------------
- **Creado**: 2026-09-14 12:54:34
- **Parametros**: ninguno
- **Tablas utilizadas**: -

### dbo.SP_ImportacionesBorradas

- **Descripcion**: -
- **Creado**: 2026-09-14 12:54:34
- **Parametros**: @usuario varchar(50); @Ipedimentokey bigint
- **Tablas utilizadas**: ImportacionesBorradas, PartidasBorradas

### dbo.SP_MensajesInicio

- **Descripcion**: Se insertan los materiales
- **Creado**: 2026-09-14 12:54:34
- **Parametros**: ninguno
- **Tablas utilizadas**: MensajesInicio

### dbo.SP_PartidasBorradas

- **Descripcion**: -
- **Creado**: 2026-09-14 12:54:34
- **Parametros**: @Usuario varchar(30); @partidakey bigint
- **Tablas utilizadas**: PartidasBorradas

### dbo.SP_PsalidasBorradas

- **Descripcion**: -
- **Creado**: 2026-09-14 12:54:34
- **Parametros**: @usuario varchar(30); @psalidakey bigint
- **Tablas utilizadas**: PsalidasBorradas

### dbo.SP_SalidasBorradas

- **Descripcion**: -
- **Creado**: 2026-09-14 12:54:34
- **Parametros**: @usuario varchar(30); @salidakey bigint
- **Tablas utilizadas**: SalidasBorradas, PsalidasBorradas

### dbo.Trazo_report

- **Descripcion**: SELECT @material = '0A4000900-00'
- **Creado**: 2026-09-14 12:54:34
- **Parametros**: ninguno
- **Tablas utilizadas**: -

### dbo.VALIDA_I_DETALLENP

- **Descripcion**: VERIFICO PARTIDA POR PARTIDA.
- **Creado**: 2026-09-14 12:54:34
- **Parametros**: ninguno
- **Tablas utilizadas**: I_ERROR, I_PEDIMENTO, I_DETALLENP, MATERIAL, PRODUCTOS, I_FACTURA, IMPORTACIONES, PARTIDAS, V_I_PEDIMENTOS

### dbo.VALIDACIONESVU

- **Descripcion**: INSERT INTO [dbo].[VALIDACIONVU]
- **Creado**: 2026-09-14 12:54:34
- **Parametros**: @DESDE DATETIME; @HASTA DATETIME; @CLAVE VARCHAR(10)
- **Tablas utilizadas**: VALIDACIONVU
- **Referencias cross-DB**: CALE_PVU_FS

### dbo.VALIDAF4

- **Descripcion**: Validacion de datos (pedimentos, descargas, detalle).
- **Creado**: 2026-09-14 12:54:34
- **Parametros**: @ANIO VARCHAR(4); @PERIODO VARCHAR(10); @CLAVE_DESTINO VARCHAR(20)
- **Tablas utilizadas**: -

### dbo.VALIDAPEDIMENTO

- **Descripcion**: PRIMERO VALIDO QUE LAS PARTIDAS EXISTAN
- **Creado**: 2026-09-14 12:54:34
- **Parametros**: @PEDIMENTO VARCHAR(30)
- **Tablas utilizadas**: -

