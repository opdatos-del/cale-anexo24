import { test, expect, type Page } from '@playwright/test';
import { assertNoRuntimeFailures, authenticateWithPermissions, E2E_ALL_PERMISSIONS, installRuntimeMonitors } from './fixtures';

const SALDOS_PATH = /\/api\/v1\/reportes\/saldos(?:\?.*)?$/;
const SALDOS_EXPORT_PATH = /\/api\/v1\/reportes\/saldos\/exportacion(?:\?.*)?$/;

async function mockSavedQueries(page: Page): Promise<void> {
  await page.route('**/api/v1/consultas-guardadas**', (route) => route.fulfill({ status: 200, contentType: 'application/json', body: '[]' }));
}

async function openSaldos(page: Page, permissions: readonly string[] = E2E_ALL_PERMISSIONS): Promise<void> {
  await authenticateWithPermissions(page, permissions);
  await mockSavedQueries(page);
  await page.goto('/reportes');
  const saldos = page.getByRole('button', { name: 'Saldos' });
  await expect(saldos).toBeVisible();
  await expect(saldos).toBeEnabled();
  await saldos.click();
  await expect(page.getByText('Consulta read-only del reporte legacy de saldos.')).toBeVisible();
  await expect(page.getByLabel('Fecha inicial del periodo')).toBeVisible();
  await expect(page.getByLabel('Fecha final del periodo')).toBeVisible();
  await expect(page.getByText('Documento / Pedimento')).toBeVisible();
}

async function setSaldosPeriod(page: Page): Promise<void> {
  await page.getByRole('button', { name: '7 días' }).click();
  await expect(page.getByRole('button', { name: 'Generar' })).toBeEnabled();
}

test('Saldos habilitado genera estado vacío y pagina contra respuestas sintéticas', async ({ page }) => {
  const requests: URL[] = [];
  await page.route(SALDOS_PATH, async (route) => {
    const url = new URL(route.request().url());
    requests.push(url);
    const pageNumber = Number(url.searchParams.get('pagina'));
    await route.fulfill({
      status: 200,
      contentType: 'application/json',
      body: JSON.stringify({
        items: pageNumber === 2 ? [{ documento: 'PED-002', fechaPago: '2025-01-02', clave: 'A1', fraccion: '0101.21.01', numeroParte: 'NP-2', descripcion: 'Fila sintética', saldo: 4 }]: [],
        total: 21,
        pagina: pageNumber,
        tamano: Number(url.searchParams.get('tamano')),
      }),
    });
  });
  const monitors = installRuntimeMonitors(page);

  await openSaldos(page);
  await setSaldosPeriod(page);
  await page.getByLabel('Documento / Pedimento').fill('PED-001');
  await page.getByRole('button', { name: 'Generar' }).click();

  await expect(page.getByText('No se encontraron resultados')).toBeVisible();
  await expect(page.locator('mat-paginator[aria-label="Paginación del reporte"]')).toBeVisible();
  await expect.poll(() => requests.length).toBe(1);
  expect(requests[0].searchParams.get('desde')).toMatch(/^\d{4}-\d{2}-\d{2}$/);
  expect(requests[0].searchParams.get('hasta')).toMatch(/^\d{4}-\d{2}-\d{2}$/);
  expect(requests[0].searchParams.get('documento')).toBe('PED-001');

  await page.getByRole('button', { name: /next page/i }).click();
  await expect(page.getByText('PED-002')).toBeVisible();
  await expect.poll(() => requests.length).toBe(2);
  expect(requests[1].searchParams.get('pagina')).toBe('2');
  await assertNoRuntimeFailures(monitors);
});

test('Saldos descarga XLSX sintético sólo con permiso de exportación', async ({ page }) => {
  const exportRequests: URL[] = [];
  await page.route(SALDOS_PATH, (route) => route.fulfill({ status: 200, contentType: 'application/json', body: JSON.stringify({ items: [{ documento: 'PED-001', fechaPago: '2025-01-01', clave: 'A1', fraccion: '0101.21.01', numeroParte: 'NP-1', descripcion: 'Fila sintética', saldo: 12 }], total: 1, pagina: 1, tamano: 20 }) }));
  await page.route(SALDOS_EXPORT_PATH, async (route) => {
    exportRequests.push(new URL(route.request().url()));
    await route.fulfill({ status: 200, contentType: 'application/vnd.openxmlformats-officedocument.spreadsheetml.sheet', body: 'xlsx-sintetico' });
  });
  const monitors = installRuntimeMonitors(page);

  await openSaldos(page);
  await setSaldosPeriod(page);
  await page.getByRole('button', { name: 'Generar' }).click();
  await expect(page.getByRole('button', { name: 'XLSX' })).toBeEnabled();

  const download = page.waitForEvent('download');
  await page.getByRole('button', { name: 'XLSX' }).click();
  expect((await download).suggestedFilename()).toBe('saldos.xlsx');
  await expect.poll(() => exportRequests.length).toBe(1);
  expect(exportRequests[0].searchParams.get('tamano')).toBe('100');
  await assertNoRuntimeFailures(monitors);
});

test('Saldos muestra error HTTP controlado sin presentar resultado anterior', async ({ page }) => {
  await page.route(SALDOS_PATH, (route) => route.fulfill({ status: 503, contentType: 'application/json', body: JSON.stringify({ message: 'Servicio sintético no disponible', correlationId: 'e2e-saldos-error' }) }));

  await openSaldos(page);
  await setSaldosPeriod(page);
  await page.getByRole('button', { name: 'Generar' }).click();

  await expect(page.getByRole('alert')).toContainText('El servicio no está disponible. Referencia: e2e-saldos-error');
  await expect(page.getByRole('button', { name: 'XLSX' })).toHaveCount(0);
});

test('Saldos oculta exportación para permiso de generación sin exportación', async ({ page }) => {
  await page.route(SALDOS_PATH, (route) => route.fulfill({ status: 200, contentType: 'application/json', body: JSON.stringify({ items: [{ documento: 'PED-001', fechaPago: '2025-01-01', clave: 'A1', fraccion: '0101.21.01', numeroParte: 'NP-1', descripcion: 'Fila sintética', saldo: 12 }], total: 1, pagina: 1, tamano: 20 }) }));

  await openSaldos(page, ['REPORTES_GENERAR']);
  await setSaldosPeriod(page);
  await page.getByRole('button', { name: 'Generar' }).click();
  await expect(page.getByText('PED-001')).toBeVisible();
  await expect(page.getByRole('button', { name: 'XLSX' })).toHaveCount(0);
});

test('Reportes deniega ruta sin permiso de generación', async ({ page }) => {
  await authenticateWithPermissions(page, ['REPORTES_EXPORTAR']);
  await page.goto('/reportes');
  await expect(page).toHaveURL(/\/forbidden$/);
  await expect(page.getByRole('heading', { name: 'No tiene permiso para acceder a este módulo' })).toBeVisible();
});
