import { provideHttpClient } from '@angular/common/http';
import { provideHttpClientTesting, HttpTestingController } from '@angular/common/http/testing';
import { TestBed } from '@angular/core/testing';
import { describe, expect, it } from 'vitest';
import { CompanyGeneralData } from '@features/catalogs/general-data/domain/company-general-data.model';
import { CompanyGeneralDataApi } from './company-general-data.api';

describe('CompanyGeneralDataApi', () => {
  it('maneja 204 sin intentar parsear un cuerpo JSON', () => {
    TestBed.configureTestingModule({ providers: [provideHttpClient(), provideHttpClientTesting()] });
    const api = TestBed.inject(CompanyGeneralDataApi);
    const http = TestBed.inject(HttpTestingController);
    let response: CompanyGeneralData | null = null;

    api.get().subscribe((data) => { response = data; });
    const request = http.expectOne('/api/v1/catalogos/datos-generales');
    request.flush(null, { status: 204, statusText: 'No Content' });

    expect(response).toBeNull();
    http.verify();
  });

  it('consulta únicamente el endpoint read-only de datos generales', () => {
    TestBed.configureTestingModule({ providers: [provideHttpClient(), provideHttpClientTesting()] });
    const api = TestBed.inject(CompanyGeneralDataApi);
    const http = TestBed.inject(HttpTestingController);
    let response: CompanyGeneralData | null = null;

    api.get().subscribe((data) => { response = data; });
    const request = http.expectOne('/api/v1/catalogos/datos-generales');
    expect(request.request.method).toBe('GET');
    request.flush({ razonSocial: 'Empresa', rfc: 'RFC', registroImmex: 'IMMEX', domicilioFiscal: 'Domicilio' });

    expect(response).toEqual({ razonSocial: 'Empresa', rfc: 'RFC', registroImmex: 'IMMEX', domicilioFiscal: 'Domicilio' });
    http.verify();
  });
});
