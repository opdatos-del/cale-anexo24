import { test, expect } from '@playwright/test';
import { assertNoRuntimeFailures, installRuntimeMonitors, login } from './fixtures';

test('bitácora abre con controles de rango', async ({ page }) => {
  const monitors = installRuntimeMonitors(page);
  await login(page);
  await page.goto('/bitacora');
  await expect(page.getByLabel('Fecha desde')).toBeVisible();
  await expect(page.getByLabel('Fecha hasta')).toBeVisible();
  await assertNoRuntimeFailures(monitors);
});
