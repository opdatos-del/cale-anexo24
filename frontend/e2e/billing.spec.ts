import { test, expect, type Page, type Request } from '@playwright/test';
import { assertNoRuntimeFailures, assertNoUnexpectedApiRequests, authenticateWithPermissions, installRuntimeMonitors, installSyntheticApiIsolation } from './fixtures';

const template = {
  nombre: 'FACTURACION',
  version: 'E2E-2026',
  hoja: 'FACTURAS',
  columnas: [{ nombre: 'Documento', obligatoria: true, tipo: 'TEXTO' }],
};

const validUpload = {
  cargas: [{
    id: 101,
    archivo: 'facturacion-e2e.xlsx',
    hash: 'hash-e2e-101',
    estado: 'VALIDADA',
    totalRegistros: 2,
    registrosValidos: 2,
    registrosInvalidos: 0,
    preview: { columnas: ['Documento', 'Importe'], filas: [{ Documento: 'FAC-001', Importe: '100.00' }, { Documento: 'FAC-002', Importe: '200.00' }] },
    errores: [],
  }],
  correlationId: 'e2e-billing-valid',
  plantilla: 'FACTURACION',
  confirmacionDisponible: false,
};

const invalidUpload = {
  cargas: [{
    id: 102,
    archivo: 'facturacion-e2e-invalida.xlsx',
    hash: 'hash-e2e-102',
    estado: 'CON_ERRORES',
    totalRegistros: 2,
    registrosValidos: 1,
    registrosInvalidos: 1,
    preview: { columnas: ['Documento'], filas: [{ Documento: 'FAC-003' }] },
    errores: [{ hoja: 'FACTURAS', fila: 3, columna: 'Documento', valorEnmascarado: null, codigo: 'DOCUMENTO_REQUERIDO', mensaje: 'El documento es obligatorio.' }],
  }],
  correlationId: 'e2e-billing-invalid',
  plantilla: 'FACTURACION',
  confirmacionDisponible: false,
};

function persistedDetail(id = 101, page = 1, size = 100) {
  return {
    id,
    archivo: 'facturacion-e2e.xlsx',
    hash: 'hash-e2e-persistida',
    estado: 'PREVISUALIZADA',
    totalRegistros: 201,
    registrosValidos: 200,
    registrosInvalidos: 1,
    preview: {
      pagina: page,
      tamano: size,
      filas: [{ Documento: page === 2 ? 'FAC-101' : 'FAC-001', Importe: page === 2 ? '101.00' : '1.00' }],
    },
    errores: [{ hoja: 'FACTURAS', fila: 4, columna: 'Importe', valorEnmascarado: null, codigo: 'IMPORTE_INVALIDO', mensaje: 'El importe no es válido.' }],
  };
}

async function mockTemplate(page: Page): Promise<void> {
  await page.route('**/api/v1/facturacion/plantilla', (route) => route.fulfill({ status: 200, contentType: 'application/json', body: JSON.stringify(template) }));
}

async function openUpload(page: Page): Promise<void> {
  await authenticateWithPermissions(page, ['FACTURACION_CARGAR']);
  await mockTemplate(page);
  await page.goto('/facturacion');
  await expect(page.getByRole('tab', { name: 'Nueva carga' })).toHaveAttribute('aria-selected', 'true');
}

async function attachSyntheticWorkbook(page: Page, name = 'facturacion-e2e.xlsx'): Promise<void> {
  await page.getByLabel('Seleccionar archivos Excel').setInputFiles({
    name,
    mimeType: 'application/vnd.openxmlformats-officedocument.spreadsheetml.sheet',
    buffer: Buffer.from('workbook-synthetic'),
  });
}

function assertMultipartWithoutUserId(request: Request): void {
  expect(request.headers()['content-type']).toContain('multipart/form-data');
  expect(new URL(request.url()).searchParams.has('usuarioId')).toBe(false);
  expect(request.postData() ?? '').not.toContain('usuarioId');
}

test.describe('Facturación browser sintético', () => {
  test.beforeEach(async ({ page }) => {
    await installSyntheticApiIsolation(page);
  });

  test.afterEach(({ page }) => {
    assertNoUnexpectedApiRequests(page);
  });

  test('carga plantilla, valida archivo sintético y no ofrece confirmación operacional', async ({ page }) => {
    let uploadRequest: Request | undefined;
    await page.route('**/api/v1/facturacion/cargas', async (route) => {
      uploadRequest = route.request();
      await route.fulfill({ status: 200, contentType: 'application/json', body: JSON.stringify(validUpload) });
    });
    const monitors = installRuntimeMonitors(page);

    await openUpload(page);
    await expect(page.getByLabel('Seleccionar archivos Excel')).toBeEnabled();
    await expect(page.getByRole('button', { name: 'Descargar layout' })).toBeEnabled();
    await expect(page.getByRole('button', { name: /Confirmar|Guardar/ })).toHaveCount(0);

    await attachSyntheticWorkbook(page);
    await page.getByRole('button', { name: 'Cargar y validar' }).click();
    await expect(page.getByText('Validada', { exact: true })).toBeVisible();
    await expect(page.getByText('FAC-001')).toBeVisible();
    await expect(page.getByText('FAC-002')).toBeVisible();
    await expect(page.getByText('e2e-billing-valid')).toBeVisible();
    expect(uploadRequest).toBeDefined();
    assertMultipartWithoutUserId(uploadRequest!);
    await expect(page.getByRole('button', { name: /Confirmar|Guardar/ })).toHaveCount(0);
    await assertNoRuntimeFailures(monitors);
  });

  test('recupera error controlado de plantilla sin habilitar carga antes del reintento', async ({ page }) => {
    let attempts = 0;
    await page.route('**/api/v1/facturacion/plantilla', async (route) => {
      attempts += 1;
      await route.fulfill(attempts === 1
        ? { status: 409, contentType: 'application/json', body: JSON.stringify({ code: 'FACTURACION_PLANTILLA_NO_CONFIGURADA', message: 'No existe una plantilla activa.', correlationId: 'e2e-template-error' }) }
        : { status: 200, contentType: 'application/json', body: JSON.stringify(template) });
    });
    const monitors = installRuntimeMonitors(page, { ignoredConsoleErrorFragments: ['409 (Conflict)'] });
    await authenticateWithPermissions(page, ['FACTURACION_CARGAR']);
    await page.goto('/facturacion');
    await expect(page.getByRole('alert')).toContainText('e2e-template-error');
    await expect(page.getByLabel('Seleccionar archivos Excel')).toBeDisabled();
    await page.getByRole('button', { name: 'Reintentar' }).click();
    await expect(page.getByLabel('Seleccionar archivos Excel')).toBeEnabled();
    await assertNoRuntimeFailures(monitors);
  });

  test('presenta errores de validación inmediata sin confundirlos con estado persistido', async ({ page }) => {
    await page.route('**/api/v1/facturacion/cargas', (route) => route.fulfill({ status: 200, contentType: 'application/json', body: JSON.stringify(invalidUpload) }));
    const monitors = installRuntimeMonitors(page);
    await openUpload(page);
    await attachSyntheticWorkbook(page, 'facturacion-e2e-invalida.xlsx');
    await page.getByRole('button', { name: 'Cargar y validar' }).click();
    await expect(page.getByText('Con errores', { exact: true })).toBeVisible();
    await expect(page.getByText('El documento es obligatorio.')).toBeVisible();
    await expect(page.getByText('FACTURAS · Fila 3 · Documento · DOCUMENTO_REQUERIDO')).toBeVisible();
    await expect(page.getByText(/Previsualizada|Inválida/)).toHaveCount(0);
    await assertNoRuntimeFailures(monitors);
  });

  test('historial filtra, pagina y reinicia página al cambiar tamaño sin exponer usuarioId', async ({ page }) => {
    const historyRequests: URL[] = [];
    await page.route('**/api/v1/facturacion/cargas**', async (route) => {
      const url = new URL(route.request().url());
      historyRequests.push(url);
      await route.fulfill({
        status: 200,
        contentType: 'application/json',
        body: JSON.stringify({
          items: [{ id: url.searchParams.get('pagina') === '2' ? 303 : 101, archivo: url.searchParams.get('pagina') === '2' ? 'pagina-2.xlsx' : 'previsualizada.xlsx', hash: 'hash', fecha: '2026-10-10T10:00:00', estado: url.searchParams.get('pagina') === '2' ? 'INVALIDA' : 'PREVISUALIZADA', totalRegistros: 2, registrosValidos: 1, registrosInvalidos: 1 }],
          total: 51,
          pagina: Number(url.searchParams.get('pagina')),
          tamano: Number(url.searchParams.get('tamano')),
        }),
      });
    });
    const monitors = installRuntimeMonitors(page);
    await openUpload(page);
    await page.getByRole('tab', { name: 'Historial' }).click();
    await expect(page.getByLabel('Historial de cargas de facturación')).toBeVisible();
    await expect(page.getByLabel('Seleccionar archivos Excel')).toHaveCount(0);
    await expect(page.getByText('Previsualizada · 2 registros · 1 válidos · 1 inválidos', { exact: true })).toBeVisible();
    await page.locator('div[aria-label="Filtros de historial"] select').selectOption('INVALIDA');
    await page.locator('div[aria-label="Filtros de historial"] input').nth(0).fill('2026-10-01');
    await page.locator('div[aria-label="Filtros de historial"] input').nth(1).fill('2026-10-10');
    await expect.poll(() => historyRequests.length).toBeGreaterThan(2);
    const filtered = historyRequests.at(-1)!;
    expect(filtered.searchParams.get('estado')).toBe('INVALIDA');
    expect(filtered.searchParams.get('desde')).toBe('2026-10-01');
    expect(filtered.searchParams.get('hasta')).toBe('2026-10-10');
    expect(filtered.searchParams.has('usuarioId')).toBe(false);

    await page.locator('mat-paginator[aria-label="Paginación del historial"]').getByRole('button', { name: /next page/i }).click();
    await expect(page.getByText('pagina-2.xlsx')).toBeVisible();
    expect(historyRequests.at(-1)!.searchParams.get('pagina')).toBe('2');
    const historyPaginator = page.locator('mat-paginator[aria-label="Paginación del historial"]');
    await historyPaginator.locator('.mat-mdc-paginator-touch-target').click();
    await page.getByRole('option', { name: '50', exact: true }).click();
    await expect(historyPaginator.getByRole('combobox')).toContainText('50');
    await expect.poll(() => historyRequests.at(-1)?.searchParams.get('tamano')).toBe('50');
    expect(historyRequests.at(-1)!.searchParams.get('pagina')).toBe('1');
    await assertNoRuntimeFailures(monitors);
  });

  test('revisa detalle persistido, conserva deep link al recargar y pagina detalle separado del historial', async ({ page }) => {
    let templateRequests = 0;
    const detailRequests: URL[] = [];
    await page.route('**/api/v1/facturacion/plantilla', async (route) => { templateRequests += 1; await route.fulfill({ status: 200, contentType: 'application/json', body: JSON.stringify(template) }); });
    await page.route('**/api/v1/facturacion/cargas**', (route) => route.fulfill({ status: 200, contentType: 'application/json', body: JSON.stringify({ items: [{ id: 101, archivo: 'facturacion-e2e.xlsx', hash: 'hash', fecha: '2026-10-10T10:00:00', estado: 'PREVISUALIZADA', totalRegistros: 201, registrosValidos: 200, registrosInvalidos: 1 }], total: 201, pagina: 1, tamano: 20 }) }));
    await page.route('**/api/v1/facturacion/cargas/101**', async (route) => {
      const url = new URL(route.request().url());
      detailRequests.push(url);
      await route.fulfill({ status: 200, contentType: 'application/json', body: JSON.stringify(persistedDetail(101, Number(url.searchParams.get('pagina')), Number(url.searchParams.get('tamano')))) });
    });
    const monitors = installRuntimeMonitors(page);
    await authenticateWithPermissions(page, ['FACTURACION_CARGAR']);
    await page.goto('/facturacion?carga=101');
    await expect(page.getByLabel('Historial de cargas de facturación')).toBeVisible();
    await expect(page.getByLabel('Seleccionar archivos Excel')).toHaveCount(0);
    await expect(page.getByLabel('Detalle de carga persistida')).toContainText('Previsualizada');
    await expect(page.getByText('FAC-001')).toBeVisible();
    await expect(page.getByText('El importe no es válido.')).toBeVisible();
    expect(templateRequests).toBe(0);

    await page.locator('mat-paginator[aria-label="Paginación de vista previa persistida"]').getByRole('button', { name: /next page/i }).click();
    await expect(page.getByText('FAC-101')).toBeVisible();
    expect(detailRequests.at(-1)!.searchParams.get('pagina')).toBe('2');
    expect(detailRequests.at(-1)!.searchParams.get('tamano')).toBe('100');

    await page.reload();
    await expect(page).toHaveURL(/\/facturacion\?carga=101$/);
    await expect(page.getByLabel('Historial de cargas de facturación')).toBeVisible();
    await expect(page.getByLabel('Seleccionar archivos Excel')).toHaveCount(0);
    await page.getByRole('button', { name: 'Cerrar revisión' }).click();
    await expect(page).toHaveURL(/\/facturacion$/);
    await expect(page.getByLabel('Detalle de carga persistida')).toHaveCount(0);
    await assertNoRuntimeFailures(monitors);
  });

  test('deep link inexistente conserva mensaje neutral sin revelar pertenencia', async ({ page }) => {
    await page.route('**/api/v1/facturacion/cargas**', (route) => route.fulfill({ status: 200, contentType: 'application/json', body: JSON.stringify({ items: [], total: 0, pagina: 1, tamano: 20 }) }));
    await page.route('**/api/v1/facturacion/cargas/999**', (route) => route.fulfill({ status: 404, contentType: 'application/json', body: JSON.stringify({ code: 'RECURSO_NO_ENCONTRADO', correlationId: 'e2e-billing-404' }) }));
    await authenticateWithPermissions(page, ['FACTURACION_CARGAR']);
    await page.goto('/facturacion?carga=999');
    await expect(page.getByRole('alert')).toContainText('No fue posible encontrar esta carga.');
    await expect(page.getByText(/pertenece a otro usuario/i)).toHaveCount(0);
  });

  test('deniega facturación antes de invocar API de negocio sin permiso', async ({ page }) => {
    await authenticateWithPermissions(page, []);
    await page.goto('/facturacion');
    await expect(page).toHaveURL(/\/forbidden$/);
    await expect(page.getByRole('heading', { name: 'No tiene permiso para acceder a este módulo' })).toBeVisible();
  });
});
