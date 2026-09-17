import { CanActivateFn, Router } from '@angular/router';
import { inject } from '@angular/core';
import { AuthService } from './auth.service';

/**
 * Bloquea rutas sin sesión activa redirigiendo al login.
 */
export const authGuard: CanActivateFn = () => {
  const auth = inject(AuthService);
  const router = inject(Router);
  if (auth.tieneSesion()) {
    return true;
  }
  return router.parseUrl('/login');
};