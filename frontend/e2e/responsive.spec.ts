import { expect, test } from '@playwright/test';
import { assertNoRuntimeFailures, installRuntimeMonitors, login } from './fixtures';

for (const route of ['/dashboard', '/reportes', '/facturacion', '/usuarios', '/operaciones/entradas']) {
  test(`sin overflow global en ${route} @responsive`, async ({ page }) => {
    const monitors = installRuntimeMonitors(page);
    await login(page);
    await page.goto(route, { waitUntil: 'domcontentloaded' });
    await page.locator('main').first().waitFor({ state: 'visible' });
    const width = await page.evaluate(() => ({ viewport: window.innerWidth, document: document.documentElement.scrollWidth }));
    if (width.document > width.viewport + 1) {
      const overflowing = await page.locator('body *').evaluateAll((elements) => elements
        .filter((element) => {
          const box = element.getBoundingClientRect();
          return box.right > document.documentElement.clientWidth + 1 || box.left < -1;
        })
        .slice(0, 8)
        .map((element) => {
          const box = element.getBoundingClientRect();
          return `${element.tagName}.${String(element.className).replace(/\s+/g, '.').slice(0, 60)} [${Math.round(box.left)},${Math.round(box.right)}]`;
        }));
      throw new Error(`${route} overflow global: viewport=${width.viewport}, document=${width.document}, elements=${overflowing.join(',')}`);
    }
    if (route === '/usuarios') {
      await expect(page.getByRole('heading', { name: 'Usuarios', exact: true })).toBeVisible();
      await expect(page.getByRole('button', { name: 'Actualizar listado de usuarios' })).toBeVisible();
      await expect(page.locator('mat-paginator')).toBeVisible();
    }
    await assertNoRuntimeFailures(monitors);
  });
}
