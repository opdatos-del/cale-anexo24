import { TestBed } from '@angular/core/testing';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { provideHttpClient } from '@angular/common/http';
import { afterEach, beforeEach, describe, expect, it } from 'vitest';
import { BillingApiService } from './billing-api.service';

describe('BillingApiService', () => {
  let service: BillingApiService;
  let http: HttpTestingController;

  beforeEach(() => {
    TestBed.configureTestingModule({ providers: [provideHttpClient(), provideHttpClientTesting()] });
    service = TestBed.inject(BillingApiService);
    http = TestBed.inject(HttpTestingController);
  });

  afterEach(() => http.verify());

  it('envía cada archivo en el campo repetido archivos al endpoint V1', () => {
    const files = [new File(['a'], 'uno.xlsx'), new File(['b'], 'dos.xls')];
    service.upload(files).subscribe();
    const request = http.expectOne('/api/v1/facturacion/cargas');
    expect(request.request.method).toBe('POST');
    expect(request.request.body).toBeInstanceOf(FormData);
    const form = request.request.body as FormData;
    expect(form.getAll('archivos')).toEqual(files);
    expect(request.request.headers.has('Content-Type')).toBe(false);
    request.flush({ cargas: [], correlationId: 'test', plantilla: 'FACTURACION:LEGACY-2026-09', confirmacionDisponible: false });
  });

  it('carga detalle tipado con pagina y tamano', () => {
    service.load(9, 2, 50).subscribe((detail) => expect(detail.preview.pagina).toBe(2));
    const request = http.expectOne('/api/v1/facturacion/cargas/9?pagina=2&tamano=50');
    expect(request.request.method).toBe('GET');
    request.flush({ id: 9, archivo: 'uno.xlsx', hash: 'hash', estado: 'PREVISUALIZADA', totalRegistros: 101, registrosValidos: 101, registrosInvalidos: 0, preview: { filas: [], pagina: 2, tamano: 50 }, errores: [] });
  });
});
