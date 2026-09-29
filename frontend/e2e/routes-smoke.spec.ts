import { test } from '@playwright/test';
import { assertNoRuntimeFailures, installRuntimeMonitors, login } from './fixtures';

const routes = [
  '/forbidden',
  '/dashboard',
  '/materiales',
  '/productos',
  '/estructuras',
  '/operaciones/entradas',
  '/operaciones/salidas',
  '/operaciones/materiales-utilizados',
  '/operaciones/activos-fijos',
  '/perfiles',
  '/usuarios',
  '/bitacora',
  '/reportes',
  '/facturacion',
] as const;

for (const route of routes) {
  test(`abre ${route}`, async ({ page }) => {
    const monitors = installRuntimeMonitors(page);
    await login(page);
    await page.goto(route, { waitUntil: 'domcontentloaded' });
    await page.locator('main').first().waitFor({ state: 'visible' });
    await assertNoRuntimeFailures(monitors);
  });
}
