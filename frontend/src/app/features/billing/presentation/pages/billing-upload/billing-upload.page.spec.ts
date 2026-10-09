import { HttpErrorResponse } from '@angular/common/http';
import { TestBed } from '@angular/core/testing';
import { describe, expect, it, vi } from 'vitest';
import { of, throwError } from 'rxjs';
import { appConfig } from '@app/app.config';
import { UploadBillingFilesUseCase } from '@features/billing/application/use-cases/upload-billing-files.use-case';
import { BillingRepository } from '@features/billing/domain/repositories/billing.repository';
import { BillingPersistedLoadStatus, BillingTemplate } from '@features/billing/domain/models/billing-upload.model';
import { BillingApiService } from '@features/billing/infrastructure/api/billing-api.service';
import { HttpBillingRepository } from '@features/billing/infrastructure/repositories/http-billing.repository';
import { BillingUploadPage } from './billing-upload.page';

const TEMPLATE: BillingTemplate = {
  nombre: 'FACTURACION',
  version: 'LEGACY-2026-09',
  hoja: 'FACTURAS',
  columnas: [{ nombre: 'Documento', obligatoria: true, tipo: 'TEXTO' }],
};

function configurePage(api: Partial<BillingApiService> = {}) {
  const billingApi = {
    template: vi.fn(() => of(TEMPLATE)),
    downloadTemplate: vi.fn(() => of(new Blob(['xlsx']))),
    ...api,
  };
  const useCase = { execute: vi.fn() };
  TestBed.configureTestingModule({
    imports: [BillingUploadPage],
    providers: [
      { provide: UploadBillingFilesUseCase, useValue: useCase },
      { provide: BillingApiService, useValue: billingApi },
    ],
  });
  const fixture = TestBed.createComponent(BillingUploadPage);
  fixture.detectChanges();
  return { fixture, billingApi, useCase };
}

describe('BillingUploadPage', () => {
  it('resuelve repositorio Billing con configuración real', () => {
    TestBed.configureTestingModule({ providers: appConfig.providers });

    expect(TestBed.inject(UploadBillingFilesUseCase)).toBeInstanceOf(UploadBillingFilesUseCase);
    expect(TestBed.inject(BillingRepository)).toBeInstanceOf(HttpBillingRepository);
  });

  it('permite sólo xls/xlsx hasta 10 MiB y máximo cinco archivos', () => {
    const { fixture } = configurePage();
    const page = fixture.componentInstance as unknown as {
      addFiles: (files: File[]) => void;
      selectedFiles: () => File[];
      selectionMessage: () => string | null;
    };
    page.addFiles([
      new File(['a'], 'uno.xlsx'), new File(['b'], 'dos.xls'), new File(['c'], 'tres.xlsx'),
      new File(['d'], 'cuatro.xlsx'), new File(['e'], 'cinco.xlsx'), new File(['f'], 'seis.xlsx'),
      new File(['g'], 'texto.csv'), new File([new Uint8Array(10 * 1024 * 1024 + 1)], 'grande.xlsx'),
    ]);
    expect(page.selectedFiles()).toHaveLength(5);
    expect(page.selectionMessage()).toContain('Sólo se permiten 5 archivos');
    expect(page.selectionMessage()).toContain('formato no permitido');
    expect(page.selectionMessage()).toContain('supera el máximo');
  });

  it('carga la plantilla y mantiene deshabilitada cualquier confirmación', () => {
    const { fixture } = configurePage();
    expect(fixture.nativeElement.textContent).toContain('Descargar layout');
    expect(fixture.nativeElement.textContent).toContain('Aún no hay cargas');
    expect(fixture.nativeElement.textContent).not.toContain('Plantilla oficial');
    expect(fixture.nativeElement.textContent).not.toContain('Confirmar carga');
    expect(fixture.nativeElement.querySelector('button[disabled]')).toBeNull();
  });

  it('muestra error de plantilla 409, bloquea acciones y permite reintentar', () => {
    const api = {
      template: vi.fn()
        .mockReturnValueOnce(throwError(() => new HttpErrorResponse({
          status: 409,
          error: { code: 'FACTURACION_PLANTILLA_NO_CONFIGURADA', message: 'No existe una plantilla activa.', correlationId: 'corr-template' },
        })))
        .mockReturnValueOnce(of(TEMPLATE)),
    };
    const { fixture, billingApi } = configurePage(api);
    expect(fixture.nativeElement.querySelector('[role="alert"]')?.textContent).toContain('corr-template');
    expect(fixture.nativeElement.querySelector('input[type="file"]')?.disabled).toBe(true);
    expect(fixture.nativeElement.querySelector('button[title="Descargar layout compatible"]')?.disabled).toBe(true);

    fixture.nativeElement.querySelector('button:not([title])').click();
    fixture.detectChanges();
    expect(billingApi.template).toHaveBeenCalledTimes(2);
    expect(fixture.nativeElement.textContent).not.toContain('No fue posible obtener la configuración');
    expect(fixture.nativeElement.querySelector('input[type="file"]')?.disabled).toBe(false);
  });

  it('muestra error genérico de plantilla 503', () => {
    const api = {
      template: vi.fn(() => throwError(() => new HttpErrorResponse({ status: 503, error: {} }))),
    };
    const { fixture } = configurePage(api);
    expect(fixture.nativeElement.querySelector('[role="alert"]')?.textContent).toContain('servicio no está disponible');
  });

  it('maneja descarga exitosa y libera el estado de carga', () => {
    const createObjectURL = vi.fn(() => 'blob:test');
    const revokeObjectURL = vi.fn();
    vi.stubGlobal('URL', { createObjectURL, revokeObjectURL });
    const { fixture, billingApi } = configurePage();
    fixture.nativeElement.querySelector('button[title="Descargar layout compatible"]').click();
    fixture.detectChanges();
    expect(billingApi.downloadTemplate).toHaveBeenCalledTimes(1);
    expect(createObjectURL).toHaveBeenCalledTimes(1);
    expect(revokeObjectURL).toHaveBeenCalledTimes(1);
    expect(fixture.nativeElement.textContent).not.toContain('Descargando');
    vi.unstubAllGlobals();
  });

  it('muestra error de descarga y no deja loading perpetuo', () => {
    const api = {
      downloadTemplate: vi.fn(() => throwError(() => new HttpErrorResponse({ status: 503, error: {} }))),
    };
    const { fixture } = configurePage(api);
    fixture.nativeElement.querySelector('button[title="Descargar layout compatible"]').click();
    fixture.detectChanges();
    expect(fixture.nativeElement.querySelector('[role="alert"]')?.textContent).toContain('No fue posible descargar el layout');
    expect(fixture.nativeElement.textContent).not.toContain('Descargando');
  });

  it.each([
    { initialStatus: 'VALIDADA' as const, persistedStatus: 'PREVISUALIZADA' as BillingPersistedLoadStatus, label: 'Validada' },
    { initialStatus: 'CON_ERRORES' as const, persistedStatus: 'INVALIDA' as BillingPersistedLoadStatus, label: 'Con errores' },
    { initialStatus: 'FALLIDA' as const, persistedStatus: 'INVALIDA' as BillingPersistedLoadStatus, label: 'Fallida' },
  ])('preserva estado de presentación $initialStatus ante estado persistido $persistedStatus', ({ initialStatus, persistedStatus, label }) => {
    const detail = { id: 1, archivo: 'uno.xlsx', hash: 'a', estado: persistedStatus, totalRegistros: 101, registrosValidos: 101, registrosInvalidos: 0, preview: { filas: [{ Documento: 'B' }], pagina: 2, tamano: 50 }, errores: [] };
    const { fixture, billingApi } = configurePage({ load: vi.fn(() => of(detail)) });
    const page = fixture.componentInstance as unknown as { response: { set(value: unknown): void; (): { cargas: { estado: string }[] } }; previewStates: { set(value: unknown): void; (): Record<number, { page: number; pageSize: number }> }; changePreviewPage(id: number, event: { pageIndex: number; pageSize: number }): void };
    page.response.set({ correlationId: 'corr', plantilla: 'FACTURACION:V1', confirmacionDisponible: false, cargas: [
      { ...detail, estado: initialStatus, preview: { columnas: ['Documento'], filas: [{ Documento: 'A' }] } },
    ] });
    page.previewStates.set({ 1: { page: 1, pageSize: 100, loading: false, error: null } });

    page.changePreviewPage(1, { pageIndex: 1, pageSize: 50 });
    fixture.detectChanges();

    expect(billingApi.load).toHaveBeenCalledWith(1, 2, 50);
    expect(page.response().cargas[0].estado).toBe(initialStatus);
    expect(fixture.nativeElement.textContent).toContain(label);
  });

});
