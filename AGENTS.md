# AGENTS.md

Instrucciones de contexto para agentes que trabajan en este repositorio.

## Estructura

```
backend/     → API Spring Boot 4.1 (Java 21, Gradle 9.7.1 Kotlin DSL + wrapper)
frontend/    → App Angular 22 (pnpm, SCSS, Angular Material)
infra/sql/   → Bootstrap BD: crea ANEXO24_DEV + usuario de aplicación
docs/        → Documentación del proyecto (requerimientos, análisis, diseño...)
.github/     → CI: sp-first-gate + backend test/build + frontend test/lint/build
```

## Comandos

```bash
# Backend (wrapper — nunca instalar Gradle globalmente)
cd backend
./gradlew.bat bootRun        # Windows; Linux: ./gradlew bootRun
./gradlew test               # tests
./gradlew build

# Frontend — SOLO pnpm (npm prohibido por decisión del proyecto)
cd frontend
pnpm install
pnpm start                   # usa proxy.conf.json: /api → localhost:8080
pnpm build
pnpm lint
```

Verificación de cadena completa: `GET http://localhost:8080/actuator/health` (Spring Security lo protege; autenticar con usuario generado en el log o pasar `--args="--spring.security.user.name=admin --spring.security.user.password=..."` a bootRun).

## Secretos y `.env` (gotcha crítico)

- Spring **no lee `.env` automáticamente**. `build.gradle.kts` tiene `loadDotEnv()` que inyecta `.env` en las tareas `bootRun` y `test`. Si el backend corre fuera de Gradle (IDE), hay que setear las variables de entorno manualmente.
- `backend/.env` está gitignored (contiene credenciales reales); `.env.example` es el template versionado.
- **Nombres de variables: `DB_URL`, `DB_USERNAME`, `DB_PASSWORD`.** `DB_USER` es un nombre obsoleto que NO funciona.
- Prohibido hardcodear credenciales en YAML o Java. Siempre `${DB_VAR}`.
- BD real es REMOTA: `10.110.110.2\SERVERSAPBO_DEV` → `ANEXO24_DEV`, usuario `opdatos`. El SQL Server local de la máquina tiene TCP/IP deshabilitado — no perseguir conexiones locales.

## Perfiles Spring (`backend/src/main/resources/`)

- `application.yml` → profile default: `local`
- `application-local.yml` → `${DB_URL}` SIN default (requiere `.env`; fail-fast si falta)
- `application-test.yml` → defaults seguros OBLIGATORIOS: CI corre tests sin `.env`. No quitar los defaults.
- `application-prod.yml` → estricto, Swagger deshabilitado.

## Arquitectura (hexagonal modular — regla dura)

Paquete raíz `com.jovycandy.anexo24`:

- `shared/` (api, exception, validation, audit), `security/`
- `catalogs/` → materials, products, structures
- `operations/` → entries, exits, usedmaterials, fixedassets
- `reports/`, `billing/`
- `administration/` → users, profiles, permissions

Cada feature: `domain/model` + `domain/port`, `application/command` + `query` + `usecase`, `infrastructure/persistence`, `api/controller` + `dto` + `mapper`.

Regla de dependencia: `api → application → domain ← infrastructure`. **El dominio NUNCA debe importar Spring, JDBC ni clases de SQL Server** (no JdbcTemplate en domain).

Nombres de carpetas en inglés (renombrados deliberadamente desde español).

## Base de datos autoritativa (regla dura)

- **`CALE_IMMEX` es la única BD operativa autoritativa.** Prohibido crear otra BD operativa, esquema paralelo o copia de tablas legacy (`MATERIAL`, `PRODUCTOS`, `IMPORTACIONES`, `PARTIDAS`, `SALIDAS`, `PSALIDAS`, `DESCARGA`, `DIRIGIDO`, clientes, proveedores, estructuras, inventarios). Prohibida la sincronización o migración progresiva `CALE_IMMEX → otra BD`. La aplicación se adapta a `CALE_IMMEX`.
- **`ANEXO24_DEV/app24` es infraestructura complementaria** (usuarios, perfiles, permisos, bitácora, staging controlado, errores de cargas, metadata técnica), nunca la nueva base operativa.
- **SP-FIRST estricto:** buscar SP existente → auditar contrato y side effects (directos/transitivos) → reutilizar; sólo si el contrato no sirve, crear `dbo.APP24_Q_*` / `dbo.APP24_C_*` en `CALE_IMMEX` consumiendo las tablas existentes. Clasificar: `SP_EXISTING_REUSABLE`, `SP_EXISTING_NOT_REUSABLE`, `NEW_SP_REQUIRED`, `TECHNICAL_EXCEPTION`, `FALSE_POSITIVE`.
- **Java sólo invoca SP** para acceso funcional; cero SQL inline (única excepción: `SystemStatusController` → `SELECT 1`).
- **Patrón de referencia: pedimentos.** Staging técnico en `app24.CargaPedimento*`; la operación autoritativa termina en `CALE_IMMEX.dbo.IMPORTACIONES/PARTIDAS/SALIDAS/PSALIDAS/DIRIGIDO` vía `APP24_C_PEDIMENTO_CONFIRMAR`. No se duplicaron tablas legacy.
- **Tablas nuevas de negocio:** sólo con justificación y autorización explícita; antes buscar equivalente/view/SP/relación existente. "Más limpio/moderno/facilita JPA" no es justificación.
- **La arquitectura hexagonal vive en la aplicación** (domain/application/ports/adapters aislan el modelo legacy, no lo reemplazan).
- **Fixtures sintéticos** (`LP_SOURCE`/`LP_TARGET` y similares) sólo dentro de Testcontainers; nunca en LIVE ni como destino de migración.
- **Identidad runtime SQL = `opdatos` (actual y autorizada).** La aplicación se conecta con `opdatos`; no cambiar la identidad SQL sin autorización explícita. El least privilege (`anexo24_app`, scripts `infra/sql/04-app-runtime-permissions.sql` … `07-runtime-security-verify.sql`) queda clasificado `DEFERRED_SECURITY_HARDENING`: propuesta futura versionada, no ejecutar en LIVE, no ampliar, y no bloquear por ello el desarrollo funcional del Anexo 24.

## CI (GitHub Actions)

- `chmod +x gradlew` requerido: commits desde Windows pierden el bit de ejecución.
- Frontend: `pnpm/action-setup` debe ir ANTES que `setup-node` (el `cache: pnpm` de setup-node necesita pnpm presente).
- Frontend: el step Test de CI (`pnpm test` = `ng test`, builder `unit-test` de Angular 22) SÍ está activo y ejecuta la suite; no quitarlo.
- Testcontainers versionado explícito (`1.20.4`): el BOM `2.0.5` falló la resolución. No regresar al BOM. En esta máquina (Docker Desktop con API mínima 1.40) los SQL IT locales requieren `JAVA_TOOL_OPTIONS=-Dapi.version=1.44`; en CI no hace falta.

## Convenciones

- **Comentarios/Javadoc/KDoc en español.** Javadoc con semántica estándar (`@param`, `@return`).
- Commits en español, convencionales: `feat:`, `fix(ci):`, `docs:`, `refactor:`.
- **Branching:** `main` (producción, estable), `staging` (pre-producción/QA), `dev` (desarrollo activo). Features se ramifican de `dev`: `feature/*`. Flujo: `feature/*` → `dev` → `staging` → `main`.
- Spring Boot 4.x reorganizó paquetes de auto-config (ej. `org.springframework.boot.jdbc.autoconfigure.*`, no `org.springframework.boot.autoconfigure.jdbc.*`) — usar nombres actuales.
- Gradle Kotlin DSL: `io.spring.dependency-management` es necesario (sin él no resuelven versiones de starters).