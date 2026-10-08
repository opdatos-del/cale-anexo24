import { describe, expect, it } from 'vitest';
import { NAVIGATION_GROUPS } from './navigation.config';

describe('navigation.config', () => {
  it('ubica Actas en Operaciones y no en Catálogos', () => {
    const operaciones = NAVIGATION_GROUPS.find((group) => group.label === 'Operaciones');
    const actas = operaciones?.items.find((item) => item.label === 'Actas');
    expect(actas).toMatchObject({ route: '/operaciones/actas', permission: 'ACTAS_CARGAR' });

    const catalogos = NAVIGATION_GROUPS.find((group) => group.label === 'Catálogos');
    expect(catalogos?.items.some((item) => item.route === '/operaciones/actas')).toBe(false);
  });

  it('ubica Constancias en Operaciones y no en Catálogos', () => {
    const operaciones = NAVIGATION_GROUPS.find((group) => group.label === 'Operaciones');
    const constancias = operaciones?.items.find((item) => item.label === 'Constancias');
    expect(constancias).toMatchObject({ route: '/operaciones/constancias', permission: 'CONSTANCIAS_CARGAR' });

    const catalogos = NAVIGATION_GROUPS.find((group) => group.label === 'Catálogos');
    expect(catalogos?.items.some((item) => item.route === '/operaciones/constancias')).toBe(false);
  });

  it('muestra importaciones para cualquier permiso de carga de catálogo', () => {
    const interfaces = NAVIGATION_GROUPS.find((group) => group.label === 'Interfaces');
    const importaciones = interfaces?.items.find((item) => item.label === 'Importaciones de catálogos');
    expect(importaciones).toMatchObject({
      route: '/catalogos/importaciones',
      anyOfPermissions: ['MATERIALES_CARGAR', 'PRODUCTOS_CARGAR', 'CLIENTES_CARGAR', 'PROVEEDORES_CARGAR', 'AGENTES_CARGAR'],
    });
  });
});
