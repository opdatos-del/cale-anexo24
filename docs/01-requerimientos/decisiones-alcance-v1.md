# Decisiones pendientes V1

## Propósito y regla de lectura

Este documento prepara las decisiones funcionales que requieren respuesta de la
empresa o del asesor. No sustituye una aprobación de negocio ni convierte una
hipótesis técnica en requisito.

Para cada tema se separan:

- **WHAT WE KNOW:** evidencia documentada o comportamiento confirmado.
- **WHAT WE OBSERVED:** pantallas, reportes, objetos o flujos observados.
- **WHAT IS IMPLEMENTED:** comportamiento actualmente entregado en la nueva aplicación.
- **WHAT IS UNKNOWN:** información que no se puede cerrar con la evidencia disponible.
- **DECISION NEEDED:** respuesta concreta requerida para congelar alcance.

Estados usados:

- `PENDING_BUSINESS`: falta una decisión funcional autorizada.
- `ANSWERED`: la empresa respondió y la evidencia fue registrada.
- `READY_FOR_IMPLEMENTATION`: existe contrato funcional y aceptación suficiente.
- `OUT_OF_SCOPE_V1`: se decidió excluirlo de V1.

Los estados iniciales de los tres temas son `PENDING_BUSINESS`.

---

## 1. Saldos

### WHAT WE KNOW

- El modelo legacy conserva `dbo.PARTIDAS.Saldo` como un valor persistido por
  partida de importación.
- Los procesos de descargo/saldo relacionan `PARTIDAS`, `DESCARGA` y `TRAZO`,
  y pueden actualizar el saldo persistido.
- Se identificaron fuentes de consulta read-only como `PR_INFORME_SALDOS`,
  `v_saldos` y `v_saldosdesp`.
- La familia `SALDOS*`, los descargos y los concentrados son procesos mutables;
  no deben ejecutarse desde una consulta GET.
- El dominio distingue entradas, salidas, consumo/descarga, retornos,
  desperdicio, CTM, dirigido, F4 y A31. No se demostró una fórmula universal
  que cubra todos esos subtipos.

### WHAT WE OBSERVED

- La aplicación legacy mostraba un reporte de saldos que requería rango de
  fechas, pero la generación falló con un error genérico durante la auditoría
  funcional.
- La evidencia técnica documenta `PR_INFORME_SALDOS` con 37 columnas y
  parámetros de fecha/documento, pero no un contrato HTTP, orden, total o
  paginación aprobados.
- La documentación de requerimientos propone como posible proyección parte o
  material, unidad, saldo inicial, entradas, consumo, salidas/retornos, saldo
  final y fecha de corte. Esa proyección sigue siendo propuesta, no decisión.
- Los snapshots read-only anteriores no son una medición actual general de
  `PARTIDAS`; la revalidación vigente depende de acceso read-only confiable.

### WHAT IS IMPLEMENTED

- No existe todavía un contrato HTTP aprobado para Saldos.
- No se fijaron endpoint, permiso, filtros, orden, paginación ni response V1.
- No se ejecutan procesos mutables de saldo o descargo desde una consulta.
- El historial de materiales utilizados cubre la consulta de descargas, pero no
  equivale por sí mismo a un reporte fiscal de saldos.

### WHAT IS UNKNOWN

- Qué significa exactamente "Saldo" para la operación y para el cumplimiento
  fiscal.
- La granularidad de una fila: partida, material, producto, pedimento +
  material, fracción, lote, estructura u otra.
- La fórmula y fuentes autoritativas para saldo inicial, entradas, consumo,
  salidas, retornos, desperdicios, ajustes y saldo final.
- Si existe saldo inicial y cómo se determina.
- La fecha de corte: fecha de importación, pago, entrada, ejecución del
  descargo, fecha de consulta u otra.
- El tratamiento de cancelaciones, correcciones, retornos, ajustes, mermas,
  desperdicios, activos fijos, CTM, dirigido, F4 y A31.
- Las unidades, conversiones, precisión y redondeo aplicables.
- Qué reporte o procedimiento debe considerarse fuente oficial y cómo se
  reconcilia una muestra contra esa fuente.

### DECISION NEEDED

1. ¿Qué representa exactamente "Saldo" para V1?
2. ¿Cuál es la granularidad oficial de una fila?
3. ¿Qué constituye una entrada, una salida, un consumo/descargo y un retorno?
4. ¿Existe saldo inicial? Si existe, ¿de qué fuente y desde qué fecha?
5. ¿Cuál es la fecha de corte y se requiere histórico reproducible a una fecha?
6. ¿Cómo se tratan ajustes, cancelaciones, retornos, mermas, desperdicios y
   activos fijos?
7. ¿Qué unidades y reglas de conversión/redondeo se deben aplicar?
8. ¿Qué reporte, vista o procedimiento es la fuente oficial autorizada?
9. ¿Qué filtros y columnas son obligatorios para la primera versión?
10. ¿Quién aprueba el resultado y qué diferencia máxima se acepta en una
    conciliación?

### Criterio de desbloqueo y aceptación

`SALDOS` sólo puede pasar de `PENDING_BUSINESS` a
`READY_FOR_IMPLEMENTATION` cuando estén aprobados:

- fórmula y fuentes;
- granularidad e identidad de la fila;
- fecha/corte y semántica histórica;
- tratamiento de ajustes y subtipos;
- unidades, precisión y redondeo;
- fuente oficial y permiso de consulta;
- al menos dos o tres casos manuales anonimizados con resultado esperado que
  negocio pueda verificar.

La aceptación debe comparar esos casos contra la fuente autorizada sin ejecutar
procesos mutables de saldo o descargo durante una consulta.

### Respuesta de negocio

`PENDING_BUSINESS` — completar en la sesión de definición funcional.

### Estado

`SALDOS = PENDING_BUSINESS`

---

## 2. Confirmación de Facturación

### WHAT WE KNOW

- La pantalla legacy observada permitía seleccionar archivos `.xls`/`.xlsx` y
  mostraba acciones `CARGAR`, `GUARDAR`, `LIMPIAR` y `DESCARGAR`.
- El layout validado de `Layout_Facturas.xlsx` tiene la hoja `FACTURAS` y 13
  columnas: Documento, Fecha, Almacen, Observaciones, Descarga, Tipo, Linea,
  Clave, Lote, Cantidad, Unidad, Dirigido y Cliente.
- La nueva aplicación valida estructura, tipos, obligatoriedad y errores
  localizables, y conserva filas normalizadas en staging durable `app24`.
- Se registra un fingerprint SHA-256 y existe protección técnica contra la
  repetición del mismo hash; la política de negocio para reintentos y archivos
  corregidos todavía debe aprobarse.
- Se identificaron al menos tres procesos legacy relacionados:
  `CARGA_FACTURAS`, `CARGAFACTURASENPSALIDAS` y
  `CREAPRODUCTOSCARGAFACTURA`.
- `CARGA_FACTURAS` se asocia con `TFACTURA`, errores, productos, clientes,
  facturas y referencias a salidas/detalles.
- `CARGAFACTURASENPSALIDAS(@PEDIMENTO, @FACTURA)` se asocia con
  `CARGAFACTURA`, busca una salida por documento y puede insertar líneas en
  `PSALIDAS`.
- `CREAPRODUCTOSCARGAFACTURA` puede crear productos faltantes durante una carga.

### WHAT WE OBSERVED

- La pantalla legacy rechazó una plantilla sintética por columnas o información;
  no se obtuvo con eso la plantilla oficial.
- La auditoría estática encontró que los flujos A y B tienen staging, destinos,
  granularidad y tablas de error diferentes; no son sinónimos.
- El flujo B requiere campos monetarios, fracción y país que no forman parte del
  layout de 13 columnas validado. Su compatibilidad con el layout actual no está
  probada.
- El flujo A no tiene correspondencia 1:1 demostrada con el layout actual.
- El diseño actual de la nueva aplicación cubre carga, validación, preview,
  errores estructurados, hash y staging; no ejecuta confirmación hacia
  `CALE_IMMEX` ni procedimientos legacy mutables.
- La documentación de implementación registra que no hay botón ni endpoint
  funcional de confirmación y que la plantilla oficial activa aún requiere
  validación funcional.

### WHAT IS IMPLEMENTED

- Recepción de `.xls`/`.xlsx` con límites técnicos documentados.
- Parseo de la hoja `FACTURAS` y validación del layout de 13 columnas.
- Preview y errores por archivo, hoja, fila, columna, regla y valor
  enmascarado.
- Persistencia durable de la carga y sus filas normalizadas en `app24`, con
  hash/fingerprint y errores asociados.
- No se guardan datos operativos en `CALE_IMMEX` como consecuencia de la carga
  actual; la fase entregada termina en validación y staging.
- No existe todavía confirmación que modifique inventario, salidas, productos,
  clientes, facturas o líneas de salida.

### WHAT IS UNKNOWN

- Qué botón o paso posterior a la previsualización es la acción autoritativa de
  confirmación.
- Qué archivo, staging y procedimiento exactos deben participar.
- Si la confirmación debe ejecutar el flujo A, B, C, CTM, un adaptador nuevo o
  excluir algunos flujos.
- Qué tablas se deben modificar y con qué granularidad header/detalle.
- Reglas para productos o clientes faltantes, salidas, `PSALIDAS`, descargos y
  efecto final en inventario.
- Idempotencia funcional: qué constituye un duplicado y cuándo se permite
  reintentar un archivo corregido.
- Transacción, rollback, aislamiento por lote/usuario, concurrencia y
  comportamiento ante error parcial.
- Estados oficiales y transición entre validada, confirmada, fallida o
  rechazada.
- Plantilla oficial, catálogos autorizados, límites de negocio y responsable
  que aprueba el resultado.

### DECISION NEEDED

La pregunta principal para la empresa es:

> Después de que una factura pasa la previsualización, ¿qué proceso exacto debe
> modificar el inventario o la operación?

La respuesta debe identificar, sin decir solamente "hacer lo mismo que el
sistema anterior":

1. archivo y formato autorizados;
2. paso o botón que confirma;
3. procedimiento o servicio autoritativo;
4. tablas y registros que aparecen o cambian;
5. reglas para productos, clientes, salidas, líneas y descargos;
6. pantalla o reporte donde se verifica el resultado;
7. comportamiento esperado ante duplicado, reintento y error;
8. rollback y evidencia de bitácora.

### Criterio de desbloqueo y aceptación

`FACTURACION_CONFIRM` sólo puede pasar de `PENDING_BUSINESS` a
`READY_FOR_IMPLEMENTATION` cuando estén aprobados:

- pipeline autoritativo y alcance de sus efectos;
- reglas de validación y plantilla oficial;
- granularidad de las operaciones resultantes;
- idempotencia, duplicados y reintentos;
- transacción y rollback;
- permisos y bitácora;
- un caso de aceptación autorizado con un archivo anonimizado de una a tres
  filas y resultado esperado conocido.

El caso debe poder reproducirse así: cargar archivo, revisar preview, confirmar,
consultar los registros resultantes en la pantalla o reporte acordado y
verificar que una repetición o fallo tenga el comportamiento aprobado. No se
usarán datos sensibles ni se ejecutarán procesos legacy sin autorización
explícita.

### Respuesta de negocio

`PENDING_BUSINESS` — la carga/validación no equivale a confirmación operativa.

### Estado

`FACTURACION_CONFIRM = PENDING_BUSINESS`

---

## 3. Dashboard V1

### WHAT WE KNOW

- El dashboard es la pantalla inicial posterior a autenticación.
- El diseño define saludo, perfil activo, accesos rápidos filtrados por permiso,
  avisos y estado del sistema.
- El frontend actual tiene una feature Dashboard y una ruta autenticada.
- La app actual muestra accesos a Materiales, Productos, Estructuras, Entradas,
  Salidas, Materiales utilizados y Activos fijos cuando el usuario tiene el
  permiso correspondiente.
- La pantalla muestra saludo, sesión activa, estado de catálogo conectado y un
  bloque de avisos operativos.
- El único indicador dinámico respaldado actualmente por un endpoint existente
  es el total de registros de Materiales, visible cuando el usuario tiene
  `MATERIALES_CONSULTAR`.

### WHAT WE OBSERVED

- El prototipo propone accesos rápidos y avisos como objetivo principal, no un
  tablero financiero o fiscal.
- El dashboard no muestra por defecto saldos ni cantidades de negocio sensibles.
- La arquitectura frontend documenta autenticación, shell, dashboard y
  catálogo de materiales como las funcionalidades actualmente migradas; no
  prueba que todas las rutas mostradas en el diseño tengan el mismo nivel de
  implementación funcional.

### WHAT IS IMPLEMENTED

- Pantalla dashboard autenticada con saludo y mensaje de sesión activa.
- Tarjetas/enlaces condicionados por permisos para los módulos disponibles.
- Consulta del total de materiales para la tarjeta de Materiales.
- Estado de carga y error con opción de reintento para ese resumen.
- Aviso informativo cuando no hay módulos asignados.
- Sección de avisos y sección de estado visual.

### WHAT IS UNKNOWN

- Si el dashboard actual es suficiente para V1 desde la perspectiva de negocio.
- Qué indicadores, periodo, filtros y permisos serían obligatorios si se requiere
  ampliar el tablero.
- Si los avisos deben ser estáticos, calculados desde eventos, o provenir de un
  servicio operativo.
- Si se necesitan métricas de materiales, productos, entradas, salidas,
  materiales utilizados, facturación, errores de carga o actividad reciente.
- Qué definición, frecuencia de actualización y fuente tendría cada indicador.

### DECISION NEEDED

1. ¿El dashboard actual —saludo, accesos autorizados, avisos, estado y total
   de materiales— es suficiente para V1?
2. Si no es suficiente, ¿qué indicadores son obligatorios y cuáles pueden
   quedar para una fase posterior?
3. Para cada indicador obligatorio, ¿cuál es la fuente, periodo, permiso,
   frecuencia de actualización y resultado esperado?
4. ¿Qué avisos son requeridos y quién los puede ver?
5. ¿Debe mostrarse algún dato de saldo, cantidad o facturación en el inicio, o
   deben permanecer sólo en sus módulos y reportes?

Las siguientes métricas son opciones para discutir, no requisitos asumidos:

- total de materiales o productos;
- entradas o salidas del periodo;
- materiales utilizados;
- cargas de facturación;
- errores de carga;
- actividad reciente.

### Criterio de desbloqueo y aceptación

`DASHBOARD_V1` sólo puede cambiar de `PENDING_BUSINESS` después de la
respuesta de negocio:

- si el dashboard actual es suficiente, pasa a `OUT_OF_SCOPE_V1` para nuevas
  ampliaciones;
- si se requieren adiciones, pasa a `READY_FOR_IMPLEMENTATION` sólo cuando
  estén priorizadas y suficientemente definidas.

La decisión debe incluir:

- clasificación `MUST_HAVE_V1`, `POST_V1` o `NOT_REQUIRED` para cada métrica;
- fórmula, fuente, periodo, permisos y frecuencia de actualización de cada
  `MUST_HAVE_V1`;
- casos de aceptación con valores esperados y comportamiento para sin datos;
- tratamiento de errores y datos no disponibles.

### Respuesta de negocio

`PENDING_BUSINESS` — no se agregan KPIs por inferencia.

### Estado

`DASHBOARD_V1 = PENDING_BUSINESS`

---

## Propuesta de alcance V1

Esta sección distingue el estado técnico de una decisión formal de alcance.

### CONFIRMED

- La configuración productiva fue integrada en `dev` y la auditoría de Bitácora
  quedó publicada e integrada.
- La aplicación nueva entrega autenticación, autorización, consultas y las
  funciones documentadas en su estado actual.
- Bitácora V1 quedó auditada con consistencia de productores, enums, paginación,
  reportes, exportación y privacidad.
- Facturación V1 cubre validación, preview y staging durable, no confirmación
  operativa hacia `CALE_IMMEX`.
- Saldos no tiene todavía contrato HTTP aprobado.
- Dashboard existe como pantalla operativa con accesos, avisos, estado y total
  de Materiales.

### PENDING

- Fórmula, fuente, granularidad, corte y aceptación de Saldos.
- Pipeline autoritativo, reglas, efectos, idempotencia, rollback y caso de
  aceptación de confirmación de Facturación.
- Suficiencia y priorización de Dashboard V1.
- Aprobación funcional de empresa/asesor para los tres temas.

### OUT OF SCOPE

Hasta que exista una decisión distinta y autorizada, no se incluye en esta fase:

- implementar cálculo o reproceso de saldos;
- ejecutar `SALDOS*`, descargos, PEPS, concentrados u otros procesos mutables
  legacy desde consultas;
- confirmar facturas contra `CALE_IMMEX` o modificar inventario, salidas,
  productos, clientes o facturas;
- elegir entre flujos legacy por inferencia;
- agregar KPIs o reglas de dashboard no aprobados;
- cambios de Java, Angular, SQL, procedimientos almacenados o migraciones como
  parte de este paquete documental.

### Clasificación solicitada para la reunión

Para cada requisito o métrica que la empresa considere, registrar una sola
clasificación:

- `MUST_HAVE_V1`
- `POST_V1`
- `NOT_REQUIRED`

`V1_SCOPE_FROZEN = NO` hasta recibir respuestas y aprobación.

---

## Lista corta para la reunión de negocio

### Saldos

1. ¿Cuál es la fórmula oficial y qué fuente la respalda?
2. ¿Cuál es la fila del reporte: partida, material, pedimento, fracción u otra?
3. ¿Qué fecha/corte y qué tratamientos de retorno, ajuste, merma y desperdicio
   aplican?
4. ¿Qué dos o tres casos reales anonimizados deben reconciliarse?

### Confirmación de Facturación

1. Después de preview aprobado, ¿qué botón/proceso confirma?
2. ¿Qué tablas o registros deben cambiar y dónde se verifica el resultado?
3. ¿Cuál es el flujo autoritativo entre `CARGA_FACTURAS`,
   `CARGAFACTURASENPSALIDAS`, `CREAPRODUCTOSCARGAFACTURA` y otros?
4. ¿Qué ocurre con duplicados, reintentos, errores parciales y rollback?
5. ¿Pueden proporcionar un archivo autorizado de una a tres filas y su
   resultado esperado?

### Dashboard

1. ¿El dashboard actual es suficiente para V1?
2. Si no, ¿qué métricas son `MUST_HAVE_V1`, `POST_V1` o `NOT_REQUIRED`?
3. Para cada métrica obligatoria, ¿cuál es la fórmula, periodo, fuente, permiso y
   valor esperado sin datos?

---

## Impacto en el reporte académico

- **Alcance:** puede afirmarse que la solución entregada cubre las funciones
  implementadas y auditadas, mientras Saldos, confirmación operativa de
  Facturación y ampliaciones de Dashboard quedan sujetas a decisión funcional.
- **Método:** se puede documentar separación entre evidencia observada,
  análisis read-only, validación de contratos y decisiones pendientes; no se
  deben presentar hipótesis legacy como reglas de negocio.
- **Resultados:** pueden afirmarse la configuración productiva integrada, la
  auditoría de Bitácora con resultado PASS y la carga de Facturación hasta
  validación/preview/staging durable.
- **Limitaciones:** deben declararse la falta de fórmula y corte aprobados para
  Saldos, la falta de pipeline de confirmación autorizado para Facturación y la
  falta de priorización empresarial de KPIs de Dashboard.
- **Recomendación:** cerrar estas tres decisiones con casos de aceptación
  verificables antes de abrir una fase de implementación funcional adicional.

---

## Estado de control

- `TECHNICAL_INTERNAL_BLOCKERS = 0`: no existen blockers técnicos internos
  identificados actualmente dentro del alcance ya definido y auditado; esto no
  significa `V1_COMPLETE = YES` ni `V1_SCOPE_FROZEN = YES`.
- `SALDOS = PENDING_BUSINESS`.
- `FACTURACION_CONFIRM = PENDING_BUSINESS`.
- `DASHBOARD_V1 = PENDING_BUSINESS`.
- `SCOPE_DECISION_REQUIRED = YES`.
- `V1_SCOPE_FROZEN = NO`.
- Esta rama es documental: no modifica Java, Angular, SQL, SP ni migraciones.
