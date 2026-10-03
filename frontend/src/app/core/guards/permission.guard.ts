import { inject } from '@angular/core';
import { ActivatedRouteSnapshot, CanActivateFn, Router } from '@angular/router';
import { AuthService } from '@core/auth/auth.service';

/**
 * Protege rutas de navegación según permisos de UX del usuario autenticado.
 *
 * Regla: si la ruta declara `permissions` (lista), esa lista es autoritativa y
 * exige al menos uno de esos permisos; así se evita que un `permission`
 * heredado de una ruta padre autorice rutas hijas con requisitos más estrictos.
 * Si `permissions` no está presente o viene vacío, se evalúa `permission`.
 */
export const permissionGuard: CanActivateFn = (route: ActivatedRouteSnapshot) => {
  const auth = inject(AuthService);
  const router = inject(Router);
  const permission = route.data['permission'] as string | undefined;
  const permissions = route.data['permissions'] as string[] | undefined;

  const allowed = permissions?.length
    ? auth.hasAnyPermission(...permissions)
    : permission
      ? auth.hasPermission(permission)
      : false;

  return allowed ? true : router.parseUrl('/forbidden');
};
