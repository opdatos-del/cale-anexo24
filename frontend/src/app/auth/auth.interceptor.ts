import { HttpInterceptorFn } from '@angular/common/http';
import { inject } from '@angular/core';
import { AuthService } from '../auth/auth.service';

/**
 * Adjunta el token JWT a cada solicitud autenticada.
 */
export const authInterceptor: HttpInterceptorFn = (req, next) => {
  const auth = inject(AuthService);
  const token = auth.token();
  if (token) {
    const clonada = req.clone({ setHeaders: { Authorization: `Bearer ${token}` } });
    return next(clonada);
  }
  return next(req);
};