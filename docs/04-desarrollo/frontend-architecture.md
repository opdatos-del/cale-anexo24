# Arquitectura frontend

## Alcance

El frontend usa Angular standalone, organizado por features. La migración actual cubre las funcionalidades existentes: autenticación, shell, dashboard y catálogo de materiales. No se crean carpetas para módulos que todavía no tienen implementación.

## Árbol real

```text
src/app/
├── core/
│   ├── auth/auth.service.ts
│   ├── guards/auth.guard.ts
│   └── interceptors/auth.interceptor.ts
├── features/
│   ├── auth/
│   │   ├── auth.routes.ts
│   │   └── presentation/pages/login/login.page.ts
│   ├── dashboard/
│   │   ├── application/dashboard-summary.service.ts
│   │   ├── dashboard.routes.ts
│   │   └── presentation/pages/dashboard/dashboard.page.ts
│   └── catalogs/materials/
│       ├── application/use-cases/search-materials.use-case.ts
│       ├── domain/models/material.model.ts
│       ├── domain/repositories/material.repository.ts
│       ├── infrastructure/api/material.api.ts
│       ├── infrastructure/dto/material-response.dto.ts
│       ├── infrastructure/mappers/material.mapper.ts
│       ├── infrastructure/repositories/http-material.repository.ts
│       ├── presentation/pages/material-list/material-list.page.ts
│       └── materials.routes.ts
├── layout/main-layout.component.ts
├── app.config.ts
└── app.routes.ts
```

## Reglas de dependencia

- `domain` contiene modelos y puertos; no importa Angular, Material ni HTTP.
- `application` coordina casos de uso y depende de `domain`.
- `infrastructure` implementa puertos y concentra `HttpClient`, URLs, DTOs y mapeos.
- `presentation` contiene páginas Angular, Material, Tailwind y routing de feature.
- `core` contiene infraestructura transversal: sesión, guard e interceptor.
- `layout` contiene únicamente shell, navegación y estado responsive.
- No existe `shared` porque todavía no hay una pieza reutilizada por varios dominios que justifique extraerla.

## Flujo de Materials

```text
MaterialListPage
  → SearchMaterialsUseCase
  → MaterialRepository
  → HttpMaterialRepository
  → MaterialApi
  → HttpClient
  → /api/v1/catalogos/materiales
```

`MaterialResponseDto` no se expone a la UI. `MaterialMapper` transforma la respuesta HTTP al modelo `Material`; los eventos de `MatPaginator` se convierten en criterios propios de la aplicación.

## Routing y lazy loading

`app.routes.ts` sólo compone el shell y carga las rutas de auth, dashboard y materiales. Cada feature mantiene su archivo de rutas y se carga de forma lazy. El guard se aplica al shell autenticado.

## Estado y errores

Se mantienen signals locales y RxJS; no se introduce NgRx. Los adaptadores HTTP no muestran notificaciones ni abren diálogos. La página decide cómo representar loading/error.

## Convenciones futuras

Una nueva feature debe ser autocontenida y comenzar con la menor cantidad de capas que su complejidad requiera. Sólo se agrega `shared` cuando una pieza tenga al menos dos consumidores de dominios distintos. Reportes, operaciones y administración se migrarán cuando exista código funcional para ellos.
