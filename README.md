# ANEXO 24

Sistema de gestión para el control de materiales, productos, estructuras y operaciones del almacén, con facturación, reportes y administración de usuarios.

## ¿Qué es?

ANEXO 24 es una plataforma web que centraliza la operación del almacén: entradas y salidas de materiales, productos y activos fijos, catálogos, reportes y facturación, sobre una base única de datos SQL Server.

## Arquitectura

Monolito modular con arquitectura hexagonal: el dominio no sabe qué hay detrás de la infraestructura (SQL Server, HTTP), lo que mantiene la lógica de negocio limpia y probable.

```
Frontend (Angular)  ──REST /api/v1──►  Backend (Spring Boot)  ──JDBC──►  SQL Server
       │                                        │
       └── proxy local ─────────────────────────┘
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

1. Ejecutar `infra/sql/00-bootstrap.sql` para crear la base `ANEXO24_DEV` y el usuario de aplicación.
2. Copiar `backend/.env.example` a `backend/.env` y cargar los datos reales de conexión.

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
cd frontend && pnpm lint && pnpm test
```

## Configuración por ambientes

Backend: perfiles Spring en `backend/src/main/resources/` (`local`, `test`, `prod`). Los secretos siempre vienen de variables de entorno, nunca de archivos versionados.

## CI

Cada push valida: tests y build del backend, lint, tests y build del frontend.