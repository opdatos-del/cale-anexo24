/**
 * Renderiza los diagramas HTML versionados (diagramas/*.html) a PNG de alta
 * resolución para el DOCX del reporte de estadía.
 *
 * No modifica el diagrama: sólo lo captura en modo impresión (oculta el chrome
 * del visor cuando el propio diagrama lo declara con `no-print`).
 *
 * Uso (desde la raíz del repositorio, con el frontend instalado):
 *   1) python -m http.server 4600 --bind 127.0.0.1        (en otra terminal)
 *   2) node scripts/report/render-diagrams.mjs
 *
 * Variables de entorno:
 *   DIAGRAMS_BASE  URL base del servidor local (default http://127.0.0.1:4600)
 */
import { createRequire } from 'node:module';
import fs from 'node:fs';
import path from 'node:path';
import { fileURLToPath } from 'node:url';

const __dirname = path.dirname(fileURLToPath(import.meta.url));
const ROOT = path.resolve(__dirname, '..', '..');
const require = createRequire(path.join(ROOT, 'frontend', 'package.json'));
const { chromium } = require('@playwright/test');

const OUT = path.join(ROOT, 'docs', '08-reporte-estadia', 'anexos', 'figuras');
const BASE = process.env.DIAGRAMS_BASE || 'http://127.0.0.1:4600';

const targets = [
  { html: 'diagramas/arquitectura-anexo24.html', png: 'arquitectura-general.png' },
  { html: 'diagramas/carga-facturacion.html', png: 'pipeline-carga-validada.png' },
];

fs.mkdirSync(OUT, { recursive: true });
const browser = await chromium.launch({ channel: 'msedge', headless: true });
const context = await browser.newContext({ viewport: { width: 1680, height: 1050 }, deviceScaleFactor: 2 });
const page = await context.newPage();

for (const target of targets) {
  await page.goto(`${BASE}/${target.html}`, { waitUntil: 'load', timeout: 60000 });
  await page.waitForTimeout(3500);
  await page.emulateMedia({ media: 'print' });
  await page.waitForTimeout(800);
  await page.screenshot({ path: path.join(OUT, target.png), fullPage: true });
  console.log(`DIAGRAM|${target.png}|saved`);
}

await browser.close();
console.log('DIAGRAMS_DONE');
