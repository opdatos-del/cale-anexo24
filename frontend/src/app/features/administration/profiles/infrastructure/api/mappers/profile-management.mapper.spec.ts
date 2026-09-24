import { describe, expect, it } from 'vitest';
import { ProfileManagementMapper } from './profile-management.mapper';

describe('ProfileManagementMapper', () => {
  it('traduce campos HTTP de actividades y conserva sus IDs', () => {
    expect(ProfileManagementMapper.toActivity({ id: 5, clave: 'CLAVE', nombre: 'Nombre', recurso: 'recurso', accion: 'LEER' })).toEqual({
      id: 5, key: 'CLAVE', name: 'Nombre', resource: 'recurso', action: 'LEER',
    });
  });

  it('mapea permisos vacíos sin confundir perfil existente', () => {
    expect(ProfileManagementMapper.toPermissions({ perfilId: 20, permisos: [] })).toEqual({ profileId: 20, permissions: [] });
  });
});
