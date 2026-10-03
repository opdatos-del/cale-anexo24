import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { TestBed } from '@angular/core/testing';
import { describe, expect, it } from 'vitest';
import { CatalogImportApiService } from './catalog-import-api.service';

describe('CatalogImportApiService', () => {
  it('usa los endpoints separados y conserva el multipart', () => {
    TestBed.configureTestingModule({ providers: [CatalogImportApiService, provideHttpClient(), provideHttpClientTesting()] });
    const service = TestBed.inject(CatalogImportApiService);
    const http = TestBed.inject(HttpTestingController);
    const file = new File(['xlsx'], 'productos.xlsx');

    service.upload('PRODUCTO', file).subscribe();
    const request = http.expectOne('/api/v1/catalogos/importaciones/productos');
    expect(request.request.method).toBe('POST');
    expect(request.request.body.get('archivo')).toBe(file);
    request.flush({});
    http.verify();
  });

  it('confirma una carga de materiales en el endpoint específico', () => {
    TestBed.configureTestingModule({ providers: [CatalogImportApiService, provideHttpClient(), provideHttpClientTesting()] });
    const service = TestBed.inject(CatalogImportApiService);
    const http = TestBed.inject(HttpTestingController);

    service.confirmMaterial(7).subscribe();
    const request = http.expectOne('/api/v1/catalogos/importaciones/materiales/7/confirmacion');
    expect(request.request.method).toBe('POST');
    expect(request.request.body).toEqual({});
    request.flush({ cargaId: 7, estado: 'CONFIRMADA', totalFilas: 1, filasValidas: 1, filasConError: 0, confirmadaEn: '2026-10-03T12:00:00' });
    http.verify();
  });

  it('confirma una carga de productos en el endpoint específico', () => {
    TestBed.configureTestingModule({ providers: [CatalogImportApiService, provideHttpClient(), provideHttpClientTesting()] });
    const service = TestBed.inject(CatalogImportApiService);
    const http = TestBed.inject(HttpTestingController);

    service.confirmProduct(11).subscribe();
    const request = http.expectOne('/api/v1/catalogos/importaciones/productos/11/confirmacion');
    expect(request.request.method).toBe('POST');
    expect(request.request.body).toEqual({});
    request.flush({ cargaId: 11, estado: 'CONFIRMADA', totalFilas: 1, filasValidas: 1, filasConError: 0, confirmadaEn: '2026-10-03T12:00:00' });
    http.verify();
  });

  it('confirma una carga de clientes en el endpoint específico', () => {
    TestBed.configureTestingModule({ providers: [CatalogImportApiService, provideHttpClient(), provideHttpClientTesting()] });
    const service = TestBed.inject(CatalogImportApiService);
    const http = TestBed.inject(HttpTestingController);

    service.confirmClient(13).subscribe();
    const request = http.expectOne('/api/v1/catalogos/importaciones/clientes/13/confirmacion');
    expect(request.request.method).toBe('POST');
    expect(request.request.body).toEqual({});
    request.flush({ cargaId: 13, estado: 'CONFIRMADA', totalFilas: 1, filasValidas: 1, filasConError: 0, confirmadaEn: '2026-10-03T12:00:00' });
    http.verify();
  });

  it('sube clientes al endpoint de clientes', () => {
    TestBed.configureTestingModule({ providers: [CatalogImportApiService, provideHttpClient(), provideHttpClientTesting()] });
    const service = TestBed.inject(CatalogImportApiService);
    const http = TestBed.inject(HttpTestingController);
    const file = new File(['xlsx'], 'clientes.xlsx');

    service.upload('CLIENTE', file).subscribe();
    const request = http.expectOne('/api/v1/catalogos/importaciones/clientes');
    expect(request.request.method).toBe('POST');
    expect(request.request.body.get('archivo')).toBe(file);
    request.flush({});
    http.verify();
  });

  it('sube proveedores al endpoint de proveedores', () => {
    TestBed.configureTestingModule({ providers: [CatalogImportApiService, provideHttpClient(), provideHttpClientTesting()] });
    const service = TestBed.inject(CatalogImportApiService);
    const http = TestBed.inject(HttpTestingController);
    const file = new File(['xlsx'], 'proveedores.xlsx');

    service.upload('PROVEEDOR', file).subscribe();
    const request = http.expectOne('/api/v1/catalogos/importaciones/proveedores');
    expect(request.request.method).toBe('POST');
    expect(request.request.body.get('archivo')).toBe(file);
    request.flush({});
    http.verify();
  });

  it('confirma una carga de proveedores en el endpoint específico', () => {
    TestBed.configureTestingModule({ providers: [CatalogImportApiService, provideHttpClient(), provideHttpClientTesting()] });
    const service = TestBed.inject(CatalogImportApiService);
    const http = TestBed.inject(HttpTestingController);

    service.confirmProvider(17).subscribe();
    const request = http.expectOne('/api/v1/catalogos/importaciones/proveedores/17/confirmacion');
    expect(request.request.method).toBe('POST');
    expect(request.request.body).toEqual({});
    request.flush({ cargaId: 17, estado: 'CONFIRMADA', totalFilas: 1, filasValidas: 1, filasConError: 0, confirmadaEn: '2026-10-03T12:00:00' });
    http.verify();
  });
});