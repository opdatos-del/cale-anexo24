import { describe, expect, it } from 'vitest';
import { authGuard } from './core/guards/auth.guard';
import { permissionGuard } from './core/guards/permission.guard';
import { routes } from './app.routes';

describe('rutas de la aplicación', () => {
  it('protege el layout y declara autoridades exactas de Usuarios y Perfiles', () => {
    const layout = routes.find((route) => route.path === '');
    expect(layout?.canActivate).toEqual([authGuard]);
    const users = layout?.children?.find((route) => route.path === 'usuarios');
    expect(users).toMatchObject({
      canActivate: [permissionGuard],
      data: { permission: 'USUARIOS_ADMINISTRAR' },
    });
    expect(users?.loadChildren).toBeTypeOf('function');
    const profiles = layout?.children?.find((route) => route.path === 'perfiles');
    expect(profiles).toMatchObject({
      canActivate: [permissionGuard],
      data: { permission: 'PERFILES_ADMINISTRAR' },
    });
    expect(profiles?.loadChildren).toBeTypeOf('function');
  });

  it('declara Facturación con la autoridad exacta de carga', () => {
    const layout = routes.find((route) => route.path === '');
    const billing = layout?.children?.find((route) => route.path === 'facturacion');
    expect(billing).toMatchObject({ canActivate: [permissionGuard], data: { permission: 'FACTURACION_CARGAR' } });
    expect(billing?.loadChildren).toBeTypeOf('function');
  });

  it('declara Reportes con la autoridad de generación', () => {
    const layout = routes.find((route) => route.path === '');
    const reports = layout?.children?.find((route) => route.path === 'reportes');
    expect(reports).toMatchObject({
      canActivate: [permissionGuard],
      data: { permission: 'REPORTES_GENERAR' },
    });
    expect(reports?.loadChildren).toBeTypeOf('function');
  });
});
