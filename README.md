# ANEXO 24

Sistema de gestión para el control de materiales, productos, estructuras y operaciones del almacén, con facturación, reportes y administración de usuarios.

## ¿Qué es?

ANEXO 24 es una plataforma web que centraliza la operación del almacén: entradas y salidas de materiales, productos y activos fijos, catálogos, reportes y facturación. La aplicación integra dos bases SQL Server: CALE_IMMEX, que conserva el Módulo C heredado, y ANEXO24_DEV, cuyo esquema app24 contiene las funcionalidades propias de la nueva aplicación.

## Arquitectura

Monolito modular con arquitectura hexagonal: el dominio no sabe qué hay detrás de la infraestructura (SQL Server, HTTP), lo que mantiene la lógica de negocio limpia y probable.

```
Frontend (Angular)  ──REST /api/v1──►  Backend (Spring Boot)
       │                                        ├── JDBC → CALE_IMMEX (Módulo C)
       └── proxy local ─────────────────────────└── JDBC → ANEXO24_DEV / app24
```

## Stack

| Capa | Tecnología |
|------|-----------|
| Backend | Java 21, Spring Boot 4.1, Spring Security, JDBC |
| Frontend | Angular 22, TypeScript, Angular Material, SCSS |
| Base de datos | SQL Server |
| Build | Gradle 9.7 (Kotlin DSL, wrapper) / pnpm |
| Documentación API | OpenAPI / Swagger UI |
| CI/CD | GitHub Actions |
| Pruebas | JUnit 5, Testcontainers, Angular testing, Playwright |

## Estructura

```
cale-anexo24/
├── backend/     → API Spring Boot (hexagonal por feature)
├── frontend/    → app Angular (core / shared / features)
├── infra/       → SQL: bootstrap de base de datos y usuario
├── docs/        → documentación del proyecto
└── .github/     → workflows de CI
```

## Requisitos

- Java 21
- Node.js 24 LTS + pnpm
- SQL Server (local o servidor de desarrollo)

## Puesta en marcha

### Base de datos

Ejecutar estos scripts con una identidad administrativa/de despliegue autorizada,
nunca con la cuenta runtime `anexo24_app`:

1. `infra/sql/00-bootstrap.sql`: crea `ANEXO24_DEV`, login y usuario runtime sin permisos globales.
2. `infra/sql/02-app-schema.sql`: crea el esquema y tablas `app24`.
3. `infra/sql/03-app-seed-security.sql`: carga perfiles, permisos y usuario inicial.
4. `infra/sql/04-app-runtime-permissions.sql`: retira roles heredados amplios y concede los permisos mínimos por objeto.

`02-app-schema.sql` recrea tablas en desarrollo; ejecutar nuevamente el paso 4
siempre después de ese script para restaurar permisos sobre objetos nuevos.

Después, copiar `backend/.env.example` a `backend/.env` y cargar datos reales de conexión.

### Backend

```bash
cd backend
./gradlew bootRun
```

- API: `http://localhost:8080`
- Swagger UI: `http://localhost:8080/swagger-ui.html`
- Health check: `http://localhost:8080/actuator/health`

### Frontend

```bash
cd frontend
pnpm install
pnpm start
```

- App: `http://localhost:4200`
- El proxy local redirige `/api` hacia el backend en `localhost:8080`.

### Pruebas

```bash
# Backend
cd backend && ./gradlew test

# Frontend
cd frontend && pnpm lint
```

## Configuración por ambientes

Backend: perfiles Spring en `backend/src/main/resources/` (`local`, `test`, `prod`). Los secretos siempre vienen de variables de entorno, nunca de archivos versionados.

## CI

Cada push valida los tests y el build del backend, además del lint y build del frontend. El frontend no tiene target de tests configurado actualmente.