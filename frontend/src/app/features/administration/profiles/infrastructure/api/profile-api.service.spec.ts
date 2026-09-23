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
});
