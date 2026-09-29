import { expect } from '@playwright/test';
import { adminTest, loginWithCredentials } from './admin-fixtures';
import { assertNoRuntimeFailures, installRuntimeMonitors } from './fixtures';

const BACKEND_URL = process.env.E2E_BACKEND_URL || 'http://127.0.0.1:8080';
const FIXTURE_PASSWORD = process.env.E2E_FIXTURE_PASSWORD;

adminTest.describe.serial('Autorización del fixture administrativo E2E', () => {
  adminTest('permite Materiales y rechaza Usuarios en UI y API', async ({ page, playwright, adminFixtures }) => {
    if (!FIXTURE_PASSWORD) throw new Error('E2E_FIXTURE_PASSWORD es obligatorio y debe existir sólo en el entorno del proceso.');
    const monitors = installRuntimeMonitors(page);
    const request = await playwright.request.newContext({ baseURL: BACKEND_URL });
    try {
      const token = await loginWithCredentials(request, 'E2E_AUTOMATION_USER', FIXTURE_PASSWORD);
      const headers = { Authorization: `Bearer ${token}` };
      const usersResponse = await request.get('/api/v1/administracion/usuarios?pagina=1&tamano=1', { headers });
      expect(usersResponse.status()).toBe(403);

      await page.goto('/login');
      await page.getByLabel('Usuario').fill('E2E_AUTOMATION_USER');
      await page.getByRole('textbox', { name: 'Contraseña' }).fill(FIXTURE_PASSWORD);
      await page.getByRole('button', { name: 'Iniciar sesión' }).click();
      await expect(page).toHaveURL(/\/dashboard(?:$|[?/#])/);
      await expect(page.locator('main').first()).toBeVisible();

      await page.goto('/materiales');
      await expect(page).toHaveURL(/\/materiales(?:$|[?/#])/);
      await expect(page.locator('main').first()).toBeVisible();

      await page.goto('/usuarios');
      await expect(page).toHaveURL(/\/forbidden(?:$|[?/#])/);
      await expect(page.locator('main').first()).toBeVisible();
      await assertNoRuntimeFailures(monitors);
      expect(adminFixtures.profile.id).toBeGreaterThan(0);
    } finally {
      await request.dispose();
    }
  });
});
