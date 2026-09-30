import { expect, test } from '@playwright/test';
import { assertNoRuntimeFailures, installRuntimeMonitors, login } from './fixtures';

test('mantiene el sidebar fijo y delega el scroll al contenido', async ({ page }) => {
  const monitors = installRuntimeMonitors(page);
  await login(page);

  const mainContent = page.getByTestId('main-content-scroll');
  const sidebar = page.locator('.app-sidebar');
  await expect(mainContent).toBeVisible();
  await expect(sidebar).toBeVisible();

  await mainContent.evaluate((element) => {
    const longContent = document.createElement('div');
    longContent.dataset['scrollTestContent'] = 'true';
    longContent.style.height = '2400px';
    longContent.setAttribute('aria-hidden', 'true');
    element.append(longContent);
  });

  const initialSidebarBox = await sidebar.boundingBox();
  const initialScroll = await mainContent.evaluate((element) => element.scrollTop);
  await mainContent.evaluate((element) => { element.scrollTop = element.scrollHeight; });
  const finalSidebarBox = await sidebar.boundingBox();
  const finalScroll = await mainContent.evaluate((element) => element.scrollTop);

  expect(initialSidebarBox).not.toBeNull();
  expect(finalSidebarBox).not.toBeNull();
  expect(finalSidebarBox!.y).toBeCloseTo(initialSidebarBox!.y, 0);
  expect(finalSidebarBox!.height).toBeCloseTo(initialSidebarBox!.height, 0);
  expect(initialScroll).toBe(0);
  expect(finalScroll).toBeGreaterThan(0);
  expect(await page.evaluate(() => window.scrollY)).toBe(0);
  await assertNoRuntimeFailures(monitors);
});
