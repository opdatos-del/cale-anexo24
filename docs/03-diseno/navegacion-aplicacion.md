# Navegación de aplicación V1

## Criterio

Navegación agrupada por dominio, sin mostrar módulos futuros ni duplicar rutas. Cada grupo desaparece cuando no contiene hijos autorizados.

- **Inicio:** dashboard.
- **Catálogos:** materiales, productos, estructuras, catálogos auxiliares y socios comerciales.
- **Operaciones:** entradas, salidas, materiales utilizados, activos fijos y carga de pedimentos.
- **Interfaces:** importaciones de catálogos y carga de facturación.
- **Reportes:** reportes disponibles.
- **Administración:** usuarios, perfiles y bitácora.

Pedimentos permanece en Operaciones por representar una carga operativa aduanera. Importaciones de catálogos vive sólo en Interfaces.

## Modelo de scroll del shell

El shell autenticado ocupa exactamente `100dvh` y bloquea su overflow. El documento exterior no participa en el desplazamiento vertical: `window.scrollY` permanece en cero y `.main-content-scroll` es el contenedor de scroll principal. Sidebar y main tienen altura limitada al viewport.

El sidebar usa tres zonas: header fijo, navegación flexible con `min-height: 0` y `overflow-y: auto` sólo cuando hace falta, y footer de cuenta siempre visible. El scroll del contenido y el scroll interno opcional de navegación son independientes.

Modo expandido muestra grupos colapsables. Mini rail conserva ancho de 76 px y abre hijos mediante `MatMenu`, sin dejar espacio residual. En móvil funciona como drawer overlay de altura completa y cierra al elegir ruta.

## RBAC y ruta activa

Metadata de grupos/items está centralizada en `navigation.config.ts`. Cada item declara `permission` o `anyOfPermissions`; grupos vacíos no se renderizan. `routerLinkActive` marca hijos y el grupo refleja rutas hijas activas.

## Cuenta

Footer muestra iniciales, nombre disponible y estado de sesión. Menú de cuenta mantiene cierre de sesión. Correo, perfil, Mi cuenta y cambio de contraseña no aparecen porque el contexto/rutas actuales no los ofrecen. Administración permanece en navegación principal.

## Validación

- `AUTHENTICATED_BROWSER_VALIDATION = NOT_EXECUTED_ENVIRONMENT`: el proceso del agente no recibió credenciales E2E.
- Regresión automatizada de scroll y geometría implementada para cuatro viewports; queda disponible para CI o un entorno E2E autorizado.
- Pruebas unitarias y de componentes: `PASS`.
- Carga product-like del shell, rutas SPA, proxy `401` y headers: `PASS`.

Esta limitación ambiental no se interpreta como aprobación ni fallo visual del navegador autenticado.

## Accesibilidad y movimiento

Botones nativos soportan teclado; grupos exponen `aria-expanded`, navegación y cuenta tienen etiquetas. Rail usa tooltips. Transiciones son breves y se reducen con `prefers-reduced-motion`.
