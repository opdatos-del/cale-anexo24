import { TestBed } from '@angular/core/testing';
import { describe, expect, it, vi } from 'vitest';
import { appConfig } from '@app/app.config';
import { UploadBillingFilesUseCase } from '@features/billing/application/use-cases/upload-billing-files.use-case';
import { BillingRepository } from '@features/billing/domain/repositories/billing.repository';
import { HttpBillingRepository } from '@features/billing/infrastructure/repositories/http-billing.repository';
import { BillingUploadPage } from './billing-upload.page';

describe('BillingUploadPage', () => {
  it('resuelve repositorio Billing con configuración real', () => {
    TestBed.configureTestingModule({ providers: appConfig.providers });

    expect(TestBed.inject(UploadBillingFilesUseCase)).toBeInstanceOf(UploadBillingFilesUseCase);
    expect(TestBed.inject(BillingRepository)).toBeInstanceOf(HttpBillingRepository);
  });

  it('permite sólo xls/xlsx hasta 10 MiB y máximo cinco archivos', () => {
    const useCase = { execute: vi.fn() };
    TestBed.configureTestingModule({ imports: [BillingUploadPage], providers: [{ provide: UploadBillingFilesUseCase, useValue: useCase }] });
    const page = TestBed.createComponent(BillingUploadPage).componentInstance as unknown as {
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

  it('muestra el layout compatible y mantiene deshabilitada cualquier confirmación', () => {
    const useCase = { execute: vi.fn() };
    TestBed.configureTestingModule({ imports: [BillingUploadPage], providers: [{ provide: UploadBillingFilesUseCase, useValue: useCase }] });
    const fixture = TestBed.createComponent(BillingUploadPage);
    fixture.detectChanges();
    expect(fixture.nativeElement.textContent).toContain('Descargar layout');
    expect(fixture.nativeElement.textContent).toContain('Aún no hay cargas');
    expect(fixture.nativeElement.querySelector('button[disabled]')).not.toBeNull();
    expect(fixture.nativeElement.textContent).not.toContain('Plantilla oficial');
    expect(fixture.nativeElement.textContent).not.toContain('Confirmar carga');
  });
});
