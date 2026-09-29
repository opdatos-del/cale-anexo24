import { expect, type Page } from '@playwright/test';

export function installRuntimeMonitors(page: Page, options: { ignoredConsoleErrorFragments?: string[] } = {}) {
  const consoleErrors: string[] = [];
  const ignoredConsoleErrorFragments = options.ignoredConsoleErrorFragments ?? [];
  const pageErrors: string[] = [];
  const serverErrors: string[] = [];

  page.on('console', (message) => {
    if (message.type() === 'error' && !ignoredConsoleErrorFragments.some((fragment) => message.text().includes(fragment))) consoleErrors.push(message.text());
  });
  page.on('pageerror', (error) => pageErrors.push(error.message));
  page.on('response', (response) => {
    if (response.url().includes('/api/') && response.status() >= 500) {
      serverErrors.push(`${response.request().method()} ${new URL(response.url()).pathname} ${response.status()}`);
    }
  });

  return { consoleErrors, pageErrors, serverErrors };
}

export async function login(page: Page): Promise<void> {
  const username = process.env.E2E_USERNAME;
  const password = process.env.E2E_PASSWORD;
  if (!username || !password) {
    throw new Error('E2E_USERNAME y E2E_PASSWORD son obligatorios; no se permiten credenciales hardcodeadas.');
  }

  await page.goto('/login');
  await page.getByLabel('Usuario').fill(username);
  await page.getByRole('textbox', { name: 'Contraseña' }).fill(password);
  await page.getByRole('button', { name: 'Iniciar sesión' }).click();
  await expect(page).toHaveURL(/\/dashboard(?:$|[?/#])/);
  await expect(page.locator('main').first()).toBeVisible();
}

export async function assertNoRuntimeFailures(monitors: ReturnType<typeof installRuntimeMonitors>): Promise<void> {
  expect(monitors.pageErrors, `page errors: ${monitors.pageErrors.join(' | ')}`).toEqual([]);
  expect(monitors.consoleErrors, `console errors: ${monitors.consoleErrors.join(' | ')}`).toEqual([]);
  expect(monitors.serverErrors, `HTTP 5xx: ${monitors.serverErrors.join(' | ')}`).toEqual([]);
}
