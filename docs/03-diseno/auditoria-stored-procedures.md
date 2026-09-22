# Auditoría global de Stored Procedures y SQL inline — FASE SP-0

## Alcance y evidencia

Auditoría creada desde `feature/backend-administration-users-commands-sensitive` (`09057de`). El inventario histórico versionado `docs/03-diseno/procedimientos-almacenados.md`, generado el 15/09/2026 07:13, registra **88 SP de CALE_IMMEX**; no prueba que sigan siendo 88 LIVE.

SP-0B intentó sólo `SELECT COUNT(*) FROM sys.procedures` con `backend/.env` y ODBC Driver 18 para ambas bases. `CALE_IMMEX` y `ANEXO24_DEV` fallaron por confianza TLS. No se aplicó `encrypt=false`, `trustServerCertificate=true` ni otro bypass; no se ejecutó SQL adicional, procedimientos ni comandos mutables. Por tanto, conteos y candidatos LIVE son **PENDIENTE**, no inferencias.

SP-0C confirmó TCP alcanzable para ambas bases, alojadas en la misma instancia configurada. Con Microsoft JDBC `13.4.0.jre11`, `encrypt=true` y `trustServerCertificate=false`, ambas conexiones fallan con `com.microsoft.sqlserver.jdbc.SQLServerException` causado por `javax.net.ssl.SSLHandshakeException` / `sun.security.provider.certpath.SunCertPathBuilderException`: `PKIX path building failed: unable to find valid certification path to requested target`. Clasificación: **TLS_CA_NO_CONFIABLE**. No se observó evidencia de `HOSTNAME_MISMATCH`, certificado expirado o aún no válido; la validación se detuvo antes, al no poder construir cadena de confianza.

No hay truststore configurado por proyecto ni variables `javax.net.ssl.trustStore` detectadas; Java usado: Oracle JDK `21.0.9` (`java.home` bajo instalación local de JDK 21). Windows tiene un certificado raíz autofirmado local con nombre corporativo, pero no está en el truststore Java y no hay evidencia read-only de que sea la CA/cadena presentada por SQL Server ni de una intermedia aprobada. No se exportó ni importó certificado, no se creó truststore y no se cambió configuración versionada. Estado: **TLS_BLOQUEADO_ESPERANDO_CA_INFRAESTRUCTURA**.

Para continuar se requiere de Infraestructura/DBA: certificado de CA raíz, certificado(s) intermedio(s) de la cadena del SQL Server y hostname oficial incluido en SAN; no se necesita contraseña, clave privada ni PFX con clave privada.

Regla objetivo: toda operación SQL funcional debe migrar a Stored Procedure. `SystemStatusController.checkDatabase` usa `SELECT 1`: **EXCEPCION_TECNICA**, health check, no deuda funcional.

## Inventario de SQL inline funcional

| Dominio | Archivo / método | Datasource | Tablas | Operación | Clasificación | Decisión / acción futura | Evidencia |
|---|---|---|---|---|---|---|---|
| Materiales | `MaterialJdbcAdapter.findPage` | `jdbcTemplate`, CALE_IMMEX | `dbo.material` | COUNT + listado/paginación | MIGRAR_A_SP | **PENDIENTE** discovery LIVE; si no cubre contrato, **CREAR** `dbo.APP24_Q_MATERIALES_LISTAR` | Java |
| Auth | `UsuarioJdbcAdapter.findByClave` | `appJdbcTemplate`, ANEXO24_DEV | `UsuarioApp` | usuario por clave | MIGRAR_A_SP | **CREAR** propuesta `app24.APP24_Q_USUARIO_POR_CLAVE` si LIVE no ofrece contrato | Java |
| Auth | `UsuarioJdbcAdapter.findAccesoByUsuario` | `appJdbcTemplate` | Usuario/Perfil/PerfilActividad/Actividad | perfil, estado, permisos | MIGRAR_A_SP | **CREAR** propuesta `app24.APP24_Q_USUARIO_ACCESO` | Java |
| Usuarios read | `UsuarioConsultaJdbcAdapter.findPage/findById` | `appJdbcTemplate` | UsuarioApp/PerfilApp | lista, count, detalle | MIGRAR_A_SP | **CREAR** `app24.APP24_Q_USUARIOS_LISTAR`, `app24.APP24_Q_USUARIO_OBTENER` | Java |
| Usuarios commands | `UsuarioComandoJdbcAdapter.exists*`, `crear`, `actualizarDatos` | `appJdbcTemplate` | UsuarioApp | unicidad, insert, update | MIGRAR_A_SP | **CREAR** `APP24_C_USUARIO_CREAR`, `APP24_C_USUARIO_ACTUALIZAR_DATOS` | Java |
| Fase 3B | `UsuarioComandoJdbcAdapter.actualizarEstado/Perfil/Vigencia` | `appJdbcTemplate` | UsuarioApp | updates sensibles | MIGRAR_A_SP | **CREAR** `APP24_C_USUARIO_CAMBIAR_ESTADO/PERFIL/VIGENCIA` | Java |
| Guardrail | `UsuarioComandoJdbcAdapter.existsConCapacidadAdministrativa` | `appJdbcTemplate` | UsuarioApp/PerfilApp/PerfilActividad/Actividad | administrador efectivo | MIGRAR_A_SP | **CREAR** `app24.APP24_Q_ADMINISTRADOR_EFECTIVO` o incluir en commands atómicos | Java |
| Perfil referencia | `PerfilReferenciaJdbcAdapter.findEstadoById` | `appJdbcTemplate` | PerfilApp | estado perfil | MIGRAR_A_SP | **CREAR** `app24.APP24_Q_PERFIL_ESTADO` | Java |
| Bitácora | `BitacoraJdbcAdapter` | `appJdbcTemplate` | BitacoraEvento | insert evento | MIGRAR_A_SP | **CREAR** `app24.APP24_C_BITACORA_REGISTRAR` | Java |
| Bitácora | `BitacoraConsultaJdbcAdapter.findPage` | `appJdbcTemplate` | BitacoraEvento/UsuarioApp | lista + count | MIGRAR_A_SP | **CREAR** `app24.APP24_Q_BITACORA_LISTAR` | Java |
| Salud | `SystemStatusController.checkDatabase` | ambos | — | `SELECT 1` | EXCEPCION_TECNICA | Conservar | Java |

## SP versionados relevantes

| Base | SP | Tipo / mutabilidad | Objetos | Consumidor potencial | Decisión | Riesgo |
|---|---|---|---|---|---|---|
| CALE_IMMEX | `dbo.APP24_Q_PRODUCTOS_LISTAR` | QUERY / READ ONLY | productos | productos | REUTILIZAR, ya usado | bajo |
| CALE_IMMEX | `dbo.APP24_Q_ENTRADAS_LISTAR` | QUERY / READ ONLY | entradas legacy | entradas | REUTILIZAR, ya usado | bajo |
| CALE_IMMEX | `dbo.APP24_Q_SALIDAS_LISTAR` | QUERY / READ ONLY | salidas legacy | salidas | REUTILIZAR, ya usado | bajo |
| CALE_IMMEX | `dbo.APP24_Q_ESTRUCTURAS_LISTAR` | QUERY / READ ONLY | estructuras | estructuras | REUTILIZAR, ya usado | bajo |
| CALE_IMMEX | `dbo.APP24_Q_MATERIALES_UTILIZADOS_LISTAR` | QUERY / READ ONLY | descarga/materiales | materiales usados | REUTILIZAR, ya usado | bajo |
| CALE_IMMEX | `dbo.APP24_Q_ACTIVOS_FIJOS_LISTAR` | QUERY / READ ONLY | partidas/importaciones | activos | REUTILIZAR, ya usado | bajo |
| CALE_IMMEX | candidato materiales | UNKNOWN | `dbo.material` | `MaterialJdbcAdapter` | PENDIENTE metadata LIVE; no candidato confirmado | no crear/alterar aún |
| ANEXO24_DEV | candidato app24 | UNKNOWN | UsuarioApp, PerfilApp, PerfilActividad, Actividad, BitacoraEvento | auth, usuarios, bitácora | PENDIENTE metadata LIVE | no asumir inexistencia |

## Estado discovery LIVE

| Base | Conexión metadata | Total LIVE | Comparación |
|---|---|---|---|
| CALE_IMMEX | Bloqueada: `TLS_CA_NO_CONFIABLE`; TCP OK | PENDIENTE | Histórico 88; comparación PENDIENTE |
| ANEXO24_DEV | Bloqueada: `TLS_CA_NO_CONFIABLE`; TCP OK | PENDIENTE | PENDIENTE |

No se conoce diferencia `NUEVO_EN_LIVE`/`YA_EXISTÍA`/`YA_NO_EXISTE`/`MODIFICADO_DESDE_INVENTARIO` hasta resolver confianza TLS con configuración aprobada.

## Procedimiento LIVE pendiente

Ejecutar sólo metadata read-only, por base: `sys.procedures`, `sys.schemas`, `sys.parameters`, `sys.sql_modules`, `sys.sql_expression_dependencies` y descripción de result set sin ejecutar. Para cada candidato registrar definición, parámetros/defaults/output, dependencias, result set, orden, filtros, paginación y efectos secundarios. Nunca ejecutar SP WRITE/MIXED/UNKNOWN.

## Decisiones

- **REUTILIZAR:** seis `APP24_Q_*` versionados de CALE_IMMEX ya cubren sus dominios.
- **ADAPTAR:** ninguno confirmado. Cualquier SP legacy casi adecuado requiere análisis de consumidores y **REQUIERE APROBACIÓN**; no alterar parámetros/result sets/efectos.
- **CREAR:** propuestas app24 y Materiales anteriores, sujetas a que LIVE no revele SP adecuado.
- **PENDIENTE:** discovery LIVE completo de ambas bases; número total de SP de CALE_IMMEX y ANEXO24_DEV aún no determinado.

## Permisos futuros

Meta posterior: reemplazar grants directos de `SELECT`/`INSERT`/`UPDATE` por `GRANT EXECUTE ON OBJECT::<SP>` mínimo. No modificar `04-app-runtime-permissions.sql` en SP-0.
