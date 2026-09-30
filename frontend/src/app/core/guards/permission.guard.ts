import { inject } from '@angular/core';
import { CanActivateFn, Router } from '@angular/router';
import { AuthService } from '@core/auth/auth.service';

/** Protege rutas de navegación según permisos de UX del usuario autenticado. */
export const permissionGuard: CanActivateFn = (route) => {
  const auth = inject(AuthService);
  const router = inject(Router);
  const permission = route.data['permission'] as string | undefined;
  const permissions = route.data['permissions'] as string[] | undefined;

  if ((permission && auth.hasPermission(permission)) || (permissions?.length && auth.hasAnyPermission(...permissions))) {
    return true;
  }

  return router.parseUrl('/forbidden');
};
