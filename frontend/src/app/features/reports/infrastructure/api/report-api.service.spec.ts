import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { TestBed } from '@angular/core/testing';
import { afterEach, beforeEach, describe, expect, it } from 'vitest';
import { ReportSearchCriteria } from '../../domain/models/report.model';
import { ReportApiService } from './report-api.service';

const criteria: ReportSearchCriteria = {
  type: 'entradas', from: '2026-01-01', to: '2026-01-31', page: 2, pageSize: 20,
  customsDocument: '  26  ', customsCode: ' A1 ', tariffFraction: ' 1234 ', partNumber: ' P-1 ',
  material: '', product: '', userId: null, module: '', result: '', correlationId: '',
};

describe('ReportApiService', () => {
  let service: ReportApiService;
  let http: HttpTestingController;

  beforeEach(() => {
    TestBed.configureTestingModule({ providers: [provideHttpClient(), provideHttpClientTesting()] });
    service = TestBed.inject(ReportApiService);
    http = TestBed.inject(HttpTestingController);
  });

  afterEach(() => http.verify());

  it('consulta entradas con el contrato y filtros aplicables', () => {
    service.search(criteria).subscribe();
    const request = http.expectOne((candidate) => candidate.url === '/api/v1/reportes/entradas');
    expect(request.request.params.keys().sort()).toEqual(['clavePedimento', 'desde', 'fraccion', 'hasta', 'numeroParte', 'pagina', 'pedimento', 'tamano']);
    expect(request.request.params.get('pedimento')).toBe('26');
    expect(request.request.params.get('pagina')).toBe('2');
    request.flush({ items: [], total: 0, pagina: 2, tamano: 20 });
  });

  it('usa filtros específicos y exportación XLSX para materiales utilizados', () => {
    service.export({ ...criteria, type: 'materiales-utilizados', material: ' MAT ', product: ' PROD ', customsDocument: ' S-1 ', customsCode: ' RT ' }).subscribe();
    const request = http.expectOne((candidate) => candidate.url === '/api/v1/reportes/materiales-utilizados/exportacion');
    expect(request.request.responseType).toBe('blob');
    expect(request.request.params.get('material')).toBe('MAT');
    expect(request.request.params.get('pedimentoSalida')).toBe('S-1');
    expect(request.request.params.get('tamano')).toBe('100');
    request.flush(new Blob());
  });

  it('convierte el periodo completo de bitácora a instantes ISO', () => {
    service.search({ ...criteria, type: 'bitacora', module: 'SEGURIDAD', result: 'EXITO' }).subscribe();
    const request = http.expectOne((candidate) => candidate.url === '/api/v1/reportes/bitacora');
    expect(request.request.params.get('desde')).toMatch(/T\d{2}:\d{2}:\d{2}\.\d{3}Z$/);
    expect(request.request.params.get('hasta')).toMatch(/T\d{2}:\d{2}:\d{2}\.\d{3}Z$/);
    expect(request.request.params.get('modulo')).toBe('SEGURIDAD');
    request.flush({ items: [], total: 0, pagina: 2, tamano: 20 });
  });
});
