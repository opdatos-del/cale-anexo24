import { describe, expect, it } from 'vitest';
import { ProfileMapper } from './profile.mapper';

describe('ProfileMapper', () => {
  it('mapea un perfil del contrato HTTP al dominio', () => {
    expect(ProfileMapper.toDomain({
      id: 3,
      nombre: 'Operación',
      estado: 'ACTIVO',
      cantidadPermisos: 12,
    })).toEqual({
      id: 3,
      name: 'Operación',
      status: 'ACTIVO',
      permissionCount: 12,
    });
  });

  it('mapea páginas y conserva sus metadatos', () => {
    expect(ProfileMapper.toPage({
      items: [{ id: 4, nombre: 'Supervisión', estado: 'INACTIVO', cantidadPermisos: 8 }],
      total: 41,
      pagina: 3,
      tamano: 20,
    })).toEqual({
      items: [{ id: 4, name: 'Supervisión', status: 'INACTIVO', permissionCount: 8 }],
      total: 41,
      page: 3,
      pageSize: 20,
    });
  });
});
