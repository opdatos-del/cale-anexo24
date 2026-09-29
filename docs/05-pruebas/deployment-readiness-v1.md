# DEPLOYMENT_READINESS_V1_AUDIT

## Alcance

Esta auditoría cierra la preparación de empaquetado local product-like para
Angular + Spring Boot. No despliega a un servidor real y no inicia legacy parity.
No se modificaron Java, Angular, SQL, stored procedures, migraciones ni lógica
de negocio.

Las decisiones de negocio permanecen sin cambios:

- `SALDOS = PENDING_BUSINESS`.
- `FACTURACION_CONFIRM = PENDING_BUSINESS`.
- `DASHBOARD_V1 = PENDING_BUSINESS`.

## STATIC

### Artefactos

- `backend/Dockerfile`: presente.
- `backend/.dockerignore`: presente.
- `frontend/Dockerfile`: presente.
- `frontend/.dockerignore`: presente.
- `frontend/nginx.conf`: presente.
- `compose.prod.yml`: presente.
- `docs/06-devops/contenedores.md`: actualizado.
- `docs/06-devops/ambientes.md`: actualizado.

### Compose y red

- Servicios: `backend` y `frontend`.
- No existe servicio SQL Server.
- Red: `anexo24`, bridge.
- `internal: true`: no configurado.
- Egress a `DB_URL` y `APP_DB_URL`: permitido por diseño.
- Backend publicado al host: no.
- Frontend publicado: `8088:8080` por defecto.
- Perfil backend: `SPRING_PROFILES_ACTIVE=prod`.
- Variables obligatorias: recibidas desde el entorno del host.
- `env_file: backend/.env`: no usado.

`docker compose -f compose.prod.yml config --quiet` pasó usando placeholders no
sensibles. No se imprimieron valores de configuración.

### Diseño de imágenes

Backend:

- Build JDK 21.
- Runtime JRE 21.
- Usuario no-root `anexo24`.
- Boot JAR único en `/app/app.jar`.
- Healthcheck read-only con `curl` contra `/actuator/health`.
- `JAVA_TOOL_OPTIONS` disponible sin shell intermedio.
- `.env`, source, tests, `.gradle`, `.git` y logs excluidos del contexto/runtime.

Frontend:

- Build Node 22 + pnpm 12.4.0.
- Output raíz verificado: `dist/frontend`.
- Contenido browser verificado: `dist/frontend/browser`.
- Runtime `nginxinc/nginx-unprivileged`.
- Node, pnpm y TypeScript no existen en la imagen final.
- Fallback SPA y proxy `/api/` configurados.

### Seguridad estática

- Compact JWT: `0`.
- Bearer token real: `0`.
- JDBC connection string sensible: `0`.
- Authorization real: `0`.
- Credenciales persistidas: no.
- `.env` real staged: no.
- Trailing whitespace en artefactos/documentación: `0`.

## BUILD

### Gates locales previos

No se repitieron porque no cambió Java, Angular, Gradle ni package configuration:

- Backend tests: PASS.
- Backend build: PASS.
- Backend bootJar: PASS.
- Frontend tests: 80/80 PASS.
- Frontend lint: PASS.
- Frontend build: PASS.

### Artefactos locales

- Backend boot JAR: generado correctamente, aproximadamente 55.4 MB.
- Frontend static output: generado correctamente en `dist/frontend`.
- Backend context aproximado: 836,062 bytes.
- Frontend context aproximado: 1,542,562 bytes.

### Docker Engine

- Docker CLI: disponible.
- Compose: disponible, `v5.3.1`.
- Contexto activo: `desktop-linux`.
- Docker Desktop fue iniciado normalmente y el daemon respondió.
- `DOCKER_ENGINE_AVAILABLE = YES`.
- No se instaló otro engine.
- No se modificó WSL.
- No se cambió persistentemente el contexto Docker.

### Imágenes construidas

- Backend: `198 MB`.
- Frontend: `24.2 MB`.
- Build Compose backend: PASS.
- Build Compose frontend: PASS.

Backend inspection:

- Usuario: `anexo24`.
- Java: Temurin 21 JRE.
- `javac`: ausente.
- `curl`: presente.
- `/app/app.jar`: presente.
- `/app/.env`, `/app/src`, `/app/.git`: ausentes.

Frontend inspection:

- Nginx: presente, `nginx -t` PASS.
- Node: ausente.
- pnpm: ausente.
- `index.html`: presente.
- JS: 30 archivos.
- CSS: 1 archivo.
- TypeScript: 0 archivos.
- `.env`: 0 archivos.

## RUNTIME

- `docker compose up -d`: PASS.
- Backend: running.
- Backend healthcheck: healthy, failing streak `0`.
- Frontend: running.
- `docker compose down`: PASS.
- Containers restantes después de `down`: `0`.

HTTP a través de Nginx:

- Root `/`: HTTP 200, `text/html`, Angular `<app-root>` presente.
- Browser index: 17,716 bytes.
- Assets: 30 JS y 1 CSS.
- Rutas SPA solicitadas: HTTP 200 y HTML en todas:
  `/dashboard`, `/materiales`, `/productos`, `/estructuras`,
  `/operaciones/entradas`, `/operaciones/salidas`,
  `/operaciones/materiales-utilizados`, `/operaciones/activos-fijos`,
  `/reportes`, `/bitacora`, `/usuarios`, `/perfiles` y `/facturacion`.
- `/api/v1/bitacora` sin token: HTTP 401.
- Proxy path: se conserva `/api/v1/...` porque `proxy_pass` no agrega URI.
- Actuator vía frontend: no se expone como Actuator; la ruta devuelve la SPA.
- Security headers: `X-Content-Type-Options`, `Referrer-Policy` y
  `X-Frame-Options` presentes.
- `index.html`: sin `Cache-Control` largo.
- Asset hashado: cache largo con `max-age=31536000`.
- Smoke browser `/dashboard`: HTTP 200, Angular cargado, `app-root` presente,
  0 errores de consola y 0 respuestas HTTP 5xx.

No se contó con una credencial autorizada para ejecutar el smoke autenticado y
no se fabricó ningún token:

- `AUTHENTICATED_PROXY_SMOKE = NOT_EXECUTED`.

## Findings

### DEPLOYMENT-001 — Docker Desktop inicialmente detenido

- **Severity:** environment blocker, resuelto.
- **Layer:** local runtime.
- **Evidence:** inicialmente `dockerDesktopLinuxEngine` no respondía y
  `Docker Desktop.exe` no estaba activo.
- **Resolution:** Docker Desktop fue iniciado normalmente; el daemon respondió
  sin cambiar WSL, contexto persistente ni instalar otro engine.

### DEPLOYMENT-002 — Ruta browser del artefacto Angular

- **Severity:** packaging bug, resuelto.
- **Layer:** frontend image.
- **Evidence:** la primera imagen copió `dist/frontend` completo y Nginx sirvió
  su índice default de 896 bytes sin JS/CSS.
- **Resolution:** el Dockerfile ahora copia `dist/frontend/browser` al web root.
  La imagen fue reconstruida y el runtime confirmó el índice Angular real,
  30 JS, 1 CSS y `app-root` presente.

## Scope discipline

- Saldos changed: **NO**.
- Billing confirmation changed: **NO**.
- Dashboard business changed: **NO**.
- SQL writes: `0`.
- Legacy procedures executed: `0`.

## Decision

- `DOCKER_ENGINE_AVAILABLE = YES`.
- `BACKEND_IMAGE_READY = PASS`.
- `FRONTEND_IMAGE_READY = PASS`.
- `REVERSE_PROXY_READY = PASS`.
- `COMPOSE_PRODUCT_LIKE_READY = PASS`.
- `SECRET_HANDLING = PASS`.
- `AUTHENTICATED_PROXY_SMOKE = NOT_EXECUTED`.
- `DEPLOYMENT_READINESS_V1_READY_TO_COMMIT = YES`.

El runtime quedó detenido limpiamente con `docker compose down`. No se realizó
commit ni push.
