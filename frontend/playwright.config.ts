import { defineConfig } from '@playwright/test';

const baseURL = process.env.E2E_BASE_URL || 'http://127.0.0.1:4300';

export default defineConfig({
  testDir: './e2e',
  timeout: 45_000,
  expect: { timeout: 10_000 },
  fullyParallel: false,
  forbidOnly: !!process.env.CI,
  retries: process.env.CI ? 1 : 0,
  reporter: [['list'], ['html', { outputFolder: 'playwright-report', open: 'never' }]],
  outputDir: 'test-results',
  use: {
    baseURL,
    headless: true,
    screenshot: { mode: 'only-on-failure', fullPage: true },
    trace: 'retain-on-failure',
    video: 'off',
  },
  projects: [
    {
      name: 'chromium-desktop',
      use: { browserName: 'chromium', viewport: { width: 1440, height: 900 } },
      testIgnore: /responsive\.spec\.ts/,
    },
    {
      name: 'chromium-laptop',
      use: { browserName: 'chromium', viewport: { width: 1024, height: 768 } },
      testMatch: /responsive\.spec\.ts/,
    },
    {
      name: 'chromium-tablet',
      use: { browserName: 'chromium', viewport: { width: 768, height: 1024 } },
      testMatch: /responsive\.spec\.ts/,
    },
    {
      name: 'chromium-mobile',
      use: { browserName: 'chromium', viewport: { width: 390, height: 844 } },
      testMatch: /responsive\.spec\.ts/,
    },
  ],
  webServer: {
    command: 'node e2e/start-frontend.mjs',
    url: `${baseURL}/login`,
    timeout: 120_000,
    reuseExistingServer: !process.env.CI,
  },
});
