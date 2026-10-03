# Auditoría forense de Stored Procedures mutables V2

## Alcance y controles

Auditoría read-only realizada contra `CALE_IMMEX` y `ANEXO24_DEV` usando la
configuración local autorizada `backend/.env` e identidad SQL `opdatos`.

- `LIVE reads`: metadata `sys.*`, columnas y `OBJECT_DEFINITION`.
- `LIVE writes`: 0.
- `LIVE mutable SP executions`: 0.
- Definiciones completas analizadas: 34.
- No se copiaron definiciones completas al repositorio; el contrato siguiente se
  deriva del cuerpo SQL LIVE leído durante esta auditoría.
- No se ejecutaron scripts `04`–`07` ni hubo cambios de hardening.

## Reconciliación LIVE

| Base | SP funcionales APP24 LIVE | Resultado |
|---|---:|---|
| `CALE_IMMEX.dbo` | 24 | 23 `APP24_Q_*` + `APP24_C_PEDIMENTO_CONFIRMAR` |
| `ANEXO24_DEV.app24` | 35 | 20 `APP24_Q_*` + 15 `APP24_C_*` |
| Total | 59 | Repo/LIVE reconciliado |

Los dos objetos ausentes del snapshot anterior de 57 fueron:

- `dbo.APP24_C_PEDIMENTO_CONFIRMAR` — creado `2026-10-01`;
- `dbo.APP24_Q_F4_LISTAR` — creado `2026-10-02`.

`DESCARGATSALIDA2` y `CTMDESCARGA` no existen como procedimientos en LIVE. Las
referencias observadas son nombres llamados por otros objetos o tablas/procesos;
no se inventa un contrato para ellos.

## Método de clasificación

Cada candidate conserva dos ejes independientes:

- `BUSINESS_LOGIC`: qué semántica demuestra el cuerpo SQL.
- `INTEGRATION_MODE`: qué faltaría para exponerla sin SQL funcional inline en
  Java.

`MIXED` nunca se usó como sinónimo de `NOT_REUSABLE`.

| FEATURE | SP | BUSINESS_LOGIC | INTEGRATION_MODE | LEGACY_STAGE | TARGETS | TESTED_SYNTHETIC | CONTRACT | IMPLEMENTABLE_NOW | BLOCKER |
|---|---|---|---|---|---|---|---|---|---|
| Materiales confirmación | `CARGA_MATERIALES` | `BUSINESS_LOGIC_CONFIRMED` | `REQUIRES_SQL_WRAPPER` | `CargaMaterial`, `ECargaMaterial` globales | `MATERIAL`, `FACTORESMP` | NO | Validaciones, reemplazo por clave, factores y errores demostrados | NO | Propiedad/exclusión del stage global legacy no acordada |
| Productos confirmación | `CARGA_PRODUCTOS` | `BUSINESS_LOGIC_CONFIRMED` | `REQUIRES_SQL_WRAPPER` | `tmpproductos`, `ECargaProducto` globales | `PRODUCTOS` | NO | Validaciones e inserción sólo de claves inexistentes demostradas | NO | Misma exclusión de stage global |
| Clientes | `CARGACLIENTES` | `BUSINESS_LOGIC_CONFIRMED` | `REQUIRES_SQL_WRAPPER` | `TMPCLIENTES`, `ECARGACLIENTES` globales | `clientes` | NO | Inserta sólo claves nuevas; valida vacío, duplicado e ID fiscal | NO | Stage global; además escribe por error en `ECARGAPROVEEDORES` para ID fiscal |
| Proveedores | `CARGAPROVEEDORES` | `BUSINESS_LOGIC_CONFIRMED` | `REQUIRES_SQL_WRAPPER` | `TMPPROVEEDORES`, `ECARGAPROVEEDORES` globales | `Proveedores` | NO | Inserta sólo claves nuevas; valida vacío, duplicado e ID fiscal | NO | Stage global y contrato UI/layout pendiente |
| Agentes | `CARGAAgentes` | `BUSINESS_LOGIC_CONFIRMED` | `REQUIRES_SQL_WRAPPER` | `TMPagentes`, `ECargaagentes` globales | `agentes` | NO | Inserta claves nuevas; valida clave/patente/duplicados | NO | Stage global y contrato UI/layout pendiente |
| Facturación flujo A | `CARGA_FACTURAS` | `BUSINESS_LOGIC_CONFIRMED` | `REQUIRES_SQL_WRAPPER` | `TFACTURA`, `TERRORFACTURA` globales | productos, clientes, `FACTURA`, `SALIDAS`, `PSALIDAS` | NO | Flujo independiente completo; usa cursores y `MAX()+1` | NO | Selección de flujo e idempotencia/aceptación de negocio |
| Facturación flujo B | `CREAPRODUCTOSCARGAFACTURA` → `CARGAFACTURASENPSALIDAS` | `BUSINESS_LOGIC_CONFIRMED` | `REQUIRES_SQL_WRAPPER` | `CARGAFACTURA`, `ERRCARGAFACTURA` globales | `PRODUCTOS`, `PSALIDAS` | NO | Helper crea productos; command valida y agrega partidas a salida existente | NO | Flujo B no es equivalente a A; requiere pedimento/salida y decisión de negocio |
| Pedimentos legacy | `INSERTAPEDIMENTO`, `VALIDAPEDIMENTO`, `VALIDA_I_DETALLENP` | `BUSINESS_LOGIC_CONFIRMED` | `NOT_REUSABLE` para V1 | stages legacy compartidos | importaciones, partidas, salidas, psalidas, errores | NO | Algoritmo legacy demostrado | ALREADY_COVERED | `APP24_C_PEDIMENTO_CONFIRMAR` ya implementa confirmación controlada |
| Actas | `CARGAACTAS` | `BUSINESS_LOGIC_PARTIAL` | `REQUIRES_SQL_WRAPPER` | `Acta` global | `SALIDAS`, `PSALIDAS`, `DIRIGIDO` | NO | Grano `Folio`+`Linea`; create-if-absent demostrado | NO | Sin validaciones, errores activos, transacción ni layout aprobado |
| Submaquila | `CARGA_SUBMAQUILA` | `BUSINESS_LOGIC_PARTIAL` | `REQUIRES_SQL_WRAPPER` | `TMPSUBMAQUILA` global | `SALIDAS`, `PSALIDAS` | NO | Crea salidas/partidas de submaquila desde filas stage | NO | Sin validación, deduplicación, transacción ni reglas fiscales |
| Constancias | `CARGACONSTANCIAS` | `BUSINESS_LOGIC_PARTIAL` | `REQUIRES_SQL_WRAPPER` | `CONSTANCIATRANSF`, `ERRORCARGA` globales | productos, salidas, psalidas, dirigido | NO | Valida e inserta productos condicionado por `settings` | NO | Layout, side effects y settings autorizados pendientes |
| PEPS/descargos | `DESCARGASALIDAPEPS` → `DESCDIRIGIDA`/`SALDOS*` | `BUSINESS_LOGIC_CONFIRMED` | `REUSABLE_WITH_APP_ORCHESTRATION` | no stage de archivo | `DESCARGA`, `PARTIDAS`, `TRAZO`, `DIRIGIDO` | NO | Algoritmo está encapsulado en SP; borra/recalcula y explota estructuras | NO | Aprobación fiscal, dry-run, lock operativo, idempotencia y rollback |
| CTM | `LIGACTMA`, `LIGACTMFACTURA`, `SALDOSCTM` | `BUSINESS_LOGIC_PARTIAL` | `UNKNOWN` | tablas CTM | descargas/trazo CTM | NO | Calls y targets demostrados | NO | Falta contrato de entrada/salida y aceptación CTM |
| Ajuste/Anexo 30 | `DESCARGAS_A31` → `A31_SALDOS`; `SP_G5`, `SP_G6`, `SP_G6_F4` | `BUSINESS_LOGIC_PARTIAL` | `REUSABLE_WITH_APP_ORCHESTRATION` | tablas A31 | A31 saldos, descargas, trazo, G5/G6 | NO | Grafo de proceso y parámetros demostrados | NO | Regla regulatoria, periodo, output/errores y aceptación |
| Exportación A31 | `SP_GENERA_TXT_COMPLETO` | `BUSINESS_LOGIC_UNKNOWN` | `NOT_REUSABLE` | no aplica | muta catálogos y ejecuta `xp_cmdshell`/BCP | NO | Efecto host/path fijo demostrado | NO | Riesgo host, ruta fija, mutación previa y ejecución externa |

## Contratos principales

### `dbo.CARGA_MATERIALES`

**Propósito.** Reprocesa las filas globales de `CargaMaterial` y mantiene el
catálogo de materiales/factores.

**Entradas.** Sin parámetros. Lee 30 columnas de `CargaMaterial`; el staging
moderno actual usa exactamente las mismas columnas y orden contractual:
`ClaveMaterial`, unidades, fracción, 18 factores, división, tipo y datos de
activo fijo.

**Validaciones.** Normaliza `TIPOM`; fuerza fracción nacional; valida unidades
por `VALIDUNIT`, fracción de ocho, longitud de claves y duplicados dentro del
stage. Publica errores por `CargaMaterialKey` en `ECargaMaterial`.

**Efectos.** Para cada clave sin error borra `MATERIAL` previo, elimina factores
huérfanos, inserta material con `MAX(MATERIALKEY)+1` y genera factores positivos.
No tiene `TRY/CATCH`, transacción ni resultset contractual.

**Implicación.** Semántica de actualización por reemplazo confirmada. Puede
reutilizarse solamente detrás de wrapper SQL: cargar stage, ejecutar legacy,
snapshot de errores, limpiar stage y auditar. Java no debe insertar en stage.

**Bloqueador actual.** `CargaMaterial` y `ECargaMaterial` son globales. Un
`sp_getapplock` de la aplicación no coordina procesos Web Forms legacy que no lo
toman. Borrar/cargar esas tablas sin acuerdo operativo puede perder una carga
legacy concurrente. Requiere ventana exclusiva o mecanismo aprobado de
coordinación entre ambos sistemas.

### `dbo.CARGA_PRODUCTOS`

Valida unidad, fracción, clave, clave cliente, nombre y duplicados de
`tmpproductos`; registra `ECargaProducto`; inserta únicamente productos con clave
inexistente y sin error. También usa `MAX(PRODUCTOKEY)+1`, sin transacción ni
resultset. El staging moderno tiene las siete columnas requeridas. Su integración
queda bloqueada por la misma propiedad de stage global.

### Clientes, proveedores y agentes

Los tres son procesos de alta, no subflujos de facturación:

- Clientes/proveedores: validan clave vacía/duplicada e ID fiscal, insertan sólo
  claves nuevas con `MAX()+1`.
- Agentes: valida clave, patente obligatoria, patente de cuatro y duplicados;
  inserta sólo claves nuevas.

`CARGACLIENTES` escribe la validación de ID fiscal en `ECARGAPROVEEDORES`, una
anomalía que un adapter debe preservar/normalizar explícitamente. Ninguno hace
update, transacción o aislamiento.

### Facturación

LIVE demuestra dos flujos distintos:

```text
Flujo A: TFACTURA → CARGA_FACTURAS
         → productos/clientes/FACTURA/SALIDAS/PSALIDAS

Flujo B: CARGAFACTURA → CREAPRODUCTOSCARGAFACTURA(@FACTURA)
         → CARGAFACTURASENPSALIDAS(@PEDIMENTO, @FACTURA)
         → PSALIDAS de una salida/pedimento existente
```

No son fases intercambiables. Ambos usan stages globales, `MAX()+1`, cursores y
no tienen transacción. La aplicación no puede elegir flujo A/B sin decisión de
negocio y caso de aceptación.

### PEPS

`PEPS_ALGORITHM_IN_SP = YES`.

`DESCARGASALIDAPEPS(@SALIDAKEY)` borra trazo/descargas previas de la salida,
recalcula saldos, recorre líneas de salida y estructura activa; para dirigidos
llama `DESCDIRIGIDA`, que a su vez llama `SALDOSDIRIGIDOS`. El motor no es
desconocido; queda bloqueado por aprobación fiscal/operativa para reutilizar ese
algoritmo con simulación, locks, permiso y rollback verificable.

### A31 y ajuste

`DESCARGAS_A31` trunca `A31_TRAZO` y `A31_DESCARGAS`, reinicializa saldos y llama
`A31_SALDOS` por cursor. `SP_G6` llama `SP_G6_F4` y `VALIDACIONESVU`; `SP_G5`
llama `LIGADESPERDICIOS`. Existe lógica reutilizable, pero no contrato regulatorio
ni superficie de entrada/salida aprobada.

`SP_GENERA_TXT_COMPLETO` queda excluido: limpia datos de catálogos y operación,
construye BCP hacia ruta fija y ejecuta `xp_cmdshell`.

## Resultado V2

| Integration mode | Candidatos |
|---|---|
| `DIRECTLY_REUSABLE` | 0 nuevos |
| `REUSABLE_WITH_APP_ORCHESTRATION` | PEPS; A31/ajuste después de aprobación |
| `REUSABLE_WITH_LEGACY_STAGE_ADAPTER` | Lógica de materiales, productos, clientes, proveedores y agentes demostrada |
| `REQUIRES_SQL_WRAPPER` | Confirmaciones que deben poblar stage legacy sin Java SQL |
| `NOT_REUSABLE` | `SP_GENERA_TXT_COMPLETO`; pedimentos legacy para V1 ya cubierto |
| `UNKNOWN` | CTM y contratos especiales restantes |

```text
MATERIALES_CONFIRM_IMPLEMENTABLE = IMPLEMENTABLE_AFTER_APPROVAL
PRODUCTOS_CONFIRM_IMPLEMENTABLE  = IMPLEMENTABLE_AFTER_APPROVAL
PEPS_ALGORITHM_IN_SP             = YES
NEW_BUSINESS_SP_REQUIRED          = NO
```

La aprobación requerida para materiales/productos no es una preferencia de
arquitectura: debe autorizar quién posee el stage global durante una confirmación,
cómo se bloquea el legacy y cómo se preserva/restaura una carga ajena. Sin ese
contrato operativo, implementar endpoint sería inseguro aunque el algoritmo sea
reutilizable.
