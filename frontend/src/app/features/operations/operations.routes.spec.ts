import { describe, expect, it } from 'vitest';
import { permissionGuard } from '@core/guards/permission.guard';
import { OPERATIONS_ROUTES } from './operations.routes';

describe('rutas de operaciones', () => {
  it('declara actas bajo operaciones con ACTAS_CARGAR', () => {
    const actas = OPERATIONS_ROUTES.find((route) => route.path === 'actas');
    expect(actas).toMatchObject({ canActivate: [permissionGuard], data: { permission: 'ACTAS_CARGAR' } });
    expect(actas?.loadChildren).toBeTypeOf('function');
  });

  it('no expone actas bajo catálogos', () => {
    expect(OPERATIONS_ROUTES.some((route) => (route.path ?? '').startsWith('catalogos'))).toBe(false);
  });
});
