import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { TestBed } from '@angular/core/testing';
import { afterEach, beforeEach, describe, expect, it } from 'vitest';
import { ProfileApiService } from './profile-api.service';

describe('ProfileApiService', () => {
  let service: ProfileApiService;
  let http: HttpTestingController;
  const baseUrl = '/api/v1/administracion/perfiles';

  beforeEach(() => {
    TestBed.configureTestingModule({ providers: [provideHttpClient(), provideHttpClientTesting()] });
    service = TestBed.inject(ProfileApiService);
    http = TestBed.inject(HttpTestingController);
  });

  afterEach(() => http.verify());

  it('consulta con filtros no vacíos, recortados y parámetros paginados en español', () => {
    service.search({ name: ' Operación ', status: 'ACTIVO', page: 2, pageSize: 50 }).subscribe();

    const request = http.expectOne((candidate) => candidate.url === baseUrl);
    expect(request.request.method).toBe('GET');
    expect(request.request.params.keys().sort()).toEqual(['estado', 'nombre', 'pagina', 'tamano']);
    expect(request.request.params.get('nombre')).toBe('Operación');
    expect(request.request.params.get('estado')).toBe('ACTIVO');
    expect(request.request.params.get('pagina')).toBe('2');
    expect(request.request.params.get('tamano')).toBe('50');
    request.flush({ items: [], total: 0, pagina: 2, tamano: 50 });
  });

  it('omite filtros vacíos', () => {
    service.search({ name: ' ', status: null, page: 1, pageSize: 100 }).subscribe();

    const request = http.expectOne((candidate) => candidate.url === baseUrl);
    expect(request.request.params.keys().sort()).toEqual(['pagina', 'tamano']);
    request.flush({ items: [], total: 0, pagina: 1, tamano: 100 });
  });

  it('consulta catálogo de actividades y permisos por perfil', () => {
    service.listActivities().subscribe();
    const activities = http.expectOne('/api/v1/administracion/actividades');
    expect(activities.request.method).toBe('GET');
    activities.flush([{ id: 8, clave: 'X', nombre: 'Actividad', recurso: 'r', accion: 'A' }]);

    service.getPermissions(12).subscribe();
    const permissions = http.expectOne(`${baseUrl}/12/permisos`);
    expect(permissions.request.method).toBe('GET');
    permissions.flush({ perfilId: 12, permisos: [] });
  });

  it('envía contratos exactos de crear, renombrar, estado y reemplazo completo', () => {
    const profile = { id: 12, nombre: 'Operaciones', estado: 'ACTIVO', cantidadPermisos: 0 };
    service.create('Operaciones').subscribe();
    const create = http.expectOne(baseUrl);
    expect(create.request.method).toBe('POST');
    expect(create.request.body).toEqual({ nombre: 'Operaciones' });
    create.flush(profile, { status: 201, statusText: 'Created' });

    service.updateName(12, 'Operaciones 2').subscribe();
    const rename = http.expectOne(`${baseUrl}/12`);
    expect(rename.request.method).toBe('PUT');
    expect(rename.request.body).toEqual({ nombre: 'Operaciones 2' });
    rename.flush({ ...profile, nombre: 'Operaciones 2' });

    service.changeStatus(12, 'INACTIVO').subscribe();
    const status = http.expectOne(`${baseUrl}/12/estado`);
    expect(status.request.method).toBe('PATCH');
    expect(status.request.body).toEqual({ estado: 'INACTIVO' });
    status.flush({ ...profile, estado: 'INACTIVO' });

    service.replacePermissions(12, [9, 2]).subscribe();
    const replace = http.expectOne(`${baseUrl}/12/permisos`);
    expect(replace.request.method).toBe('PUT');
    expect(replace.request.body).toEqual({ actividadIds: [9, 2] });
    replace.flush({ perfilId: 12, permisos: [] });
  });
});
