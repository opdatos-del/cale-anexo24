import { test, expect, type Download, type Page } from '@playwright/test';
import { assertNoRuntimeFailures, authenticateWithPermissions, installRuntimeMonitors } from './fixtures';

async function downloadBytes(download: Download): Promise<number> {
  const stream = await download.createReadStream();
  let total = 0;
  for await (const chunk of stream ?? []) total += Buffer.byteLength(chunk);
  return total;
}

async function mockSavedQueries(page: Page): Promise<void> {
  await page.route('**/api/v1/consultas-guardadas**', (route) => route.fulfill({ status: 200, contentType: 'application/json', body: '[]' }));
}

test('Entradas construye solicitud, pagina y exporta XLSX sólo con permiso de exportación', async ({ page }) => {
  const requests: URL[] = [];
  const exports: URL[] = [];
  await page.route('**/api/v1/reportes/entradas/exportacion**', async (route) => {
    exports.push(new URL(route.request().url()));
    await route.fulfill({ status: 200, contentType: 'application/vnd.openxmlformats-officedocument.spreadsheetml.sheet', body: 'xlsx-entradas-sintetico' });
  });
  await page.route('**/api/v1/reportes/entradas**', async (route) => {
    const url = new URL(route.request().url());
    requests.push(url);
    await route.fulfill({ status: 200, contentType: 'application/json', body: JSON.stringify({ items: [{ pedimento: url.searchParams.get('pagina') === '2' ? 'PED-002' : 'PED-001', clavePedimento: 'A1', fechaEntrada: '2026-10-01', fechaPago: '2026-10-02', fraccion: '0101.21.01', unidadComercial: 'KG', cantidadComercial: 1, numeroParte: 'NP-1' }], total: 21, pagina: Number(url.searchParams.get('pagina')), tamano: Number(url.searchParams.get('tamano')) }) });
  });
  await page.route("**/api/v1/reportes/entradas/exportacion**", async (route) => {
    exports.push(new URL(route.request().url()));
    await route.fulfill({ status: 200, contentType: "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet", body: "xlsx-entradas-sintetico" });
  });
  const monitors = installRuntimeMonitors(page);
  await authenticateWithPermissions(page, ['REPORTES_GENERAR', 'REPORTES_EXPORTAR']);
  await mockSavedQueries(page);
  await page.goto('/reportes');
  await page.getByRole('button', { name: 'Entradas', exact: true }).click();
  await page.getByRole('button', { name: '7 días' }).click();
  await page.getByRole('textbox', { name: 'Pedimento', exact: true }).fill('PED-001');
  await page.getByRole('button', { name: 'Generar' }).click();
  await expect(page.getByText('PED-001')).toBeVisible();
  expect(requests.at(-1)!.searchParams.get('pedimento')).toBe('PED-001');
  expect(requests.at(-1)!.searchParams.get('desde')).toMatch(/^\d{4}-\d{2}-\d{2}$/);
  await page.locator('mat-paginator[aria-label="Paginación del reporte"]').getByRole('button', { name: /next page/i }).click();
  await expect(page.getByText('PED-002')).toBeVisible();
  expect(requests.at(-1)!.searchParams.get('pagina')).toBe('2');
  const download = page.waitForEvent('download');
  await page.getByRole('button', { name: 'XLSX' }).click();
  const artifact = await download;
  expect(artifact.suggestedFilename()).toBe('entradas.xlsx');
  expect(await downloadBytes(artifact)).toBeGreaterThan(0);
  expect(exports.at(-1)!.searchParams.get('pedimento')).toBe('PED-001');
  expect(exports.at(-1)!.searchParams.get('tamano')).toBe('100');
  await assertNoRuntimeFailures(monitors);
});

test('Entradas oculta exportación sin REPORTES_EXPORTAR', async ({ page }) => {
  await page.route('**/api/v1/reportes/entradas**', (route) => route.fulfill({ status: 200, contentType: 'application/json', body: JSON.stringify({ items: [{ pedimento: 'PED-001' }], total: 1, pagina: 1, tamano: 20 }) }));
  await authenticateWithPermissions(page, ['REPORTES_GENERAR']);
  await mockSavedQueries(page);
  await page.goto('/reportes');
  await page.getByRole('button', { name: '7 días' }).click();
  await page.getByRole('button', { name: 'Generar' }).click();
  await expect(page.getByText('PED-001')).toBeVisible();
  await expect(page.getByRole('button', { name: 'XLSX' })).toHaveCount(0);
});

test('Bitácora filtra, pagina, exporta CSV filtrado y muestra vacío y error controlados', async ({ page }) => {
  const requests: URL[] = [];
  let mode: 'data' | 'empty' | 'error' = 'data';
  await page.route('**/api/v1/bitacora**', async (route) => {
    const url = new URL(route.request().url());
    requests.push(url);
    if (mode === 'error') {
      await route.fulfill({ status: 503, contentType: 'application/json', body: JSON.stringify({ message: 'No disponible', correlationId: 'e2e-bitacora-error' }) });
      return;
    }
    await route.fulfill({ status: 200, contentType: 'application/json', body: JSON.stringify({
      items: mode === 'empty' ? [] : [{ id: 1, fecha: '2026-10-10T10:00:00.000Z', usuarioId: 7, usuario: 'Operador E2E', modulo: 'FACTURACION', accion: 'CARGA_VALIDADA', detalle: 'Detalle sintético', resultado: 'EXITO', correlationId: 'corr-e2e' }],
      total: mode === 'empty' ? 0 : 41,
      pagina: Number(url.searchParams.get('pagina')),
      tamano: Number(url.searchParams.get('tamano')),
    }) });
  });
  const monitors = installRuntimeMonitors(page, { ignoredConsoleErrorFragments: ['503 (Service Unavailable)'] });
  await authenticateWithPermissions(page, ['BITACORA_CONSULTAR']);
  await page.goto('/bitacora');
  await expect(page.getByText('Operador E2E')).toBeVisible();
  await page.locator('#audit-log-user-id').fill('7');
  await page.locator('#audit-log-module').selectOption('FACTURACION');
  await page.locator('#audit-log-result').selectOption('EXITO');
  await page.locator('#audit-log-correlation-id').fill('corr-e2e');
  await page.waitForTimeout(600);
  const filtered = requests.at(-1)!;
  expect(filtered.searchParams.get('usuarioId')).toBe('7');
  expect(filtered.searchParams.get('modulo')).toBe('FACTURACION');
  expect(filtered.searchParams.get('resultado')).toBe('EXITO');
  expect(filtered.searchParams.get('correlationId')).toBe('corr-e2e');
  await page.locator('mat-paginator[aria-label="Paginación de bitácora"]').getByRole('button', { name: /next page/i }).click();
  await expect.poll(() => requests.at(-1)?.searchParams.get('pagina')).toBe('2');
  const visible = await page.getByRole('table', { name: 'Eventos de bitácora' }).innerText();
  const download = page.waitForEvent('download');
  await page.getByRole('button', { name: 'Exportar bitácora a CSV' }).click();
  const artifact = await download;
  expect(artifact.suggestedFilename()).toBe('bitacora.csv');
  expect(await downloadBytes(artifact)).toBeGreaterThan(0);
  expect(await page.getByRole('table', { name: 'Eventos de bitácora' }).innerText()).toBe(visible);
  expect(requests.at(-1)!.searchParams.get('pagina')).toBe('1');
  expect(requests.at(-1)!.searchParams.get('tamano')).toBe('100');
  mode = 'empty';
  await page.getByRole('button', { name: 'Actualizar consulta de bitácora' }).click();
  await expect(page.getByText('No se encontraron eventos')).toBeVisible();
  mode = 'error';
  await page.getByRole('button', { name: 'Actualizar consulta de bitácora' }).click();
  await expect(page.getByLabel('Error de consulta')).toBeVisible();
  expect(monitors.serverErrors).toContain('GET /api/v1/bitacora 503');
  await assertNoRuntimeFailures({ ...monitors, serverErrors: [] });
});

test('Bitácora deniega ruta sin BITACORA_CONSULTAR', async ({ page }) => {
  await authenticateWithPermissions(page, []);
  await page.goto('/bitacora');
  await expect(page).toHaveURL(/\/forbidden$/);
});

test('Usuarios consulta filtros, pagina y exporta CSV sin incluir identificadores técnicos', async ({ page }) => {
  const requests: URL[] = [];
  await page.route('**/api/v1/administracion/perfiles**', (route) => route.fulfill({ status: 200, contentType: 'application/json', body: JSON.stringify({ items: [{ id: 3, nombre: 'OPERADOR', estado: 'ACTIVO', cantidadPermisos: 4 }], total: 1, pagina: 1, tamano: 100 }) }));
  await page.route('**/api/v1/administracion/usuarios**', async (route) => {
    const url = new URL(route.request().url());
    requests.push(url);
    await route.fulfill({ status: 200, contentType: 'application/json', body: JSON.stringify({
      items: [{ id: 99, clave: 'e2e01', nombre: 'Usuario E2E', correo: 'e2e@example.test', estado: 'ACTIVO', vigencia: null, perfilId: 3, perfilNombre: 'OPERADOR' }],
      total: 41,
      pagina: Number(url.searchParams.get('pagina')),
      tamano: Number(url.searchParams.get('tamano')),
    }) });
  });
  const monitors = installRuntimeMonitors(page);
  await authenticateWithPermissions(page, ['USUARIOS_ADMINISTRAR']);
  await page.goto('/usuarios');
  await expect(page.getByRole('table').getByText('Usuario E2E')).toBeVisible();
  await page.getByLabel('Clave').fill('e2e01');
  await page.getByLabel('Nombre').fill('Usuario E2E');
  await page.getByLabel('Correo').fill('e2e@example.test');
  await page.getByLabel('Estado').click();
  await page.getByRole('option', { name: 'Activo', exact: true }).click();
  await page.getByLabel('Filtrar por perfil').click();
  await page.getByRole('option', { name: 'OPERADOR', exact: true }).click();
  await page.waitForTimeout(600);
  const filtered = requests.at(-1)!;
  expect(filtered.searchParams.get('clave')).toBe('e2e01');
  expect(filtered.searchParams.get('nombre')).toBe('Usuario E2E');
  expect(filtered.searchParams.get('correo')).toBe('e2e@example.test');
  expect(filtered.searchParams.get('estado')).toBe('ACTIVO');
  expect(filtered.searchParams.get('perfilId')).toBe('3');
  await page.locator('mat-paginator[aria-label="Paginación de usuarios"]').getByRole('button', { name: /next page/i }).click();
  await expect.poll(() => requests.at(-1)?.searchParams.get('pagina')).toBe('2');
  const visible = await page.getByRole('table').getByText('Usuario E2E').innerText();
  const download = page.waitForEvent('download');
  await page.getByRole('button', { name: 'Exportar usuarios a CSV' }).click();
  const artifact = await download;
  expect(artifact.suggestedFilename()).toBe('usuarios.csv');
  expect(await downloadBytes(artifact)).toBeGreaterThan(0);
  expect(await page.getByRole('table').getByText('Usuario E2E').innerText()).toBe(visible);
  expect(requests.at(-1)!.searchParams.get('pagina')).toBe('1');
  expect(requests.at(-1)!.searchParams.get('tamano')).toBe('100');
  await page.getByRole('button', { name: 'Limpiar filtros' }).click();
  await expect.poll(() => requests.at(-1)?.searchParams.get('clave')).toBeNull();
  await assertNoRuntimeFailures(monitors);
});

test('Usuarios deniega ruta sin USUARIOS_ADMINISTRAR', async ({ page }) => {
  await authenticateWithPermissions(page, []);
  await page.goto('/usuarios');
  await expect(page).toHaveURL(/\/forbidden$/);
});
