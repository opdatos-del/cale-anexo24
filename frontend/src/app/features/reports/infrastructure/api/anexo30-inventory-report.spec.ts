import { TestBed } from '@angular/core/testing';
import { provideHttpClient } from '@angular/common/http';
import { provideHttpClientTesting, HttpTestingController } from '@angular/common/http/testing';
import { describe, expect, it } from 'vitest';
import { ReportApiService } from './report-api.service';

describe('ReportApiService inventario inicial Anexo 30', () => {
  it('consulta snapshot con filtro opcional y paginación, sin periodo', () => {
    TestBed.configureTestingModule({ providers: [ReportApiService, provideHttpClient(), provideHttpClientTesting()] });
    const service = TestBed.inject(ReportApiService);
    const http = TestBed.inject(HttpTestingController);
    service.search({ type: 'anexo30-revision-inventario-inicial', from: '', to: '', page: 2, pageSize: 50, customsDocument: '', customsCode: '', tariffFraction: '', partNumber: '', material: '', product: '', userId: null, module: '', result: '', correlationId: '', filter: 'F4' }).subscribe();
    const request = http.expectOne('/api/v1/reportes/anexo30-revision-inventario-inicial?pagina=2&tamano=50&filtro=F4');
    expect(request.request.method).toBe('GET');
    request.flush({ items: [], total: 0, pagina: 2, tamano: 50 });
    http.verify();
  });
});
