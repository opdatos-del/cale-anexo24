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
});
