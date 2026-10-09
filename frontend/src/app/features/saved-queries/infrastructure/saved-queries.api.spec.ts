import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { TestBed } from '@angular/core/testing';
import { afterEach, beforeEach, describe, expect, it } from 'vitest';
import { SavedQueriesApi } from './saved-queries.api';

describe('SavedQueriesApi', () => {
  let api: SavedQueriesApi;
  let http: HttpTestingController;
  const url = '/api/v1/consultas-guardadas';

  beforeEach(() => {
    TestBed.configureTestingModule({ providers: [provideHttpClient(), provideHttpClientTesting()] });
    api = TestBed.inject(SavedQueriesApi);
    http = TestBed.inject(HttpTestingController);
  });
  afterEach(() => http.verify());

  it('lista con alcance opcional', () => {
    api.list('ENTRADAS').subscribe();
    const request = http.expectOne(url + '?alcance=ENTRADAS');
    expect(request.request.method).toBe('GET');
    request.flush([]);
  });

  it('crea, actualiza y elimina presets por endpoints tipados', () => {
    const body = { nombre: 'Entradas del mes', descripcion: null, alcance: 'ENTRADAS' as const, criterios: { from: '2026-10-01', to: '2026-10-31' } };
    api.create(body).subscribe();
    const create = http.expectOne(url);
    expect(create.request.method).toBe('POST');
    expect(create.request.body).toEqual(body);
    create.flush({ id: 4, ...body, fechaCreacion: '2026-10-01', fechaActualizacion: '2026-10-01' });

    api.update(4, body).subscribe();
    const update = http.expectOne(url + '/4');
    expect(update.request.method).toBe('PUT');
    update.flush({ id: 4, ...body, fechaCreacion: '2026-10-01', fechaActualizacion: '2026-10-02' });

    api.delete(4).subscribe();
    const remove = http.expectOne(url + '/4');
    expect(remove.request.method).toBe('DELETE');
    remove.flush(null);
  });
});
