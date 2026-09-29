import { existsSync, unlinkSync, writeFileSync } from 'node:fs';
import { spawn } from 'node:child_process';
import { resolve } from 'node:path';

const proxyPath = resolve('proxy.playwright.generated.json');
const target = process.env.E2E_BACKEND_URL || 'http://127.0.0.1:8080';
writeFileSync(proxyPath, JSON.stringify({ '/api': { target, secure: false, changeOrigin: true } }, null, 2));

const command = process.platform === 'win32' ? 'pnpm.cmd' : 'pnpm';
const child = spawn(command, ['exec', 'ng', 'serve', '--host', '127.0.0.1', '--port', '4300', '--proxy-config', proxyPath], {
  stdio: 'inherit',
  shell: process.platform === 'win32',
});

const cleanup = () => {
  if (existsSync(proxyPath)) unlinkSync(proxyPath);
};
process.on('SIGINT', () => { cleanup(); child.kill('SIGINT'); });
process.on('SIGTERM', () => { cleanup(); child.kill('SIGTERM'); });
child.on('exit', (code, signal) => { cleanup(); process.exit(code ?? (signal ? 1 : 0)); });
