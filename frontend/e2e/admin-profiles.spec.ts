import { expect } from '@playwright/test';
import { adminLogin, adminTest, PROFILE_KEY } from './admin-fixtures';
import { assertNoRuntimeFailures, installRuntimeMonitors } from './fixtures';

adminTest.describe.serial('Administración de perfiles E2E', () => {
  adminTest('lista y filtra el perfil E2E', async ({ page, adminFixtures }) => {
    const monitors = installRuntimeMonitors(page);
    await adminLogin(page);
    await page.goto('/perfiles');
    await expect(page.getByRole('heading', { name: 'Perfiles', exact: true })).toBeVisible();
    const search = page.getByLabel('Buscar perfil');
    await search.fill(PROFILE_KEY);
    const profileButton = page.getByText(PROFILE_KEY, { exact: true }).locator('xpath=ancestor::button[1]');
    await expect(profileButton).toBeVisible();
    await profileButton.click();
    await expect(page.getByRole('heading', { name: PROFILE_KEY, exact: true })).toBeVisible();
    await expect(page.getByText(/permisos sincronizados/i)).toBeVisible();
    expect(adminFixtures.profile.id).toBeGreaterThan(0);
    await assertNoRuntimeFailures(monitors);
  });

  adminTest('edita y restaura el nombre del perfil mediante UI', async ({ page, adminFixtures }) => {
    const monitors = installRuntimeMonitors(page);
    await adminLogin(page);
    await page.goto('/perfiles');
    await page.getByLabel('Buscar perfil').fill(PROFILE_KEY);
    const profileButton = page.getByText(PROFILE_KEY, { exact: true }).locator('xpath=ancestor::button[1]');
    await expect(profileButton).toBeVisible();
    await profileButton.click();
    await expect(page.getByRole('heading', { name: PROFILE_KEY, exact: true })).toBeVisible();
    const name = page.getByRole('textbox', { name: 'Nombre del perfil', exact: true });
    const editedName = `${PROFILE_KEY} EDITED`;
    await name.fill(editedName);
    const updateResponsePromise = page.waitForResponse((response) => response.request().method() === 'PUT' && new URL(response.url()).pathname === `/api/v1/administracion/perfiles/${adminFixtures.profile.id}`);
    await page.getByRole('button', { name: 'Guardar nombre' }).click();
    const updateResponse = await updateResponsePromise;
    expect(updateResponse.status()).toBe(200);
    const updateBody = await updateResponse.json() as { id: number; nombre: string };
    expect(updateBody.id).toBe(adminFixtures.profile.id);
    expect(updateBody.nombre).toBe(editedName);
    await expect(name).toHaveValue(editedName);
    await name.fill(PROFILE_KEY);
    await page.getByRole('button', { name: 'Guardar nombre' }).click();
    await expect(name).toHaveValue(PROFILE_KEY);
    await assertNoRuntimeFailures(monitors);
  });

  adminTest('reemplaza permisos desde UI y conserva el conjunto baseline', async ({ page, adminFixtures }) => {
    const monitors = installRuntimeMonitors(page);
    await adminLogin(page);
    await page.goto('/perfiles');
    await page.getByLabel('Buscar perfil').fill(PROFILE_KEY);
    await page.getByText(PROFILE_KEY, { exact: true }).locator('xpath=ancestor::button[1]').click();
    await expect(page.getByRole('checkbox', { name: /MATERIALES_CONSULTAR/ })).toBeChecked();
    await expect(page.getByRole('checkbox', { name: /PRODUCTOS_CONSULTAR/ })).toBeChecked();
    const material = page.getByRole('checkbox', { name: /MATERIALES_CONSULTAR/ });
    await material.uncheck();
    await page.getByRole('button', { name: 'Guardar permisos' }).click();
    await expect(page.getByText('Permisos sincronizados')).toBeVisible();
    await material.check();
    await page.getByRole('button', { name: 'Guardar permisos' }).click();
    await expect(page.getByText('Permisos sincronizados')).toBeVisible();
    expect(adminFixtures.activityIds).toHaveLength(2);
    await assertNoRuntimeFailures(monitors);
  });

  adminTest('valida la transición de estado y deja el perfil activo', async ({ page }) => {
    const monitors = installRuntimeMonitors(page, { ignoredConsoleErrorFragments: ['409 (Conflict)'] });
    await adminLogin(page);
    await page.goto('/perfiles');
    await page.getByLabel('Buscar perfil').fill(PROFILE_KEY);
    await page.getByText(PROFILE_KEY, { exact: true }).locator('xpath=ancestor::button[1]').click();
    await page.getByRole('button', { name: 'Inactivar perfil' }).click();
    const confirm = page.getByRole('dialog').getByRole('button', { name: 'Inactivar perfil' });
    await confirm.click();
    const error = page.getByRole('alert');
    if (await error.count()) {
      await expect(error).toContainText(/No fue posible|administrador/i);
    } else {
      await expect(page.getByRole('button', { name: 'Activar perfil' })).toBeVisible();
      await page.getByRole('button', { name: 'Activar perfil' }).click();
      await page.getByRole('dialog').getByRole('button', { name: 'Activar perfil' }).click();
      await expect(page.getByRole('button', { name: 'Inactivar perfil' })).toBeVisible();
    }
    await assertNoRuntimeFailures(monitors);
  });
});
