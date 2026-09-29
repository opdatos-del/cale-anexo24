import { test } from '@playwright/test';
import { assertNoRuntimeFailures, installRuntimeMonitors, login } from './fixtures';

test('login válido y navegación inicial', async ({ page }) => {
  const monitors = installRuntimeMonitors(page);
  await login(page);
  await assertNoRuntimeFailures(monitors);
});
