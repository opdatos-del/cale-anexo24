import { test, expect } from '@playwright/test';
import { assertNoRuntimeFailures, installRuntimeMonitors, login } from './fixtures';

const template = {
  nombre: 'FACTURACION',
  version: 'LEGACY-2026-09',
  hoja: 'FACTURAS',
  columnas: [{ nombre: 'Documento', obligatoria: true, tipo: 'TEXTO' }],
};

test('facturación carga plantilla y mantiene confirmación bloqueada', async ({ page }) => {
  const monitors = installRuntimeMonitors(page);
  await login(page);
  await page.goto('/facturacion');
  await expect(page.getByRole('button', { name: 'Descargar layout' })).toBeEnabled();

  await expect(page.getByRole('button', { name: /Confirmar|Guardar/ })).toHaveCount(0);
  await assertNoRuntimeFailures(monitors);
});

test('facturación muestra y recupera error de plantilla 409', async ({ page }) => {
  let attempts = 0;
  await page.route('**/api/v1/facturacion/plantilla', async (route) => {
    attempts += 1;
    if (attempts === 1) {
      await route.fulfill({ status: 409, contentType: 'application/json', body: JSON.stringify({ code: 'FACTURACION_PLANTILLA_NO_CONFIGURADA', message: 'No existe una plantilla activa de Facturación.', correlationId: 'e2e-template-error' }) });
      return;
    }
    await route.fulfill({ status: 200, contentType: 'application/json', body: JSON.stringify(template) });
  });
  const monitors = installRuntimeMonitors(page, { ignoredConsoleErrorFragments: ['409 (Conflict)'] });
  await login(page);
  await page.goto('/facturacion');
  await expect(page.getByRole('alert')).toContainText('e2e-template-error');
  await expect(page.getByLabel('Seleccionar archivos Excel')).toBeDisabled();
  await expect(page.getByRole('button', { name: 'Descargar layout' })).toBeDisabled();
  await page.getByRole('button', { name: 'Reintentar' }).click();
  await expect(page.getByRole('button', { name: 'Descargar layout' })).toBeEnabled();
  await expect(page.getByLabel('Seleccionar archivos Excel')).toBeEnabled();
  await assertNoRuntimeFailures(monitors);
});
