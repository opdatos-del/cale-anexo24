# AGENTS.md

Instrucciones de contexto para agentes que trabajan en este repositorio.

## Estructura

```
backend/     → API Spring Boot 4.1 (Java 21, Gradle 9.7.1 Kotlin DSL + wrapper)
frontend/    → App Angular 22 (pnpm, SCSS, Angular Material)
infra/sql/   → Bootstrap BD: crea ANEXO24_DEV + usuario de aplicación
docs/        → Documentación del proyecto (requerimientos, análisis, diseño...)
.github/     → CI: backend test/build + frontend lint/build
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

## CI (GitHub Actions)

- `chmod +x gradlew` requerido: commits desde Windows pierden el bit de ejecución.
- Frontend: `pnpm/action-setup` debe ir ANTES que `setup-node` (el `cache: pnpm` de setup-node necesita pnpm presente).
- No hay step de tests frontend: `angular.json` no tiene target `test` (scaffold con `--skip-tests`). No re-agregar `pnpm test` a CI sin configurar el runner antes.
- Testcontainers versionado explícito (`1.20.4`): el BOM `2.0.5` falló la resolución. No regresar al BOM.

## Convenciones

- **Comentarios/Javadoc/KDoc en español.** Javadoc con semántica estándar (`@param`, `@return`).
- Commits en español, convencionales: `feat:`, `fix(ci):`, `docs:`, `refactor:`.
- Branching simple: `main` + `feature/*`. Sin `develop`.
- Spring Boot 4.x reorganizó paquetes de auto-config (ej. `org.springframework.boot.jdbc.autoconfigure.*`, no `org.springframework.boot.autoconfigure.jdbc.*`) — usar nombres actuales.
- Gradle Kotlin DSL: `io.spring.dependency-management` es necesario (sin él no resuelven versiones de starters).