import { TestBed } from '@angular/core/testing';
import { HttpErrorResponse } from '@angular/common/http';
import { ActivatedRoute, Router, convertToParamMap } from '@angular/router';
import { describe, expect, it, vi } from 'vitest';
import { of, throwError } from 'rxjs';
import { BillingApiService } from '@features/billing/infrastructure/api/billing-api.service';
import { BillingHistoryComponent } from './billing-history.component';

const PAGE = {
  items: [
    {
      id: 101,
      archivo: 'a.xlsx',
      hash: 'hash-a',
      fecha: '2026-10-10T12:00:00',
      estado: 'PREVISUALIZADA' as const,
      totalRegistros: 2,
      registrosValidos: 1,
      registrosInvalidos: 1,
    },
  ],
  total: 21,
  pagina: 1,
  tamano: 20,
};
const DETAIL = {
  id: 101,
  archivo: 'a.xlsx',
  hash: 'hash-a',
  estado: 'PREVISUALIZADA' as const,
  totalRegistros: 2,
  registrosValidos: 1,
  registrosInvalidos: 1,
  preview: {
    filas: [
      { Documento: 'DOC-A', Importe: '12.50' },
      { Documento: 'DOC-B', Importe: null },
    ],
    pagina: 1,
    tamano: 100,
  },
  errores: [
    {
      hoja: 'FACTURAS',
      fila: 2,
      columna: 'Documento',
      valorEnmascarado: null,
      codigo: 'ERR-1',
      mensaje: 'Fila inválida',
    },
  ],
};

function configure(query: Record<string, string> = {}) {
  const api = { history: vi.fn(() => of(PAGE)), load: vi.fn(() => of(DETAIL)) };
  const router = { navigate: vi.fn(() => Promise.resolve(true)) };
  TestBed.configureTestingModule({
    imports: [BillingHistoryComponent],
    providers: [
      { provide: BillingApiService, useValue: api },
      {
        provide: ActivatedRoute,
        useValue: { snapshot: { queryParamMap: convertToParamMap(query) } },
      },
      { provide: Router, useValue: router },
    ],
  });
  const fixture = TestBed.createComponent(BillingHistoryComponent);
  fixture.detectChanges();
  return { fixture, api };
}

describe('BillingHistoryComponent', () => {
  it('recupera deep-link y muestra métricas, filas y errores persistidos', () => {
    const { fixture, api } = configure({ carga: '101' });

    expect(api.load).toHaveBeenCalledWith(101, 1, 100);
    expect(fixture.nativeElement.textContent).toContain('DOC-A');
    expect(fixture.nativeElement.textContent).toContain('12.50');
    expect(fixture.nativeElement.textContent).toContain('Estado persistido');
    expect(fixture.nativeElement.textContent).toContain('Total');
    expect(fixture.nativeElement.textContent).toContain('Fila inválida');
  });

  it('envía página y tamaño elegidos por paginator de historial', () => {
    const { fixture, api } = configure();
    const component = fixture.componentInstance as unknown as {
      changePage(event: { pageIndex: number; pageSize: number }): void;
    };

    component.changePage({ pageIndex: 1, pageSize: 50 });

    expect(api.history).toHaveBeenLastCalledWith(null, null, null, 2, 50);
  });

  it('muestra error controlado cuando detalle no pertenece al usuario', () => {
    const { fixture, api } = configure({ carga: '101' });
    api.load.mockReturnValue(throwError(() => ({ status: 404 })));
    const component = fixture.componentInstance as unknown as { review(id: number, updateUrl?: boolean): void };

    component.review(101, false);
    fixture.detectChanges();

    expect(fixture.nativeElement.querySelector('[role="alert"]')?.textContent).toContain('No fue posible encontrar esta carga');
  });



  it('muestra mensaje neutral para HTTP 404 sin cuerpo', () => {
    const { fixture, api } = configure({ carga: '101' });
    api.load.mockReturnValue(throwError(() => new HttpErrorResponse({ status: 404 })));
    (fixture.componentInstance as unknown as { review(id: number, updateUrl?: boolean): void }).review(101, false);
    fixture.detectChanges();
    expect(fixture.nativeElement.querySelector('[role="alert"]')?.textContent).toContain('No fue posible encontrar esta carga.');
  });

  it('conserva correlation ID seguro sin revelar propiedad en HTTP 404', () => {
    const { fixture, api } = configure({ carga: '101' });
    api.load.mockReturnValue(throwError(() => new HttpErrorResponse({ status: 404, error: { correlationId: 'e2e-404' } })));
    (fixture.componentInstance as unknown as { review(id: number, updateUrl?: boolean): void }).review(101, false);
    fixture.detectChanges();
    const message = fixture.nativeElement.querySelector('[role="alert"]')?.textContent;
    expect(message).toContain('No fue posible encontrar esta carga. Referencia: e2e-404');
    expect(message).not.toMatch(/propiedad|otro usuario|pertenece/i);
  });

  it('conserva traducción actual para errores distintos de 404', () => {
    const { fixture, api } = configure({ carga: '101' });
    api.load.mockReturnValue(throwError(() => new HttpErrorResponse({ status: 503, error: { correlationId: 'e2e-503' } })));
    (fixture.componentInstance as unknown as { review(id: number, updateUrl?: boolean): void }).review(101, false);
    fixture.detectChanges();
    expect(fixture.nativeElement.querySelector('[role="alert"]')?.textContent).toContain('El servicio no está disponible. Referencia: e2e-503');
  });
});
