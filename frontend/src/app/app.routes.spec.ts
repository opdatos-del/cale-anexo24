import { describe, expect, it } from 'vitest';
import { authGuard } from './core/guards/auth.guard';
import { permissionGuard } from './core/guards/permission.guard';
import { routes } from './app.routes';

describe('rutas de la aplicación', () => {
  it('protege el layout y declara la frontera RBAC de usuarios', () => {
    const layout = routes.find((route) => route.path === '');
    expect(layout?.canActivate).toEqual([authGuard]);
    const users = layout?.children?.find((route) => route.path === 'usuarios');
    expect(users).toMatchObject({
      canActivate: [permissionGuard],
      data: { permission: 'USUARIOS_ADMINISTRAR' },
    });
    expect(users?.loadChildren).toBeTypeOf('function');
  });
});
