import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { TestBed } from '@angular/core/testing';
import { provideHttpClient } from '@angular/common/http';
import { describe, expect, it } from 'vitest';
import { PedimentApiService } from './pediment-api.service';

describe('PedimentApiService', () => {
  it('envía el archivo al endpoint de staging sin ruta de confirmación', () => {
    TestBed.configureTestingModule({ providers: [PedimentApiService, provideHttpClient(), provideHttpClientTesting()] });
    const service = TestBed.inject(PedimentApiService);
    const http = TestBed.inject(HttpTestingController);
    const file = new File(['xlsx'], 'pedimentos.xlsx');
    service.upload(file).subscribe();
    const request = http.expectOne('/api/v1/operaciones/pedimentos/cargas');
    expect(request.request.method).toBe('POST');
    expect(request.request.body).toBeInstanceOf(FormData);
    expect(request.request.url).not.toContain('confirmar');
    request.flush({});
    http.verify();
  });
});
