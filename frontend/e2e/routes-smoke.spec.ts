import { test, expect } from '@playwright/test';
import { assertNoRuntimeFailures, authenticateWithPermissions, installRuntimeMonitors, mockSmokeReadApi } from './fixtures';

const protectedRoutes = [
  { route: '/dashboard', title: 'Hola, E2E Sintético' },
  { route: '/materiales', title: 'Materiales' },
  { route: '/productos', title: 'Productos' },
  { route: '/estructuras', title: 'Estructuras' },
  { route: '/catalogos', title: 'Catálogos auxiliares', characteristic: 'Seleccionar catálogo auxiliar' },
  { route: '/catalogos/datos-generales', title: 'Datos generales de la empresa', characteristic: 'Información empresarial' },
  { route: '/catalogos/socios-comerciales', title: 'Socios comerciales', characteristic: 'Seleccionar socio comercial' },
  { route: '/catalogos/importaciones', title: 'Importar materiales, productos, clientes, proveedores y agentes aduanales', characteristic: 'Tipo de catálogo' },
  { route: '/operaciones/entradas', title: 'Entradas' },
  { route: '/operaciones/salidas', title: 'Salidas' },
  { route: '/operaciones/materiales-utilizados', title: 'Materiales utilizados' },
  { route: '/operaciones/activos-fijos', title: 'Activos fijos' },
  { route: '/operaciones/pedimentos', title: 'Carga, validación y confirmación de pedimentos', characteristic: 'Seleccionar archivo de pedimentos' },
  { route: '/operaciones/actas', title: 'Carga, validación y confirmación de actas', characteristic: 'Seleccionar archivo de actas' },
  { route: '/operaciones/constancias', title: 'Carga, validación y confirmación de constancias', characteristic: 'Seleccionar archivo de constancias' },
  { route: '/facturacion', title: 'Carga de facturación', characteristic: 'Secciones de facturación' },
  { route: '/reportes', title: 'Reportes', characteristic: 'Tipo de reporte' },
  { route: '/usuarios', title: 'Usuarios' },
  { route: '/perfiles', title: 'Perfiles' },
  { route: '/bitacora', title: 'Bitácora' },
] as const;

test('abre /login sin sesión y muestra formulario de autenticación', async ({ page }) => {
  const monitors = installRuntimeMonitors(page);
  await page.goto('/login');
  await expect(page.getByRole('heading', { name: 'Anexo 24' })).toBeVisible();
  await expect(page.getByLabel('Usuario')).toBeVisible();
  await expect(page.getByRole('textbox', { name: 'Contraseña' })).toBeVisible();
  await assertNoRuntimeFailures(monitors);
});

test('abre /forbidden con mensaje de autorización', async ({ page }) => {
  const monitors = installRuntimeMonitors(page);
  await page.goto('/forbidden');
  await expect(page.getByRole('heading', { name: 'No tiene permiso para acceder a este módulo' })).toBeVisible();
  await expect(page.getByRole('link', { name: 'Volver al inicio' })).toBeVisible();
  await assertNoRuntimeFailures(monitors);
});

for (const item of protectedRoutes) {
  test(`abre y refresca ${item.route} con API sintética`, async ({ page }) => {
    await authenticateWithPermissions(page);
    await mockSmokeReadApi(page);
    const monitors = installRuntimeMonitors(page);

    await page.goto(item.route, { waitUntil: 'domcontentloaded' });
    await expect(page.getByRole('heading', { name: item.title, exact: true })).toBeVisible();
    if (item.characteristic) await expect(page.getByLabel(item.characteristic)).toBeVisible();

    await page.reload({ waitUntil: 'domcontentloaded' });
    await expect(page.getByRole('heading', { name: item.title, exact: true })).toBeVisible();
    if (item.characteristic) await expect(page.getByLabel(item.characteristic)).toBeVisible();
    await assertNoRuntimeFailures(monitors);
  });
}

test('pedimentos conserva guard de carga', async ({ page }) => {
  await authenticateWithPermissions(page, ['OPERACIONES_CONSULTAR']);
  await page.goto('/operaciones/pedimentos');
  await expect(page).toHaveURL(/\/forbidden$/);
  await expect(page.getByRole('heading', { name: 'No tiene permiso para acceder a este módulo' })).toBeVisible();
});
