# Marco teórico

Conceptos utilizados en el proyecto; cada uno conectado con una decisión real.
No es un manual de tecnologías.

## Anexo 24 y control de inventarios de comercio exterior

El Anexo 24 de las Reglas Generales de Comercio Exterior establece el control
automatizado de inventarios para empresas con programas de comercio exterior.
El proyecto materializa ese control en consultas, reportes y cargas que
reflejan la operación registrada en `CALE_IMMEX`, sin reinterpretar reglas
fiscales que no estén documentadas.

## Base operativa autoritativa y staging

`CALE_IMMEX` es la única base operativa autoritativa. Las cargas de archivos
no escriben directamente sobre ella: primero se validan y persisten en un
staging aislado (`ANEXO24_DEV.app24`) con hash, filas normalizadas y errores, y
sólo una confirmación autoritativa (con transacción e idempotencia) modifica la
operación cuando existe contrato aprobado.

## Arquitectura hexagonal (puertos y adaptadores)

Separa dominio, aplicación, puertos e infraestructura para aislar el modelo
legacy del código moderno: el dominio no depende de Spring ni de SQL Server, y
la API conversa con el dominio a través de casos de uso. Regla dura del
proyecto: `api → application → domain ← infrastructure`.

## Spring Boot y Java 21 (backend)

Implementa la API REST, la seguridad (JWT/RBAC), la validación de cargas y el
acceso a datos vía `JdbcTemplate` sobre procedimientos almacenados. Spring Boot
4 reorganizó paquetes de auto-configuración, detalle relevante para el
mantenimiento.

## Angular (frontend)

Aplicación SPA con Angular Material que consume la API a través de un proxy
`/api`. Organiza features por dominio (catálogos, operaciones, reportes,
administración), con guards de sesión y permisos, estados de carga/error/vacío
y diseño responsivo verificado (desktop/tablet/mobile).

## SQL Server y procedimientos almacenados

`CALE_IMMEX` y `ANEXO24_DEV` residen en SQL Server. El acceso funcional ocurre
exclusivamente mediante procedimientos almacenados (`dbo.APP24_Q_*` para
consultas, `app24.APP24_C_*` para comandos internos), lo que acota la
superficie de datos y centraliza reglas.

## SP-FIRST

Regla de trabajo del proyecto: antes de escribir SQL nuevo se busca un SP
existente, se audita su contrato y efectos, y se reutiliza si sirve; sólo si no
sirve se crea uno nuevo versionado. Un gate de CI (`check-inline-sql.py`)
verifica que Java no contenga SQL funcional embebido.

## Validación de archivos y hash anticuplicados

Las cargas `.xls/.xlsx` se parsean contra un contrato de columnas, se validan
tipos y obligatoriedad, se reportan errores localizables y se fingerprintan
(SHA-256) para impedir duplicados. El resultado es un preview paginado antes de
cualquier confirmación.

## JWT y RBAC

La sesión se representa con JWT firmado; los permisos viajan en el token y se
verifican en backend (`@PreAuthorize`) y en rutas del frontend (guards). El
frontend es UX; la frontera de seguridad es el backend.

## Pruebas y CI

La evidencia combina pruebas unitarias e integración (incluye
Testcontainers para SQL), pruebas E2E interactivas y gates automatizados en
GitHub Actions: SP-FIRST, permisos runtime, tests y build. CI es requisito de
integración, no un extra.
