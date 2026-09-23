import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { provideHttpClient } from '@angular/common/http';
import { TestBed } from '@angular/core/testing';
import { afterEach, beforeEach, describe, expect, it } from 'vitest';
import { UserApiService } from './user-api.service';

describe('UserApiService', () => {
  let service: UserApiService;
  let http: HttpTestingController;
  const baseUrl = '/api/v1/administracion/usuarios';

  beforeEach(() => {
    TestBed.configureTestingModule({ providers: [provideHttpClient(), provideHttpClientTesting()] });
    service = TestBed.inject(UserApiService);
    http = TestBed.inject(HttpTestingController);
  });

  afterEach(() => http.verify());

  it('consulta aplicando sólo filtros no vacíos y recortados', () => {
    service.search({ key: ' CLAVE ', name: ' ', email: ' correo@example.test ', status: 'ACTIVO', profileId: 3, page: 2, pageSize: 50 }).subscribe();

    const request = http.expectOne((candidate) => candidate.url === baseUrl);
    expect(request.request.method).toBe('GET');
    expect(request.request.params.keys().sort()).toEqual(['clave', 'correo', 'estado', 'pagina', 'perfilId', 'tamano']);
    expect(request.request.params.get('pagina')).toBe('2');
    expect(request.request.params.get('tamano')).toBe('50');
    expect(request.request.params.get('clave')).toBe('CLAVE');
    expect(request.request.params.get('correo')).toBe('correo@example.test');
    expect(request.request.params.get('estado')).toBe('ACTIVO');
    expect(request.request.params.get('perfilId')).toBe('3');
    request.flush({ items: [], total: 0, pagina: 2, tamano: 50 });
  });

  it('omite perfilId cuando no existe', () => {
    service.search({ key: '', name: '', email: '', status: null, profileId: null, page: 1, pageSize: 20 }).subscribe();
    const request = http.expectOne((candidate) => candidate.url === baseUrl);
    expect(request.request.params.has('perfilId')).toBe(false);
    request.flush({ items: [], total: 0, pagina: 1, tamano: 20 });
  });

  it('crea, obtiene y actualiza un usuario con URL y cuerpos exactos', () => {
    service.create({ clave: 'NUEVO', nombre: 'Nuevo usuario', correo: 'nuevo@example.test', password: 'ClaveSegura1!', vigencia: null, perfilId: 3 }).subscribe();
    const createRequest = http.expectOne(baseUrl);
    expect(createRequest.request.method).toBe('POST');
    expect(createRequest.request.body).toEqual({ clave: 'NUEVO', nombre: 'Nuevo usuario', correo: 'nuevo@example.test', password: 'ClaveSegura1!', vigencia: null, perfilId: 3 });
    createRequest.flush({ id: 8, clave: 'NUEVO', nombre: 'Nuevo usuario', correo: 'nuevo@example.test', estado: 'ACTIVO', vigencia: null, perfilId: 3, perfilNombre: 'Operación' });
    service.getById(7).subscribe();
    const getRequest = http.expectOne(`${baseUrl}/7`);
    expect(getRequest.request.method).toBe('GET');
    getRequest.flush({ id: 7, clave: 'OPERADOR', nombre: 'Operador', correo: 'operador@example.test', estado: 'ACTIVO', vigencia: null, perfilId: 3, perfilNombre: 'Operación' });

    service.update(7, { nombre: 'Nombre actualizado', correo: 'actualizado@example.test' }).subscribe();
    const updateRequest = http.expectOne(`${baseUrl}/7`);
    expect(updateRequest.request.method).toBe('PUT');
    expect(updateRequest.request.body).toEqual({ nombre: 'Nombre actualizado', correo: 'actualizado@example.test' });
    updateRequest.flush({ id: 7, clave: 'OPERADOR', nombre: 'Nombre actualizado', correo: 'actualizado@example.test', estado: 'ACTIVO', vigencia: null, perfilId: 3, perfilNombre: 'Operación' });
  });

  it('cambia estado, vigencia y perfil con los cuerpos exactos', () => {
    service.changeStatus(7, { estado: 'INACTIVO' }).subscribe();
    const statusRequest = http.expectOne(`${baseUrl}/7/estado`);
    expect(statusRequest.request.method).toBe('PATCH');
    expect(statusRequest.request.body).toEqual({ estado: 'INACTIVO' });
    statusRequest.flush({ id: 7, clave: 'OPERADOR', nombre: 'Operador', correo: 'operador@example.test', estado: 'INACTIVO', vigencia: null, perfilId: 3, perfilNombre: 'Operación' });

    service.changeExpiration(7, { vigencia: null }).subscribe();
    const expirationRequest = http.expectOne(`${baseUrl}/7/vigencia`);
    expect(expirationRequest.request.method).toBe('PATCH');
    expect(expirationRequest.request.body).toEqual({ vigencia: null });
    expirationRequest.flush({ id: 7, clave: 'OPERADOR', nombre: 'Operador', correo: 'operador@example.test', estado: 'INACTIVO', vigencia: null, perfilId: 3, perfilNombre: 'Operación' });

    service.changeProfile(7, { perfilId: 4 }).subscribe();
    const profileRequest = http.expectOne(`${baseUrl}/7/perfil`);
    expect(profileRequest.request.method).toBe('PATCH');
    expect(profileRequest.request.body).toEqual({ perfilId: 4 });
    profileRequest.flush({ id: 7, clave: 'OPERADOR', nombre: 'Operador', correo: 'operador@example.test', estado: 'INACTIVO', vigencia: null, perfilId: 4, perfilNombre: 'Consulta' });
  });

  it('restablece la contraseña y acepta una respuesta 204 sin cuerpo', () => {
    const password = 'ClaveTemporal1!';
    let completed = false;
    service.resetPassword(7, { password }).subscribe({ complete: () => { completed = true; } });

    const request = http.expectOne(`${baseUrl}/7/password`);
    expect(request.request.method).toBe('POST');
    expect(request.request.body).toEqual({ password });
    request.flush(null, { status: 204, statusText: 'No Content' });
    expect(completed).toBe(true);
  });
});
