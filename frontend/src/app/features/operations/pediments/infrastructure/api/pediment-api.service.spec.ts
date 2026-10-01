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

  it('confirma la carga contra el endpoint de confirmación', () => {
    TestBed.configureTestingModule({ providers: [PedimentApiService, provideHttpClient(), provideHttpClientTesting()] });
    const service = TestBed.inject(PedimentApiService);
    const http = TestBed.inject(HttpTestingController);
    let respuesta: unknown;
    service.confirm(7).subscribe((value) => (respuesta = value));
    const request = http.expectOne('/api/v1/operaciones/pedimentos/cargas/7/confirmacion');
    expect(request.request.method).toBe('POST');
    expect(request.request.body).toEqual({});
    const payload = {
      cargaId: 7,
      estado: 'CONFIRMADA',
      resultado: 'CONFIRMED',
      tipoOperacion: 1,
      operacionesProcesadas: 1,
      partidasProcesadas: 2,
      fechaConfirmacion: '2026-05-28T10:15:30',
    };
    request.flush(payload);
    expect(respuesta).toEqual(payload);
    http.verify();
  });
});
