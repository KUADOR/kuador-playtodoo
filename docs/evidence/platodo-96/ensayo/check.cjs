// Opens app and backoffice through the rehearsal Traefik with the real hostnames and HTTPS origins.
// Environment: CAPTURE_DIR (screenshots), SUPER_PW (password of super@example.test, never printed).
// Requires the `playwright` package (resolved through NODE_PATH) and Google Chrome.
const { chromium } = require('playwright');
const path = require('node:path');
const ROOT = 'develop.play.kuador.com';
const outDir = process.env.CAPTURE_DIR;
const HTTPS_PORT = 41001;
const SETTLE_MS = 1500;
const results = [];
const problems = [];

function watch(page, label) {
  page.on('console', (m) => { if (m.type() === 'error') problems.push(`${label} console: ${m.text()}`); });
  page.on('requestfailed', (r) => problems.push(`${label} failed: ${r.method()} ${r.url()} ${r.failure()?.errorText}`));
  page.on('response', (r) => { if (r.url().includes('/api/')) results.push(`${label} | ${r.request().method()} ${r.url()} | ${r.status()}`); });
}

(async () => {
  const browser = await chromium.launch({
    channel: 'chrome',
    args: [`--host-resolver-rules=MAP *.${ROOT} 127.0.0.1:${HTTPS_PORT}`, '--ignore-certificate-errors'],
  });
  const context = await browser.newContext({ viewport: { width: 1280, height: 800 } });

  const app = await context.newPage();
  watch(app, 'app');
  await app.goto(`https://canchas-el-sol.${ROOT}/`, { waitUntil: 'networkidle' });
  await app.screenshot({ path: path.join(outDir, 'app-canchas-el-sol.png') });
  results.push(`app | text | ${(await app.innerText('body')).replace(/\s+/g, ' ').slice(0, 160)}`);
  await app.goto(`https://canchas-el-sol.${ROOT}/ruta/interna`, { waitUntil: 'networkidle' });
  await app.reload({ waitUntil: 'networkidle' });
  results.push(`app | reload internal route | ${(await app.title())} / ${(await app.innerText('body')).replace(/\s+/g, ' ').slice(0, 120)}`);

  const bo = await context.newPage();
  watch(bo, 'backoffice');
  await bo.goto(`https://admin.${ROOT}/`, { waitUntil: 'networkidle' });
  await bo.fill('input[type=email]', 'super@example.test');
  await bo.fill('input[type=password]', process.env.SUPER_PW);
  await bo.click('button[type=submit]');
  await bo.waitForLoadState('networkidle');
  await bo.waitForTimeout(SETTLE_MS);
  await bo.screenshot({ path: path.join(outDir, 'backoffice-complejos.png') });
  results.push(`backoffice | url | ${bo.url()}`);
  results.push(`backoffice | text | ${(await bo.innerText('body')).replace(/\s+/g, ' ').slice(0, 300)}`);
  await bo.reload({ waitUntil: 'networkidle' });
  results.push(`backoffice | reload | ${bo.url()}`);
  await browser.close();
  console.log(results.join('\n'));
  console.log(`PROBLEMS: ${problems.length}`);
  console.log(problems.join('\n'));
})().catch((e) => { console.error(e); process.exit(1); });
