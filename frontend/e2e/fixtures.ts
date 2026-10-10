import { expect, type Page } from '@playwright/test';

export function installRuntimeMonitors(page: Page, options: { ignoredConsoleErrorFragments?: string[] } = {}) {
  const consoleErrors: string[] = [];
  const ignoredConsoleErrorFragments = options.ignoredConsoleErrorFragments ?? [];
  const pageErrors: string[] = [];
  const serverErrors: string[] = [];

  page.on('console', (message) => {
    if (message.type() === 'error' && !ignoredConsoleErrorFragments.some((fragment) => message.text().includes(fragment))) consoleErrors.push(message.text());
  });
  page.on('pageerror', (error) => pageErrors.push(error.message));
  page.on('response', (response) => {
    if (response.url().includes('/api/') && response.status() >= 500) {
      serverErrors.push(`${response.request().method()} ${new URL(response.url()).pathname} ${response.status()}`);
    }
  });

  return { consoleErrors, pageErrors, serverErrors };
}

export async function login(page: Page): Promise<void> {
  const username = process.env.E2E_USERNAME;
  const password = process.env.E2E_PASSWORD;
  if (!username || !password) {
    throw new Error('E2E_USERNAME y E2E_PASSWORD son obligatorios; no se permiten credenciales hardcodeadas.');
  }

  await page.goto('/login');
  await page.getByLabel('Usuario').fill(username);
  await page.getByRole('textbox', { name: 'Contraseña' }).fill(password);
  await page.getByRole('button', { name: 'Iniciar sesión' }).click();
  await expect(page).toHaveURL(/\/dashboard(?:$|[?/#])/);
  await expect(page.locator('main').first()).toBeVisible();
}

export async function assertNoRuntimeFailures(monitors: ReturnType<typeof installRuntimeMonitors>): Promise<void> {
  expect(monitors.pageErrors, `page errors: ${monitors.pageErrors.join(' | ')}`).toEqual([]);
  expect(monitors.consoleErrors, `console errors: ${monitors.consoleErrors.join(' | ')}`).toEqual([]);
  expect(monitors.serverErrors, `HTTP 5xx: ${monitors.serverErrors.join(' | ')}`).toEqual([]);
}


/** Permisos mínimos agregados para recorrer superficies existentes en browser con API simulada. */
export const E2E_ALL_PERMISSIONS = [
  'CATALOGOS_AUX_CONSULTAR',
  'MATERIALES_CONSULTAR',
  'PRODUCTOS_CONSULTAR',
  'ESTRUCTURAS_CONSULTAR',
  'USUARIOS_ADMINISTRAR',
  'PERFILES_ADMINISTRAR',
  'BITACORA_CONSULTAR',
  'FACTURACION_CARGAR',
  'MATERIALES_CARGAR',
  'PRODUCTOS_CARGAR',
  'CLIENTES_CARGAR',
  'PROVEEDORES_CARGAR',
  'AGENTES_CARGAR',
  'OPERACIONES_CONSULTAR',
  'PEDIMENTOS_CARGAR',
  'ACTAS_CARGAR',
  'CONSTANCIAS_CARGAR',
  'REPORTES_GENERAR',
  'REPORTES_EXPORTAR',
] as const;

/**
 * Prepara una sesión UI sintética antes de navegar. No sustituye autorización
 * backend: las pruebas de integración validan esa frontera por separado.
 */
export async function authenticateWithPermissions(page: Page, permissions: readonly string[] = E2E_ALL_PERMISSIONS): Promise<void> {
  await page.addInitScript((allowedPermissions: readonly string[]) => {
    localStorage.setItem('anexo24_token', 'e2e-synthetic-token');
    localStorage.setItem('anexo24_user_name', 'E2E Sintético');
    localStorage.setItem('anexo24_permissions', JSON.stringify(allowedPermissions));
  }, permissions);
}

/**
 * Simula sólo GET read-only requeridos por las siete rutas smoke añadidas.
 * Intercepta toda llamada `/api`; fallback de página vacía evita acceso al backend configurado.
 */
export async function mockSmokeReadApi(page: Page): Promise<void> {
  await page.route('**/api/v1/**', async (route) => {
    const request = route.request();
    const path = new URL(request.url()).pathname;

    if (request.method() !== 'GET') {
      throw new Error(`Solicitud no read-only durante smoke: ${request.method()} ${path}`);
    }

    if (path === '/api/v1/catalogos/datos-generales') {
      await route.fulfill({ status: 200, contentType: 'application/json', body: JSON.stringify({ razonSocial: 'CALE E2E', rfc: 'XAXX010101000', registroImmex: 'IMMEX-E2E', domicilioFiscal: 'Domicilio sintético' }) });
      return;
    }

    if (path === '/api/v1/consultas-guardadas') {
      await route.fulfill({ status: 200, contentType: 'application/json', body: '[]' });
      return;
    }

    if (path === '/api/v1/facturacion/plantilla') {
      await route.fulfill({ status: 200, contentType: 'application/json', body: JSON.stringify({ nombre: 'FACTURACION', version: 'E2E', hoja: 'FACTURAS', columnas: [{ nombre: 'Documento', obligatoria: true, tipo: 'TEXTO' }] }) });
      return;
    }

    if (['/api/v1/catalogos/unidades', '/api/v1/catalogos/tipos-material', '/api/v1/catalogos/almacenes', '/api/v1/catalogos/categorias', '/api/v1/catalogos/clientes', '/api/v1/catalogos/proveedores', '/api/v1/catalogos/agentes-aduanales'].includes(path)) {
      await route.fulfill({ status: 200, contentType: 'application/json', body: JSON.stringify({ items: [], total: 0, pagina: 1, tamano: 20 }) });
      return;
    }

    await route.fulfill({ status: 200, contentType: 'application/json', body: JSON.stringify({ items: [], total: 0, pagina: 1, tamano: 20 }) });
  });
}
