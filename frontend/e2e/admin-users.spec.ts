import { expect } from '@playwright/test';
import { adminLogin, adminTest, USER_EMAIL, USER_KEY, USER_NAME } from './admin-fixtures';
import { assertNoRuntimeFailures, installRuntimeMonitors } from './fixtures';

const NEXT_PASSWORD = () => process.env.E2E_FIXTURE_PASSWORD_NEXT || (() => { throw new Error('E2E_FIXTURE_PASSWORD_NEXT es obligatorio'); })();

adminTest.describe.serial('Administración de usuarios E2E', () => {
  async function openFixtureActions(page: import('@playwright/test').Page, action: string): Promise<void> {
    const row = page.getByRole('row', { name: new RegExp(USER_KEY) });
    await expect(row).toBeVisible();
    await row.getByRole('button', { name: new RegExp(`Acciones para ${USER_NAME}`) }).click();
    await page.getByRole('menuitem', { name: action }).click();
  }

  adminTest('lista, filtra por clave/nombre/correo/estado/perfil y limpia filtros', async ({ page, adminFixtures }) => {
    const monitors = installRuntimeMonitors(page);
    await adminLogin(page);
    await page.goto('/usuarios');
    await expect(page.getByRole('heading', { name: 'Usuarios', exact: true })).toBeVisible();
    const key = page.getByLabel('Clave');
    const name = page.getByLabel('Nombre');
    const email = page.getByLabel('Correo');
    await key.fill(USER_KEY);
    await expect(page.getByRole('row', { name: new RegExp(USER_KEY) })).toBeVisible();
    await key.fill('');
    await name.fill(USER_NAME);
    await expect(page.getByRole('row', { name: new RegExp(USER_KEY) })).toBeVisible();
    await name.fill('');
    await email.fill(USER_EMAIL);
    await expect(page.getByRole('row', { name: new RegExp(USER_KEY) })).toBeVisible();
    await email.fill('');
    await page.getByLabel('Estado').click();
    await page.getByRole('option', { name: 'Activo', exact: true }).click();
    await expect(page.getByRole('row', { name: new RegExp(USER_KEY) })).toBeVisible();
    await page.getByLabel('Filtrar por perfil').click();
    await page.getByRole('option', { name: adminFixtures.profile.name, exact: true }).click();
    await expect(page.getByRole('row', { name: new RegExp(USER_KEY) })).toBeVisible();
    await page.getByRole('button', { name: 'Limpiar filtros' }).click();
    await expect(page.getByRole('row', { name: new RegExp(USER_KEY) })).toBeVisible();
    await assertNoRuntimeFailures(monitors);
  });

  adminTest('edita datos y restaura nombre/correo mediante UI', async ({ page }) => {
    const monitors = installRuntimeMonitors(page);
    await adminLogin(page);
    await page.goto('/usuarios');
    await openFixtureActions(page, 'Editar datos');
    await expect(page.getByRole('dialog')).toBeVisible();
    await page.getByRole('dialog').getByLabel('Nombre').fill(`${USER_NAME} Edited`);
    await page.getByRole('dialog').getByLabel('Correo').fill('e2e-automation-edited@invalid.local');
    await page.getByRole('dialog').getByRole('button', { name: 'Guardar' }).click();
    await expect(page.getByRole('row', { name: /E2E Automation User Edited/ })).toBeVisible();
    await openFixtureActions(page, 'Editar datos');
    await page.getByRole('dialog').getByLabel('Nombre').fill(USER_NAME);
    await page.getByRole('dialog').getByLabel('Correo').fill(USER_EMAIL);
    await page.getByRole('dialog').getByRole('button', { name: 'Guardar' }).click();
    await expect(page.getByRole('row', { name: new RegExp(USER_KEY) })).toContainText(USER_NAME);
    await assertNoRuntimeFailures(monitors);
  });

  adminTest('cambia vigencia, perfil y restaura baseline', async ({ page, adminFixtures }) => {
    const monitors = installRuntimeMonitors(page);
    await adminLogin(page);
    await page.goto('/usuarios');
    await openFixtureActions(page, 'Cambiar vigencia');
    await page.getByRole('dialog').getByLabel('Fecha de vigencia').fill('12/31/2099');
    await page.getByRole('dialog').getByRole('button', { name: 'Guardar' }).click();
    await expect(page.getByRole('row', { name: new RegExp(USER_KEY) })).toBeVisible();
    await openFixtureActions(page, 'Cambiar vigencia');
    await page.getByRole('dialog').getByLabel('Fecha de vigencia').fill('');
    await page.getByRole('dialog').getByRole('button', { name: 'Guardar' }).click();

    await openFixtureActions(page, 'Cambiar perfil');
    await page.getByRole('dialog').getByLabel('Perfil activo').click();
    await page.getByRole('option', { name: adminFixtures.profileAlt.name }).click();
    await page.getByRole('dialog').getByRole('button', { name: 'Guardar' }).click();
    await openFixtureActions(page, 'Cambiar perfil');
    await page.getByRole('dialog').getByLabel('Perfil activo').click();
    await page.getByRole('option', { name: adminFixtures.profile.name, exact: true }).click();
    await page.getByRole('dialog').getByRole('button', { name: 'Guardar' }).click();
    await assertNoRuntimeFailures(monitors);
  });

  adminTest('inactiva y reactiva el usuario E2E', async ({ page }) => {
    const monitors = installRuntimeMonitors(page);
    await adminLogin(page);
    await page.goto('/usuarios');
    await openFixtureActions(page, 'Inactivar usuario');
    await page.getByRole('dialog').getByRole('button', { name: 'Inactivar' }).click();
    await expect(page.getByRole('row', { name: new RegExp(USER_KEY) })).toContainText('Inactivo');
    await openFixtureActions(page, 'Activar usuario');
    await page.getByRole('dialog').getByRole('button', { name: 'Activar' }).click();
    await expect(page.getByRole('row', { name: new RegExp(USER_KEY) })).toContainText('Activo');
    await assertNoRuntimeFailures(monitors);
  });

  adminTest('restablece password sólo del fixture y permite login aislado', async ({ page }) => {
    const monitors = installRuntimeMonitors(page);
    await adminLogin(page);
    await page.goto('/usuarios');
    await openFixtureActions(page, 'Restablecer contraseña');
    const dialog = page.getByRole('dialog');
    await dialog.getByLabel('Nueva contraseña').fill(NEXT_PASSWORD());
    await dialog.getByLabel('Confirmar contraseña').fill(NEXT_PASSWORD());
    await dialog.getByRole('button', { name: 'Restablecer' }).click();
    await expect(dialog).toBeHidden();

    const fixturePage = await page.context().newPage();
    await fixturePage.goto('/login');
    await fixturePage.getByLabel('Usuario').fill(USER_KEY);
    await fixturePage.getByRole('textbox', { name: 'Contraseña' }).fill(NEXT_PASSWORD());
    await fixturePage.getByRole('button', { name: 'Iniciar sesión' }).click();
    await expect(fixturePage).toHaveURL(/\/dashboard(?:$|[?/#])/);
    await expect(fixturePage.locator('main').first()).toBeVisible();
    await fixturePage.close();
    await assertNoRuntimeFailures(monitors);
  });
});
