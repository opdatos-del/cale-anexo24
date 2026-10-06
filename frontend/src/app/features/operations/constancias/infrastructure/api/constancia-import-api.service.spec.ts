import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { TestBed } from '@angular/core/testing';
import { provideHttpClient } from '@angular/common/http';
import { describe, expect, it } from 'vitest';
import { ConstanciaImportApiService } from './constancia-import-api.service';

describe('ConstanciaImportApiService', () => {
  function arrange() {
    TestBed.configureTestingModule({ providers: [ConstanciaImportApiService, provideHttpClient(), provideHttpClientTesting()] });
    return { api: TestBed.inject(ConstanciaImportApiService), http: TestBed.inject(HttpTestingController) };
  }

  it('envía el archivo al staging de constancias, nunca bajo /catalogos', () => {
    const { api, http } = arrange();
    api.upload(new File(['xlsx'], 'constancias.xlsx')).subscribe();
    const request = http.expectOne('/api/v1/operaciones/constancias/importaciones');
    expect(request.request.method).toBe('POST');
    expect(request.request.body).toBeInstanceOf(FormData);
    expect(request.request.url).not.toContain('/catalogos');
    request.flush({});
    http.verify();
  });

  it('consulta detalle y errores paginados del staging', () => {
    const { api, http } = arrange();
    api.get(3, 1, 50).subscribe();
    const detalle = http.expectOne((req) =>
      req.url === '/api/v1/operaciones/constancias/importaciones/3'
      && req.params.get('pagina') === '1' && req.params.get('tamano') === '50');
    expect(detalle.request.method).toBe('GET');
    detalle.flush({});

    api.errors(3, 2, 20).subscribe();
    const errores = http.expectOne((req) =>
      req.url === '/api/v1/operaciones/constancias/importaciones/3/errores'
      && req.params.get('pagina') === '2' && req.params.get('tamano') === '20');
    expect(errores.request.method).toBe('GET');
    errores.flush([]);
    http.verify();
  });

  it('confirma contra el endpoint de operación de constancias', () => {
    const { api, http } = arrange();
    let respuesta: unknown;
    api.confirm(9).subscribe((value) => (respuesta = value));
    const request = http.expectOne('/api/v1/operaciones/constancias/9/confirmacion');
    expect(request.request.method).toBe('POST');
    expect(request.request.body).toEqual({});
    const payload = { cargaId: 9, estado: 'CONFIRMADA', totalFilas: 2, filasValidas: 2, filasConError: 0, confirmadaEn: '2026-10-05T10:15:30', resultado: 'CONFIRMED' };
    request.flush(payload);
    expect(respuesta).toEqual(payload);
    http.verify();
  });
});
