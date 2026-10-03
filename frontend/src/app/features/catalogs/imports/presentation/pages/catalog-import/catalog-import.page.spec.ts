import { TestBed } from '@angular/core/testing';
import { describe, expect, it, vi } from 'vitest';
import { of, Subject, throwError } from 'rxjs';
import { AuthService } from '@core/auth/auth.service';
import { NotificationService } from '@core/notifications/notification.service';
import { ConfirmService } from '@core/ui/confirm-dialog/confirm.service';
import { ConfirmCatalogMaterialImportUseCase } from '@features/catalogs/imports/application/use-cases/confirm-catalog-material-import.use-case';
import { UploadCatalogImportUseCase } from '@features/catalogs/imports/application/use-cases/upload-catalog-import.use-case';
import { CatalogImportResponse, CatalogMaterialImportConfirmation } from '@features/catalogs/imports/domain/models/catalog-import.model';
import { CatalogImportPage } from './catalog-import.page';

const RESPONSE: CatalogImportResponse = {
  id: 7,
  tipo: 'MATERIAL',
  archivo: 'materiales.xlsx',
  hash: 'a'.repeat(64),
  estado: 'PREVISUALIZADA',
  totalFilas: 1,
  filasValidas: 1,
  filasInvalidas: 0,
  columnas: ['ClaveMaterial'],
  filas: [{ ClaveMaterial: 'MAT001' }],
  totalPersistido: 1,
  errores: [],
};

const CONFIRMATION: CatalogMaterialImportConfirmation = {
  cargaId: 7,
  estado: 'CONFIRMADA',
  totalFilas: 1,
  filasValidas: 1,
  filasConError: 0,
  confirmadaEn: '2026-10-03T12:00:00',
};

interface PageHarness {
  result: { set(value: CatalogImportResponse): void; (): CatalogImportResponse | null };
  isConfirming: () => boolean;
  requestConfirmation(): void;
}

function configure(options: {
  permissions?: string[];
  upload?: ReturnType<typeof vi.fn>;
  confirm?: ReturnType<typeof vi.fn>;
  dialogResult?: boolean;
} = {}) {
  const permissions = options.permissions ?? ['MATERIALES_CARGAR', 'PRODUCTOS_CARGAR'];
  const upload = options.upload ?? vi.fn(() => of(RESPONSE));
  const confirm = options.confirm ?? vi.fn(() => of(CONFIRMATION));
  const dialog = { ask: vi.fn(() => of(options.dialogResult ?? true)) };
  const notifications = { error: vi.fn(), success: vi.fn() };
  TestBed.configureTestingModule({
    imports: [CatalogImportPage],
    providers: [
      { provide: UploadCatalogImportUseCase, useValue: { execute: upload } },
      { provide: ConfirmCatalogMaterialImportUseCase, useValue: { execute: confirm } },
      { provide: ConfirmService, useValue: dialog },
      { provide: NotificationService, useValue: notifications },
      { provide: AuthService, useValue: { hasPermission: (permission: string) => permissions.includes(permission) } },
    ],
  });
  const fixture = TestBed.createComponent(CatalogImportPage);
  fixture.detectChanges();
  return { fixture, harness: fixture.componentInstance as unknown as PageHarness, upload, confirm, dialog, notifications };
}

function selectFile(harness: { onFileSelected: (event: Event) => void }, file = new File(['xlsx'], 'materiales.xlsx')): void {
  const input = document.createElement('input');
  Object.defineProperty(input, 'files', { value: [file] });
  harness.onFileSelected({ target: input } as unknown as Event);
}

describe('CatalogImportPage', () => {
  it('muestra sólo las pestañas permitidas', () => {
    const { fixture } = configure({ permissions: ['PRODUCTOS_CARGAR'] });
    expect(fixture.nativeElement.textContent).toContain('Productos');
    expect(fixture.nativeElement.textContent).not.toContain('Materiales');
  });

  it('sube material y muestra preview sin confirmación para quien sólo puede cargar', () => {
    const { fixture, harness, upload } = configure();
    selectFile(harness as unknown as { onFileSelected: (event: Event) => void });
    fixture.detectChanges();

    expect(upload).toHaveBeenCalledWith('MATERIAL', expect.any(File));
    expect(fixture.nativeElement.textContent).toContain('MAT001');
    expect(fixture.nativeElement.textContent).toContain('La confirmación hacia CALE_IMMEX todavía no está habilitada');
    expect(fixture.nativeElement.textContent).not.toContain('Confirmar importación');
  });

  it('muestra Confirmar importación sólo para materiales previsualizados con permiso', () => {
    const { fixture, harness } = configure({ permissions: ['MATERIALES_CARGAR', 'MATERIALES_CONFIRMAR'] });
    harness.result.set(RESPONSE);
    fixture.detectChanges();

    expect(fixture.nativeElement.textContent).toContain('Confirmar importación');
  });

  it('mantiene productos únicamente en previsualización aunque exista permiso de confirmar materiales', () => {
    const { fixture, harness } = configure({ permissions: ['PRODUCTOS_CARGAR', 'MATERIALES_CONFIRMAR'] });
    harness.result.set({ ...RESPONSE, tipo: 'PRODUCTO' });
    fixture.detectChanges();

    expect(fixture.nativeElement.textContent).toContain('La confirmación hacia CALE_IMMEX todavía no está habilitada');
    expect(fixture.nativeElement.textContent).not.toContain('Confirmar importación');
  });

  it('cancela la confirmación sin invocar el caso de uso', () => {
    const { harness, confirm, dialog } = configure({ permissions: ['MATERIALES_CARGAR', 'MATERIALES_CONFIRMAR'], dialogResult: false });
    harness.result.set(RESPONSE);
    harness.requestConfirmation();

    expect(dialog.ask).toHaveBeenCalledWith(expect.objectContaining({
      message: 'La confirmación actualizará el catálogo de materiales utilizando las reglas actuales del sistema Anexo 24.',
    }));
    expect(confirm).not.toHaveBeenCalled();
  });

  it('confirma, muestra estado CONFIRMADA y actualiza los totales', () => {
    const { fixture, harness, confirm, notifications } = configure({ permissions: ['MATERIALES_CARGAR', 'MATERIALES_CONFIRMAR'] });
    harness.result.set(RESPONSE);
    harness.requestConfirmation();
    fixture.detectChanges();

    expect(confirm).toHaveBeenCalledWith(7);
    expect(harness.result()?.estado).toBe('CONFIRMADA');
    expect(fixture.nativeElement.textContent).toContain('Importación confirmada correctamente');
    expect(fixture.nativeElement.textContent).toContain('2026-10-03T12:00:00');
    expect(notifications.success).toHaveBeenCalledWith('Importación de materiales confirmada correctamente.');
  });

  it('muestra carga mientras confirma y evita solicitudes duplicadas', () => {
    const response = new Subject<CatalogMaterialImportConfirmation>();
    const { fixture, harness, confirm } = configure({
      permissions: ['MATERIALES_CARGAR', 'MATERIALES_CONFIRMAR'],
      confirm: vi.fn(() => response),
    });
    harness.result.set(RESPONSE);
    harness.requestConfirmation();
    harness.requestConfirmation();
    fixture.detectChanges();

    expect(harness.isConfirming()).toBe(true);
    expect(fixture.nativeElement.textContent).toContain('Confirmando importación...');
    expect(confirm).toHaveBeenCalledTimes(1);

    response.next(CONFIRMATION);
    response.complete();
  });

  it('muestra un error recuperable si falla la confirmación', () => {
    const { fixture, harness, notifications } = configure({
      permissions: ['MATERIALES_CARGAR', 'MATERIALES_CONFIRMAR'],
      confirm: vi.fn(() => throwError(() => ({ error: { message: 'La carga ya no es confirmable' } }))),
    });
    harness.result.set(RESPONSE);
    harness.requestConfirmation();
    fixture.detectChanges();

    expect(harness.result()?.estado).toBe('PREVISUALIZADA');
    expect(fixture.nativeElement.querySelector('[role="alert"]')?.textContent).toContain('La carga ya no es confirmable');
    expect(notifications.error).toHaveBeenCalledWith('No fue posible confirmar la importación de materiales.');
  });

  it('no ofrece confirmación para una carga ya confirmada', () => {
    const { fixture, harness } = configure({ permissions: ['MATERIALES_CARGAR', 'MATERIALES_CONFIRMAR'] });
    harness.result.set({ ...RESPONSE, estado: 'CONFIRMADA' });
    fixture.detectChanges();

    expect(fixture.nativeElement.textContent).toContain('Importación confirmada correctamente');
    expect(fixture.nativeElement.textContent).not.toContain('Confirmar importación');
  });

  it('acepta extensión xls y muestra error recuperable', () => {
    const upload = vi.fn(() => throwError(() => ({ error: { message: 'Header inválido' } })));
    const { fixture, harness } = configure({ upload });
    selectFile(harness as unknown as { onFileSelected: (event: Event) => void }, new File(['xls'], 'materiales.xls'));
    fixture.detectChanges();

    expect(upload).toHaveBeenCalledWith('MATERIAL', expect.any(File));
    expect(fixture.nativeElement.querySelector('[role="alert"]')?.textContent).toContain('Header inválido');
  });

  it('rechaza extensiones no soportadas', () => {
    const { fixture, harness } = configure();
    selectFile(harness as unknown as { onFileSelected: (event: Event) => void }, new File(['csv'], 'materiales.csv'));
    fixture.detectChanges();

    expect(fixture.nativeElement.textContent).toContain('Selecciona un archivo .xls o .xlsx');
  });
});
