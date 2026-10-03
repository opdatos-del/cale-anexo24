import { TestBed } from '@angular/core/testing';
import { ActivatedRouteSnapshot, Router } from '@angular/router';
import { describe, expect, it, vi } from 'vitest';
import { AuthService } from '@core/auth/auth.service';
import { permissionGuard } from './permission.guard';

const FORBIDDEN = { redirect: '/forbidden' };

function arrange(granted: string[]) {
  const parseUrl = vi.fn(() => FORBIDDEN);
  TestBed.configureTestingModule({
    providers: [
      {
        provide: AuthService,
        useValue: {
          hasPermission: (permission: string) => granted.includes(permission),
          hasAnyPermission: (...permissions: string[]) => permissions.some((permission) => granted.includes(permission)),
        },
      },
      { provide: Router, useValue: { parseUrl } },
    ],
  });
  const run = (data: Record<string, unknown>) => TestBed.runInInjectionContext(
    () => permissionGuard({ data } as unknown as ActivatedRouteSnapshot, {} as never),
  );
  return { run, parseUrl };
}

describe('permissionGuard', () => {
  it('permite una ruta con permission único cuando el usuario lo tiene', () => {
    const { run } = arrange(['MATERIALES_CONSULTAR']);
    expect(run({ permission: 'MATERIALES_CONSULTAR' })).toBe(true);
  });

  it('redirige a /forbidden con permission único cuando el usuario no lo tiene', () => {
    const { run, parseUrl } = arrange([]);
    expect(run({ permission: 'MATERIALES_CONSULTAR' })).toBe(FORBIDDEN);
    expect(parseUrl).toHaveBeenCalledWith('/forbidden');
  });

  it('permite una ruta con permissions cuando el usuario tiene el primero', () => {
    const { run } = arrange(['MATERIALES_CARGAR']);
    expect(run({ permissions: ['MATERIALES_CARGAR', 'PRODUCTOS_CARGAR'] })).toBe(true);
  });

  it('permite una ruta con permissions cuando el usuario tiene el segundo', () => {
    const { run } = arrange(['PRODUCTOS_CARGAR']);
    expect(run({ permissions: ['MATERIALES_CARGAR', 'PRODUCTOS_CARGAR'] })).toBe(true);
  });

  it('redirige a /forbidden con permissions cuando el usuario no tiene ninguno', () => {
    const { run, parseUrl } = arrange(['CATALOGOS_AUX_CONSULTAR']);
    expect(run({ permissions: ['MATERIALES_CARGAR', 'PRODUCTOS_CARGAR'] })).toBe(FORBIDDEN);
    expect(parseUrl).toHaveBeenCalledWith('/forbidden');
  });

  it('ignora el permission heredado cuando la ruta hija declara permissions (caso FUN-E2E-003)', () => {
    const { run, parseUrl } = arrange(['CATALOGOS_AUX_CONSULTAR']);
    const data = {
      permission: 'CATALOGOS_AUX_CONSULTAR',
      permissions: ['MATERIALES_CARGAR', 'PRODUCTOS_CARGAR'],
    };
    expect(run(data)).toBe(FORBIDDEN);
    expect(parseUrl).toHaveBeenCalledWith('/forbidden');
  });

  it('permite el caso heredado cuando el usuario tiene MATERIALES_CARGAR', () => {
    const { run } = arrange(['CATALOGOS_AUX_CONSULTAR', 'MATERIALES_CARGAR']);
    expect(run({
      permission: 'CATALOGOS_AUX_CONSULTAR',
      permissions: ['MATERIALES_CARGAR', 'PRODUCTOS_CARGAR'],
    })).toBe(true);
  });

  it('permite el caso heredado cuando el usuario tiene PRODUCTOS_CARGAR', () => {
    const { run } = arrange(['CATALOGOS_AUX_CONSULTAR', 'PRODUCTOS_CARGAR']);
    expect(run({
      permission: 'CATALOGOS_AUX_CONSULTAR',
      permissions: ['MATERIALES_CARGAR', 'PRODUCTOS_CARGAR'],
    })).toBe(true);
  });

  it('redirige a /forbidden cuando la ruta no declara permisos', () => {
    const { run, parseUrl } = arrange(['MATERIALES_CONSULTAR']);
    expect(run({})).toBe(FORBIDDEN);
    expect(parseUrl).toHaveBeenCalledWith('/forbidden');
  });
});
