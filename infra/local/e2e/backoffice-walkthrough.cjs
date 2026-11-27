// Browser walkthrough of the backoffice against the real api (epic PLATODO-27, task PLATODO-93).
//
// Drives the backoffice built without the development mock (`npm run start:api`) through the local proxy: login,
// complexes (P2), detail (P4), payments and history, support access with a reason (P8), support view (P9), audit (P7),
// plans (P10), plan fields (P12) and operator permissions (P13). Every screen is captured at desktop and mobile width,
// and the run fails if any api call fails at the network level (for example CORS) or answers 5xx, or if the console
// logs an error. Started by backoffice-walkthrough.sh, which prepares the data; see infra/local/e2e/README.md.
//
// Environment: BACKOFFICE_URL, LOGIN_EMAIL, LOGIN_PASSWORD (never printed), COMPLEX_ID, CAPTURE_DIR.
// Requires the `playwright` package (resolved through NODE_PATH) and its Chromium.
const { chromium } = require('playwright');
const fs = require('node:fs');
const path = require('node:path');

const SCREENS = { desktop: { width: 1280, height: 800 }, mobile: { width: 390, height: 844 } };
const SUPPORT_REASON = 'Revisión de reservas pedida por el complejo (recorrido E2E)';
const SETTLE_MS = 800;

function required(name) {
  const value = process.env[name];
  if (!value) throw new Error(`Set ${name}`);
  return value;
}

const baseUrl = required('BACKOFFICE_URL');
const email = required('LOGIN_EMAIL');
const password = required('LOGIN_PASSWORD');
const complexId = required('COMPLEX_ID');
const captureDir = required('CAPTURE_DIR');

const steps = [];
const problems = [];

async function capture(page, name) {
  await page.waitForLoadState('networkidle');
  await page.waitForTimeout(SETTLE_MS);
  for (const [screen, size] of Object.entries(SCREENS)) {
    await page.setViewportSize(size);
    await page.waitForTimeout(SETTLE_MS);
    await page.screenshot({ path: path.join(captureDir, `${name}-${screen === 'desktop' ? 'escritorio' : 'movil'}.png`), fullPage: true });
  }
  await page.setViewportSize(SCREENS.desktop);
}

async function step(description, action) {
  try {
    await action();
    steps.push({ description, result: 'OK' });
  } catch (error) {
    steps.push({ description, result: `FALLA: ${error.message.split('\n')[0]}` });
  }
}

function watchApi(page) {
  page.on('requestfailed', (request) => {
    if (request.url().includes('/api/')) problems.push(`network ${request.method()} ${request.url()}: ${request.failure()?.errorText}`);
  });
  page.on('response', (response) => {
    if (response.url().includes('/api/') && response.status() >= 500) problems.push(`${response.status()} ${response.url()}`);
  });
  page.on('console', (message) => {
    if (message.type() === 'error') problems.push(`console: ${message.text()}`);
  });
}

async function expectVisible(page, text) {
  await page.getByText(text, { exact: false }).first().waitFor({ state: 'visible', timeout: 10_000 });
}

(async () => {
  fs.mkdirSync(captureDir, { recursive: true });
  const browser = await chromium.launch();
  const page = await browser.newPage({ viewport: SCREENS.desktop, locale: 'es' });
  watchApi(page);

  await step('Inicio de sesión del SUPER_ADMIN contra la api real', async () => {
    await page.goto(`${baseUrl}/login`);
    await page.getByLabel('Correo').fill(email);
    await page.getByLabel('Contraseña').fill(password);
    await page.getByRole('button', { name: 'Iniciar sesión' }).click();
    await page.waitForURL('**/complexes');
  });
  await step('P2 Complejos: listado real con plan, estado y último pago (CA19)', async () => {
    await expectVisible(page, 'Complejo Backoffice E2E');
    await capture(page, 'p2-complejos');
  });
  await step('P4 Detalle del complejo', async () => {
    await page.goto(`${baseUrl}/complexes/${complexId}`);
    await expectVisible(page, 'Acceder a datos del complejo');
    await capture(page, 'p4-detalle');
  });
  await step('P4 Pagos (CA7)', async () => {
    await page.goto(`${baseUrl}/complexes/${complexId}/payments`);
    await expectVisible(page, 'Transferencia');
    await capture(page, 'p4-pagos');
  });
  await step('P6 Historial de plan y estado (CA5, CA6)', async () => {
    await page.goto(`${baseUrl}/complexes/${complexId}/history`);
    await page.waitForLoadState('networkidle');
    await capture(page, 'p6-historial');
  });
  await step('P8 Acceso con motivo: abre la sesión de soporte (CA13)', async () => {
    await page.goto(`${baseUrl}/complexes/${complexId}`);
    await page.getByRole('button', { name: 'Acceder a datos del complejo' }).click();
    await page.getByLabel('Motivo del acceso').fill(SUPPORT_REASON);
    await capture(page, 'p8-motivo');
    await page.getByRole('button', { name: 'Acceder', exact: true }).click();
    await page.waitForURL('**/support');
  });
  await step('P9 Vista de soporte en el host del complejo con X-Support-Session', async () => {
    await expectVisible(page, 'Reserva del recorrido backoffice');
    await capture(page, 'p9-soporte');
  });
  await step('P7 Auditoría con persona, motivo y resultado (CA13)', async () => {
    await page.goto(`${baseUrl}/complexes/${complexId}/audit`);
    await expectVisible(page, SUPPORT_REASON);
    await capture(page, 'p7-auditoria');
  });
  await step('P10 Planes', async () => {
    await page.goto(`${baseUrl}/plans`);
    await expectVisible(page, 'Plan Backoffice E2E');
    await capture(page, 'p10-planes');
  });
  await step('P12 Campos de plan (CA4)', async () => {
    await page.goto(`${baseUrl}/plans/fields`);
    await page.waitForLoadState('networkidle');
    await capture(page, 'p12-campos');
  });
  await step('P13 Permisos del operador (CA16)', async () => {
    await page.goto(`${baseUrl}/operator-permissions`);
    await page.waitForLoadState('networkidle');
    await capture(page, 'p13-permisos');
  });
  await step('Sin fallas de red, CORS, 5xx ni errores de consola', async () => {
    if (problems.length > 0) throw new Error(problems.join(' | '));
  });

  await browser.close();
  console.log('| # | Paso | Resultado |');
  console.log('|---|---|---|');
  steps.forEach((s, i) => console.log(`| ${i + 1} | ${s.description} | ${s.result} |`));
  const failed = steps.filter((s) => s.result !== 'OK').length;
  console.log();
  console.log(failed ? `**Resultado: ${failed} de ${steps.length} pasos fallaron.**` : `**Resultado: los ${steps.length} pasos dieron lo esperado.**`);
  process.exitCode = failed ? 1 : 0;
})();
