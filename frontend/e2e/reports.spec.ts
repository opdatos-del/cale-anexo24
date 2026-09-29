import { test, expect } from '@playwright/test';
import { assertNoRuntimeFailures, installRuntimeMonitors, login } from './fixtures';

test('reportes expone opciones disponibles y mantiene Saldos bloqueado', async ({ page }) => {
  const monitors = installRuntimeMonitors(page);
  await login(page);
  await page.goto('/reportes');
  await expect(page.getByRole('button', { name: 'Entradas' })).toBeVisible();
  await expect(page.getByRole('button', { name: 'Salidas' })).toBeVisible();
  await expect(page.getByRole('button', { name: 'Materiales utilizados' })).toBeVisible();
  await expect(page.getByRole('button', { name: 'Bitácora' })).toBeVisible();
  await expect(page.getByRole('button', { name: /Saldos/ })).toBeDisabled();
  await assertNoRuntimeFailures(monitors);
});
