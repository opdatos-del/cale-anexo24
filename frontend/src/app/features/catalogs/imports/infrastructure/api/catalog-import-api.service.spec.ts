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
});
