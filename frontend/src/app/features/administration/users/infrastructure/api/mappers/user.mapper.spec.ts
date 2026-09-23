import { describe, expect, it } from 'vitest';
import { UserMapper } from './user.mapper';

describe('UserMapper', () => {
  it('mapea un usuario del contrato HTTP al dominio', () => {
    const user = UserMapper.toDomain({
      id: 7,
      clave: 'OPERADOR',
      nombre: 'Usuario de prueba',
      correo: 'operador@example.test',
      estado: 'ACTIVO',
      vigencia: null,
      perfilId: 3,
      perfilNombre: 'Operación',
    });

    expect(user).toEqual({
      id: 7,
      key: 'OPERADOR',
      name: 'Usuario de prueba',
      email: 'operador@example.test',
      status: 'ACTIVO',
      expiration: null,
      profileId: 3,
      profileName: 'Operación',
    });
  });

  it('mapea páginas y conserva la vigencia definida', () => {
    expect(UserMapper.toPage({
      items: [{
        id: 8,
        clave: 'SUPERVISOR',
        nombre: 'Supervisora',
        correo: 'supervisora@example.test',
        estado: 'INACTIVO',
        vigencia: '2030-12-31',
        perfilId: 4,
        perfilNombre: 'Supervisión',
      }],
      total: 41,
      pagina: 3,
      tamano: 20,
    })).toEqual({
      items: [{
        id: 8,
        key: 'SUPERVISOR',
        name: 'Supervisora',
        email: 'supervisora@example.test',
        status: 'INACTIVO',
        expiration: '2030-12-31',
        profileId: 4,
        profileName: 'Supervisión',
      }],
      total: 41,
      page: 3,
      pageSize: 20,
    });
  });

  it('traduce los comandos de modificación al contrato HTTP', () => {
    expect(UserMapper.toUpdateRequest({ name: ' Nombre ', email: ' correo@example.test ' })).toEqual({
      nombre: ' Nombre ',
      correo: ' correo@example.test ',
    });
    expect(UserMapper.toStatusRequest({ status: 'INACTIVO' })).toEqual({ estado: 'INACTIVO' });
    expect(UserMapper.toExpirationRequest({ expiration: null })).toEqual({ vigencia: null });
    expect(UserMapper.toPasswordRequest({ password: 'valor-no-expuesto' })).toEqual({ password: 'valor-no-expuesto' });
    expect(UserMapper.toCreateRequest({ key: 'NUEVO', name: 'Nuevo', email: 'nuevo@example.test', password: 'ClaveSegura1!', expiration: null, profileId: 3 })).toEqual({
      clave: 'NUEVO', nombre: 'Nuevo', correo: 'nuevo@example.test', password: 'ClaveSegura1!', vigencia: null, perfilId: 3,
    });
    expect(UserMapper.toProfileRequest({ profileId: 4 })).toEqual({ perfilId: 4 });
  });
});
