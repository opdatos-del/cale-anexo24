import { TestBed } from '@angular/core/testing';
import { describe, expect, it, vi } from 'vitest';
import { of, throwError } from 'rxjs';
import { AuthService } from '@core/auth/auth.service';
import { NotificationService } from '@core/notifications/notification.service';
import { UploadCatalogImportUseCase } from '@features/catalogs/imports/application/use-cases/upload-catalog-import.use-case';
import { CatalogImportResponse } from '@features/catalogs/imports/domain/models/catalog-import.model';
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

function configure(options: { permissions?: string[]; execute?: ReturnType<typeof vi.fn> } = {}) {
  const permissions = options.permissions ?? ['MATERIALES_CARGAR', 'PRODUCTOS_CARGAR'];
  const useCase = options.execute ? { execute: options.execute } : { execute: vi.fn(() => of(RESPONSE)) };
  TestBed.configureTestingModule({
    imports: [CatalogImportPage],
    providers: [
      { provide: UploadCatalogImportUseCase, useValue: useCase },
      { provide: NotificationService, useValue: { error: vi.fn() } },
      { provide: AuthService, useValue: { hasPermission: (permission: string) => permissions.includes(permission) } },
    ],
  });
  const fixture = TestBed.createComponent(CatalogImportPage);
  fixture.detectChanges();
  return { fixture, useCase };
}

describe('CatalogImportPage', () => {
  it('muestra sólo las pestañas permitidas', () => {
    const { fixture } = configure({ permissions: ['PRODUCTOS_CARGAR'] });
    expect(fixture.nativeElement.textContent).toContain('Productos');
    expect(fixture.nativeElement.textContent).not.toContain('Materiales');
  });

  it('sube material y muestra preview sin confirmación', () => {
    const { fixture, useCase } = configure();
    const page = fixture.componentInstance as unknown as { onFileSelected: (event: Event) => void };
    const input = document.createElement('input');
    Object.defineProperty(input, 'files', { value: [new File(['xlsx'], 'materiales.xlsx')] });

    page.onFileSelected({ target: input } as unknown as Event);
    fixture.detectChanges();

    expect(useCase.execute).toHaveBeenCalledWith('MATERIAL', expect.any(File));
    expect(fixture.nativeElement.textContent).toContain('MAT001');
    expect(fixture.nativeElement.textContent).toContain('La confirmación hacia CALE_IMMEX todavía no está habilitada');
    expect(fixture.nativeElement.textContent).not.toContain('Confirmar carga');
  });

  it('acepta extensión xls y muestra error recuperable', () => {
    const execute = vi.fn(() => throwError(() => ({ error: { message: 'Header inválido' } })));
    const { fixture } = configure({ execute });
    const page = fixture.componentInstance as unknown as { onFileSelected: (event: Event) => void };
    const input = document.createElement('input');
    Object.defineProperty(input, 'files', { value: [new File(['xls'], 'materiales.xls')] });

    page.onFileSelected({ target: input } as unknown as Event);
    fixture.detectChanges();

    expect(execute).toHaveBeenCalledWith('MATERIAL', expect.any(File));
    expect(fixture.nativeElement.querySelector('[role="alert"]')?.textContent).toContain('Header inválido');
  });

  it('rechaza extensiones no soportadas', () => {
    const { fixture } = configure();
    const page = fixture.componentInstance as unknown as { onFileSelected: (event: Event) => void };
    const input = document.createElement('input');
    Object.defineProperty(input, 'files', { value: [new File(['csv'], 'materiales.csv')] });

    page.onFileSelected({ target: input } as unknown as Event);
    fixture.detectChanges();

    expect(fixture.nativeElement.textContent).toContain('Selecciona un archivo .xls o .xlsx');
  });
});
