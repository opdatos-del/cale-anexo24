import { expect, test } from '@playwright/test';
import { assertNoRuntimeFailures, installRuntimeMonitors, login } from './fixtures';

test('mantiene el documento bloqueado y el contenido como dueño del scroll @responsive', async ({ page }) => {
  const monitors = installRuntimeMonitors(page);
  await login(page);

  const mainContent = page.getByTestId('main-content-scroll');
  await mainContent.evaluate((element) => {
    const longContent = document.createElement('div');
    longContent.style.height = '2400px';
    longContent.setAttribute('aria-hidden', 'true');
    element.append(longContent);
  });

  const sidebar = page.locator('.app-sidebar');
  const desktopSidebar = page.viewportSize()!.width >= 768;
  const initialSidebarBox = desktopSidebar ? await sidebar.boundingBox() : null;
  await mainContent.evaluate((element) => { element.scrollTop = element.scrollHeight; });
  const contentScrollTop = await mainContent.evaluate((element) => element.scrollTop);

  expect(contentScrollTop).toBeGreaterThan(0);
  expect(await page.evaluate(() => window.scrollY)).toBe(0);
  if (desktopSidebar) {
    const finalSidebarBox = await sidebar.boundingBox();
    expect(initialSidebarBox).not.toBeNull();
    expect(finalSidebarBox).not.toBeNull();
    expect(finalSidebarBox!.y).toBeCloseTo(initialSidebarBox!.y, 0);
    expect(finalSidebarBox!.height).toBeCloseTo(initialSidebarBox!.height, 0);
  }
  await assertNoRuntimeFailures(monitors);
});

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
