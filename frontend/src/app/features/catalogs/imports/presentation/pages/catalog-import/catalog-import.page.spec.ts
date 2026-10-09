import { TestBed } from '@angular/core/testing';
import { describe, expect, it, vi } from 'vitest';
import { of, Subject, throwError } from 'rxjs';
import { AuthService } from '@core/auth/auth.service';
import { NotificationService } from '@core/notifications/notification.service';
import { ConfirmService } from '@core/ui/confirm-dialog/confirm.service';
import { ConfirmCatalogMaterialImportUseCase } from '@features/catalogs/imports/application/use-cases/confirm-catalog-material-import.use-case';
import { ConfirmCatalogProductImportUseCase } from '@features/catalogs/imports/application/use-cases/confirm-catalog-product-import.use-case';
import { ConfirmCatalogClientImportUseCase } from '@features/catalogs/imports/application/use-cases/confirm-catalog-client-import.use-case';
import { ConfirmCatalogProviderImportUseCase } from '@features/catalogs/imports/application/use-cases/confirm-catalog-provider-import.use-case';
import { ConfirmCatalogAgentImportUseCase } from '@features/catalogs/imports/application/use-cases/confirm-catalog-agent-import.use-case';
import { UploadCatalogImportUseCase } from '@features/catalogs/imports/application/use-cases/upload-catalog-import.use-case';
import { GetCatalogImportPreviewUseCase } from '@features/catalogs/imports/application/use-cases/get-catalog-import-preview.use-case';
import { CatalogImportErrorCsvService } from '@features/catalogs/imports/application/catalog-import-error-csv.service';
import { CatalogImportResponse, CatalogMaterialImportConfirmation, CatalogProductImportConfirmation, CatalogClientImportConfirmation, CatalogProviderImportConfirmation, CatalogAgentImportConfirmation } from '@features/catalogs/imports/domain/models/catalog-import.model';
import { CatalogImportPage } from './catalog-import.page';

const RESPONSE_MATERIAL: CatalogImportResponse = {
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

const RESPONSE_PRODUCTO: CatalogImportResponse = {
  ...RESPONSE_MATERIAL,
  id: 9,
  tipo: 'PRODUCTO',
  archivo: 'productos.xlsx',
  columnas: ['CveProducto'],
  filas: [{ CveProducto: 'PROD-1' }],
};

const RESPONSE_CLIENTE: CatalogImportResponse = {
  ...RESPONSE_MATERIAL,
  id: 11,
  tipo: 'CLIENTE',
  archivo: 'clientes.xlsx',
  columnas: ['Clave'],
  filas: [{ Clave: 'CLI-001' }],
};

const RESPONSE_PROVEEDOR: CatalogImportResponse = {
  ...RESPONSE_MATERIAL,
  id: 13,
  tipo: 'PROVEEDOR',
  archivo: 'proveedores.xlsx',
  columnas: ['Clave'],
  filas: [{ Clave: 'PROV-001' }],
};

const CONFIRMATION_MATERIAL: CatalogMaterialImportConfirmation = {
  cargaId: 7,
  estado: 'CONFIRMADA',
  totalFilas: 1,
  filasValidas: 1,
  filasConError: 0,
  confirmadaEn: '2026-10-03T12:00:00',
};

const CONFIRMATION_PRODUCTO: CatalogProductImportConfirmation = {
  cargaId: 9,
  estado: 'CONFIRMADA',
  totalFilas: 1,
  filasValidas: 1,
  filasConError: 0,
  confirmadaEn: '2026-10-03T12:00:00',
};

const CONFIRMATION_CLIENTE: CatalogClientImportConfirmation = {
  cargaId: 11,
  estado: 'CONFIRMADA',
  totalFilas: 1,
  filasValidas: 1,
  filasConError: 0,
  confirmadaEn: '2026-10-03T12:00:00',
};

const CONFIRMATION_PROVEEDOR: CatalogProviderImportConfirmation = {
  cargaId: 13,
  estado: 'CONFIRMADA',
  totalFilas: 1,
  filasValidas: 1,
  filasConError: 0,
  confirmadaEn: '2026-10-03T12:00:00',
};

const RESPONSE_AGENTE: CatalogImportResponse = {
  ...RESPONSE_MATERIAL,
  id: 15,
  tipo: 'AGENTE',
  archivo: 'agentes.xlsx',
  columnas: ['Clave', 'Nombre', 'Domicilio', 'Rfc', 'Patente'],
  filas: [{ Clave: 'AGE-001', Nombre: 'Agente Uno', Domicilio: 'Calle 1', Rfc: 'AAA010101', Patente: '1234' }],
};

const CONFIRMATION_AGENTE: CatalogAgentImportConfirmation = {
  cargaId: 15,
  estado: 'CONFIRMADA',
  totalFilas: 1,
  filasValidas: 1,
  filasConError: 0,
  confirmadaEn: '2026-10-05T12:00:00',
};

interface PageHarness {
  result: { set(value: CatalogImportResponse): void; (): CatalogImportResponse | null };
  isConfirming: () => boolean;
  type: () => string;
  requestConfirmation(): void;
  changePreviewPage(event: { pageIndex: number; pageSize: number }): void;
  downloadErrors(): void;
  selectType(type: string): void;
}

function configure(options: {
  permissions?: string[];
  upload?: ReturnType<typeof vi.fn>;
  preview?: ReturnType<typeof vi.fn>;
  errorDownload?: ReturnType<typeof vi.fn>;
  confirmMaterial?: ReturnType<typeof vi.fn>;
  confirmProduct?: ReturnType<typeof vi.fn>;
  confirmClient?: ReturnType<typeof vi.fn>;
  confirmProvider?: ReturnType<typeof vi.fn>;
  confirmAgent?: ReturnType<typeof vi.fn>;
  dialogResult?: boolean;
} = {}) {
  const permissions = options.permissions ?? ['MATERIALES_CARGAR', 'PRODUCTOS_CARGAR', 'CLIENTES_CARGAR', 'PROVEEDORES_CARGAR'];
  const upload = options.upload ?? vi.fn(() => of(RESPONSE_MATERIAL));
  const preview = options.preview ?? vi.fn(() => of(RESPONSE_MATERIAL));
  const errorDownload = options.errorDownload ?? vi.fn(() => of(undefined));
  const confirmMaterial = options.confirmMaterial ?? vi.fn(() => of(CONFIRMATION_MATERIAL));
  const confirmProduct = options.confirmProduct ?? vi.fn(() => of(CONFIRMATION_PRODUCTO));
  const confirmClient = options.confirmClient ?? vi.fn(() => of(CONFIRMATION_CLIENTE));
  const confirmProvider = options.confirmProvider ?? vi.fn(() => of(CONFIRMATION_PROVEEDOR));
  const confirmAgent = options.confirmAgent ?? vi.fn(() => of(CONFIRMATION_AGENTE));
  const dialog = { ask: vi.fn(() => of(options.dialogResult ?? true)) };
  const notifications = { error: vi.fn(), success: vi.fn() };
  TestBed.configureTestingModule({
    imports: [CatalogImportPage],
    providers: [
      { provide: UploadCatalogImportUseCase, useValue: { execute: upload } },
      { provide: GetCatalogImportPreviewUseCase, useValue: { execute: preview } },
      { provide: CatalogImportErrorCsvService, useValue: { download: errorDownload } },
      { provide: ConfirmCatalogMaterialImportUseCase, useValue: { execute: confirmMaterial } },
      { provide: ConfirmCatalogProductImportUseCase, useValue: { execute: confirmProduct } },
      { provide: ConfirmCatalogClientImportUseCase, useValue: { execute: confirmClient } },
      { provide: ConfirmCatalogProviderImportUseCase, useValue: { execute: confirmProvider } },
      { provide: ConfirmCatalogAgentImportUseCase, useValue: { execute: confirmAgent } },
      { provide: ConfirmService, useValue: dialog },
      { provide: NotificationService, useValue: notifications },
      { provide: AuthService, useValue: { hasPermission: (permission: string) => permissions.includes(permission) } },
    ],
  });
  const fixture = TestBed.createComponent(CatalogImportPage);
  fixture.detectChanges();
  return {
    fixture,
    harness: fixture.componentInstance as unknown as PageHarness,
    upload,
    preview,
    errorDownload,
    confirmMaterial,
    confirmProduct,
    confirmClient,
    confirmProvider,
    confirmAgent,
    dialog,
    notifications,
  };
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

  it.each([
    ['MATERIALES_CARGAR', 'Materiales', 'MATERIAL'],
    ['PRODUCTOS_CARGAR', 'Productos', 'PRODUCTO'],
    ['CLIENTES_CARGAR', 'Clientes', 'CLIENTE'],
    ['PROVEEDORES_CARGAR', 'Proveedores', 'PROVEEDOR'],
    ['AGENTES_CARGAR', 'Agentes aduanales', 'AGENTE'],
  ])('muestra sólo tipo autorizado e inicia en %s', (permission, label, type) => {
    const { fixture, harness } = configure({ permissions: [permission] });
    expect(fixture.nativeElement.textContent).toContain(label);
    expect(harness.type()).toBe(type);
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
    harness.result.set(RESPONSE_MATERIAL);
    fixture.detectChanges();

    expect(fixture.nativeElement.textContent).toContain('Confirmar importación');
    expect(fixture.nativeElement.textContent).toContain('La confirmación actualizará el catálogo de materiales');
  });

  it('muestra Confirmar importación para productos previsualizados con permiso y advierte que no actualiza existentes', () => {
    const { fixture, harness } = configure({ permissions: ['PRODUCTOS_CARGAR', 'PRODUCTOS_CONFIRMAR'] });
    harness.result.set({ ...RESPONSE_PRODUCTO, estado: 'PREVISUALIZADA' });
    fixture.detectChanges();

    expect(fixture.nativeElement.textContent).toContain('Confirmar importación');
    expect(fixture.nativeElement.textContent).toContain('La confirmación incorpora únicamente los registros válidos nuevos');
  });

  it('mantiene productos en previsualización aunque exista permiso de confirmar materiales', () => {
    const { fixture, harness } = configure({ permissions: ['PRODUCTOS_CARGAR', 'MATERIALES_CONFIRMAR'] });
    harness.result.set(RESPONSE_PRODUCTO);
    fixture.detectChanges();

    expect(fixture.nativeElement.textContent).toContain('La confirmación hacia CALE_IMMEX todavía no está habilitada');
    expect(fixture.nativeElement.textContent).not.toContain('Confirmar importación');
  });

  it('cancela la confirmación de materiales sin invocar el caso de uso', () => {
    const { harness, confirmMaterial, dialog } = configure({ permissions: ['MATERIALES_CARGAR', 'MATERIALES_CONFIRMAR'], dialogResult: false });
    harness.result.set(RESPONSE_MATERIAL);
    harness.requestConfirmation();

    expect(dialog.ask).toHaveBeenCalledWith(expect.objectContaining({
      title: 'Confirmar importación de materiales',
      message: 'La confirmación actualizará el catálogo de materiales utilizando las reglas actuales del sistema Anexo 24.',
    }));
    expect(confirmMaterial).not.toHaveBeenCalled();
  });

  it('confirma materiales, muestra estado CONFIRMADA y actualiza los totales', () => {
    const { fixture, harness, confirmMaterial, notifications } = configure({ permissions: ['MATERIALES_CARGAR', 'MATERIALES_CONFIRMAR'] });
    harness.result.set(RESPONSE_MATERIAL);
    harness.requestConfirmation();
    fixture.detectChanges();

    expect(confirmMaterial).toHaveBeenCalledWith(7);
    expect(harness.result()?.estado).toBe('CONFIRMADA');
    expect(fixture.nativeElement.textContent).toContain('Importación confirmada correctamente');
    expect(fixture.nativeElement.textContent).toContain('2026-10-03T12:00:00');
    expect(notifications.success).toHaveBeenCalledWith('Importación de materiales confirmada correctamente.');
  });

  it('confirma productos invocando el caso de uso de productos', () => {
    const { fixture, harness, confirmProduct, notifications } = configure({ permissions: ['PRODUCTOS_CARGAR', 'PRODUCTOS_CONFIRMAR'] });
    harness.result.set(RESPONSE_PRODUCTO);
    harness.requestConfirmation();
    fixture.detectChanges();

    expect(confirmProduct).toHaveBeenCalledWith(9);
    expect(harness.result()?.estado).toBe('CONFIRMADA');
    expect(notifications.success).toHaveBeenCalledWith('Importación de productos confirmada correctamente.');
  });

  it('muestra Confirmar importación para clientes previsualizados con permiso y advierte que no actualiza existentes', () => {
    const { fixture, harness } = configure({ permissions: ['CLIENTES_CARGAR', 'CLIENTES_CONFIRMAR'] });
    harness.result.set({ ...RESPONSE_CLIENTE, estado: 'PREVISUALIZADA' });
    fixture.detectChanges();

    expect(fixture.nativeElement.textContent).toContain('Confirmar importación');
    expect(fixture.nativeElement.textContent).toContain('La confirmación incorpora únicamente los registros válidos nuevos');
  });

  it('confirma clientes invocando el caso de uso de clientes', () => {
    const { fixture, harness, confirmClient, notifications } = configure({ permissions: ['CLIENTES_CARGAR', 'CLIENTES_CONFIRMAR'] });
    harness.result.set(RESPONSE_CLIENTE);
    harness.requestConfirmation();
    fixture.detectChanges();

    expect(confirmClient).toHaveBeenCalledWith(11);
    expect(harness.result()?.estado).toBe('CONFIRMADA');
    expect(notifications.success).toHaveBeenCalledWith('Importación de clientes confirmada correctamente.');
  });

  it('muestra un error recuperable si falla la confirmación de clientes', () => {
    const { fixture, harness, notifications } = configure({
      permissions: ['CLIENTES_CARGAR', 'CLIENTES_CONFIRMAR'],
      confirmClient: vi.fn(() => throwError(() => ({ error: { message: 'La carga ya no es confirmable' } }))),
    });
    harness.result.set(RESPONSE_CLIENTE);
    harness.requestConfirmation();
    fixture.detectChanges();

    expect(harness.result()?.estado).toBe('PREVISUALIZADA');
    expect(fixture.nativeElement.querySelector('[role="alert"]')?.textContent).toContain('La carga ya no es confirmable');
    expect(notifications.error).toHaveBeenCalledWith('No fue posible confirmar la importación de clientes.');
  });

  it('confirma proveedores invocando el caso de uso de proveedores', () => {
    const { fixture, harness, confirmProvider, notifications } = configure({ permissions: ['PROVEEDORES_CARGAR', 'PROVEEDORES_CONFIRMAR'] });
    harness.result.set(RESPONSE_PROVEEDOR);
    harness.requestConfirmation();
    fixture.detectChanges();

    expect(confirmProvider).toHaveBeenCalledWith(13);
    expect(harness.result()?.estado).toBe('CONFIRMADA');
    expect(notifications.success).toHaveBeenCalledWith('Importación de proveedores confirmada correctamente.');
  });

  it('muestra un error recuperable si falla la confirmación de proveedores', () => {
    const { fixture, harness, notifications } = configure({
      permissions: ['PROVEEDORES_CARGAR', 'PROVEEDORES_CONFIRMAR'],
      confirmProvider: vi.fn(() => throwError(() => ({ error: { message: 'La carga ya no es confirmable' } }))),
    });
    harness.result.set(RESPONSE_PROVEEDOR);
    harness.requestConfirmation();
    fixture.detectChanges();

    expect(harness.result()?.estado).toBe('PREVISUALIZADA');
    expect(fixture.nativeElement.querySelector('[role="alert"]')?.textContent).toContain('La carga ya no es confirmable');
    expect(notifications.error).toHaveBeenCalledWith('No fue posible confirmar la importación de proveedores.');
  });

  it('confirma agentes aduanales invocando el caso de uso de agentes', () => {
    const { fixture, harness, confirmAgent, notifications } = configure({ permissions: ['AGENTES_CARGAR', 'AGENTES_CONFIRMAR'] });
    harness.result.set(RESPONSE_AGENTE);
    harness.requestConfirmation();
    fixture.detectChanges();

    expect(confirmAgent).toHaveBeenCalledWith(15);
    expect(harness.result()?.estado).toBe('CONFIRMADA');
    expect(notifications.success).toHaveBeenCalledWith('Importación de agentes aduanales confirmada correctamente.');
  });

  it('muestra un error recuperable si falla la confirmación de agentes aduanales', () => {
    const { fixture, harness, notifications } = configure({
      permissions: ['AGENTES_CARGAR', 'AGENTES_CONFIRMAR'],
      confirmAgent: vi.fn(() => throwError(() => ({ error: { message: 'La carga ya no es confirmable' } }))),
    });
    harness.result.set(RESPONSE_AGENTE);
    harness.requestConfirmation();
    fixture.detectChanges();

    expect(harness.result()?.estado).toBe('PREVISUALIZADA');
    expect(fixture.nativeElement.querySelector('[role="alert"]')?.textContent).toContain('La carga ya no es confirmable');
    expect(notifications.error).toHaveBeenCalledWith('No fue posible confirmar la importación de agentes aduanales.');
  });

  it('muestra la pestaña de agentes aduanales con permiso de carga', () => {
    const { fixture } = configure({ permissions: ['AGENTES_CARGAR'] });
    fixture.detectChanges();

    expect(fixture.nativeElement.textContent).toContain('Agentes aduanales');
  });

  it('oculta la pestaña de agentes aduanales sin permiso de carga', () => {
    const { fixture } = configure({ permissions: ['MATERIALES_CARGAR'] });
    fixture.detectChanges();

    expect(fixture.nativeElement.textContent).not.toContain('Agentes aduanales');
  });

  it('muestra carga mientras confirma materiales y evita solicitudes duplicadas', () => {
    const response = new Subject<CatalogMaterialImportConfirmation>();
    const { fixture, harness, confirmMaterial } = configure({
      permissions: ['MATERIALES_CARGAR', 'MATERIALES_CONFIRMAR'],
      confirmMaterial: vi.fn(() => response),
    });
    harness.result.set(RESPONSE_MATERIAL);
    harness.requestConfirmation();
    harness.requestConfirmation();
    fixture.detectChanges();

    expect(harness.isConfirming()).toBe(true);
    expect(fixture.nativeElement.textContent).toContain('Confirmando importación...');
    expect(confirmMaterial).toHaveBeenCalledTimes(1);

    response.next(CONFIRMATION_MATERIAL);
    response.complete();
  });

  it('muestra un error recuperable si falla la confirmación de productos', () => {
    const { fixture, harness, notifications } = configure({
      permissions: ['PRODUCTOS_CARGAR', 'PRODUCTOS_CONFIRMAR'],
      confirmProduct: vi.fn(() => throwError(() => ({ error: { message: 'La carga ya no es confirmable' } }))),
    });
    harness.result.set(RESPONSE_PRODUCTO);
    harness.requestConfirmation();
    fixture.detectChanges();

    expect(harness.result()?.estado).toBe('PREVISUALIZADA');
    expect(fixture.nativeElement.querySelector('[role="alert"]')?.textContent).toContain('La carga ya no es confirmable');
    expect(notifications.error).toHaveBeenCalledWith('No fue posible confirmar la importación de productos.');
  });

  it('muestra un error recuperable si falla la confirmación de materiales', () => {
    const { fixture, harness, notifications } = configure({
      permissions: ['MATERIALES_CARGAR', 'MATERIALES_CONFIRMAR'],
      confirmMaterial: vi.fn(() => throwError(() => ({ error: { message: 'La carga ya no es confirmable' } }))),
    });
    harness.result.set(RESPONSE_MATERIAL);
    harness.requestConfirmation();
    fixture.detectChanges();

    expect(harness.result()?.estado).toBe('PREVISUALIZADA');
    expect(fixture.nativeElement.querySelector('[role="alert"]')?.textContent).toContain('La carga ya no es confirmable');
    expect(notifications.error).toHaveBeenCalledWith('No fue posible confirmar la importación de materiales.');
  });

  it('no ofrece confirmación para una carga ya confirmada', () => {
    const { fixture, harness } = configure({ permissions: ['MATERIALES_CARGAR', 'MATERIALES_CONFIRMAR'] });
    harness.result.set({ ...RESPONSE_MATERIAL, estado: 'CONFIRMADA' });
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

  it('muestra paginador solo cuando la previsualizacion supera su tamano de pagina', () => {
    const { fixture, harness } = configure();
    harness.result.set({ ...RESPONSE_MATERIAL, totalPersistido: 101 });
    fixture.detectChanges();

    expect(fixture.nativeElement.querySelector('mat-paginator')).not.toBeNull();
  });

  it('consulta la pagina solicitada sin volver a subir el archivo', () => {
    const response = { ...RESPONSE_MATERIAL, filas: [{ ClaveMaterial: 'MAT101' }], totalPersistido: 150 };
    const { fixture, harness, preview, upload } = configure({ preview: vi.fn(() => of(response)) });
    harness.result.set({ ...RESPONSE_MATERIAL, totalPersistido: 150 });

    harness.changePreviewPage({ pageIndex: 1, pageSize: 50 });
    fixture.detectChanges();

    expect(preview).toHaveBeenCalledWith('MATERIAL', 7, 2, 50);
    expect(upload).not.toHaveBeenCalled();
    expect(fixture.nativeElement.textContent).toContain('MAT101');
  });

  it('muestra error recuperable al fallar una pagina de previsualizacion', () => {
    const { fixture, harness, notifications } = configure({ preview: vi.fn(() => throwError(() => ({ error: { message: 'Pagina no disponible' } }))) });
    harness.result.set({ ...RESPONSE_MATERIAL, totalPersistido: 101 });

    harness.changePreviewPage({ pageIndex: 1, pageSize: 100 });
    fixture.detectChanges();

    expect(fixture.nativeElement.textContent).toContain('Pagina no disponible');
    expect(notifications.error).toHaveBeenCalledWith(expect.stringContaining('previsualiz'));
  });

  it('resetea la previsualizacion al cambiar de tipo', () => {
    const { harness } = configure();
    harness.result.set({ ...RESPONSE_MATERIAL, totalPersistido: 150 });

    harness.selectType('PRODUCTO');

    expect(harness.result()).toBeNull();
  });

  it('muestra y descarga errores sin borrar la previsualizacion', () => {
    const { fixture, harness, errorDownload } = configure();
    harness.result.set({ ...RESPONSE_MATERIAL, filasInvalidas: 1 });
    fixture.detectChanges();

    harness.downloadErrors();

    expect(fixture.nativeElement.textContent).toContain('Descargar errores');
    expect(errorDownload).toHaveBeenCalledWith('MATERIAL', 7);
    expect(harness.result()).not.toBeNull();
  });

  it('notifica error de descarga sin borrar la previsualizacion', () => {
    const { harness, notifications } = configure({ errorDownload: vi.fn(() => throwError(() => new Error('Fallo CSV'))) });
    harness.result.set({ ...RESPONSE_MATERIAL, filasInvalidas: 1 });

    harness.downloadErrors();

    expect(notifications.error).toHaveBeenCalledWith('Fallo CSV');
    expect(harness.result()).not.toBeNull();
  });
});