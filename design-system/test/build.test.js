import assert from 'node:assert/strict';
import { mkdtemp, readFile, readdir, rm } from 'node:fs/promises';
import os from 'node:os';
import path from 'node:path';
import { after, before, describe, it } from 'node:test';
import {
  CALENDAR_FIXTURE_TARGETS,
  FONT_FACES,
  GENERATED_HEADER,
  NATIVE_FONT_TARGETS,
  TOKEN_OUTPUTS,
  WEB_FONT_TARGETS,
} from '../build/targets.js';
import { buildDesignSystem } from '../scripts/build.js';

const readText = (root, relativeFile) => readFile(path.join(root, relativeFile), 'utf8');

async function snapshot(root) {
  const entries = await readdir(root, { withFileTypes: true, recursive: true });
  const files = entries.filter((entry) => entry.isFile()).map((entry) => path.join(entry.parentPath, entry.name)).sort();
  return Promise.all(files.map(async (file) => [path.relative(root, file), (await readFile(file)).toString('base64')]));
}

describe('design system build', () => {
  let outputRoot;

  before(async () => {
    outputRoot = await mkdtemp(path.join(os.tmpdir(), 'design-system-test-'));
    await buildDesignSystem(outputRoot);
  });

  after(() => rm(outputRoot, { recursive: true, force: true }));

  it('writes every token file with the GENERATED header on its first line', async () => {
    for (const { dir, file } of TOKEN_OUTPUTS) {
      const [firstLine] = (await readText(outputRoot, path.join(dir, file))).split('\n');
      assert.ok(firstLine.includes(GENERATED_HEADER), `${dir}/${file} lacks the header`);
    }
  });

  it('follows the naming contract in CSS, TypeScript and Dart', async () => {
    const appCss = await readText(outputRoot, 'app/src/shared/design/tokens.css');
    const backofficeCss = await readText(outputRoot, 'backoffice/src/styles/tokens.css');
    const ts = await readText(outputRoot, 'app/src/shared/design/tokens.ts');
    const dart = await readText(outputRoot, 'movil/lib/core/design/tokens.g.dart');

    for (const css of [appCss, backofficeCss]) {
      assert.match(css, /--color-primary: #0B4FB3;/);
      assert.match(css, /--color-accent-strong: #277A18;/);
      assert.match(css, /--space-4: 16px;/);
      assert.match(css, /--radius-md: 10px;/);
      assert.match(css, /--shadow-card: 0px 1px 3px 0px rgba\(11, 47, 102, 0.14\);/);
      assert.match(css, /--font-family-heading: "Nunito", system-ui, sans-serif;/);
      assert.match(css, /--color-focus: #0B4FB3;/);
    }
    assert.match(ts, /colorPrimary: '#0B4FB3',/);
    assert.match(ts, /colorAccentStrong: '#277A18',/);
    assert.match(ts, /space4: '16px',/);
    assert.match(ts, /radiusMd: '10px',/);
    assert.match(ts, /fontWeightSemibold: 600,/);
    assert.match(ts, /shadowCard: '0px 1px 3px 0px rgba\(11, 47, 102, 0.14\)',/);
    assert.match(dart, /static const Color colorPrimary = Color\(0xFF0B4FB3\);/);
    assert.match(dart, /static const Color colorAccentStrong = Color\(0xFF277A18\);/);
    assert.match(dart, /static const double space4 = 16.0;/);
    assert.match(dart, /static const double radiusMd = 10.0;/);
    assert.match(dart, /static const FontWeight fontWeightExtrabold = FontWeight.w800;/);
    assert.match(dart, /static const List<BoxShadow> shadowCard = \[BoxShadow\(color: Color.fromRGBO\(11, 47, 102, 0.14\)/);
  });

  it('emits the same set of tokens for every target', async () => {
    const counts = await Promise.all(
      TOKEN_OUTPUTS.map(async ({ dir, file }) => {
        const content = await readText(outputRoot, path.join(dir, file));
        return (content.match(/^ {2}(--|static const |[a-zA-Z0-9]+: )/gm) ?? []).length;
      }),
    );
    assert.ok(counts[0] > 0);
    assert.deepEqual(new Set(counts).size, 1, `token counts differ: ${counts.join(', ')}`);
  });

  it('copies woff2 fonts and an @font-face sheet into the web pieces', async () => {
    for (const { cssDir, fontsDir } of WEB_FONT_TARGETS) {
      const fontsCss = await readText(outputRoot, path.join(cssDir, 'fonts.css'));
      const copied = await readdir(path.join(outputRoot, fontsDir));
      for (const face of FONT_FACES) {
        assert.ok(copied.includes(`${face.file}.woff2`), `${fontsDir} lacks ${face.file}.woff2`);
        assert.match(fontsCss, new RegExp(`url\\("\\./fonts/${face.file}\\.woff2"\\)`));
      }
      assert.ok(copied.includes('nunito-OFL.txt') && copied.includes('inter-OFL.txt'), 'font licences missing');
    }
  });

  it('copies ttf fonts into movil', async () => {
    for (const { fontsDir } of NATIVE_FONT_TARGETS) {
      const copied = await readdir(path.join(outputRoot, fontsDir));
      for (const face of FONT_FACES) {
        assert.ok(copied.includes(`${face.file}.ttf`), `${fontsDir} lacks ${face.file}.ttf`);
      }
    }
  });

  it('copies every calendar fixture into each piece', async () => {
    const source = (await readdir(new URL('../fixtures/calendar/', import.meta.url))).sort();
    for (const target of CALENDAR_FIXTURE_TARGETS) {
      assert.deepEqual((await readdir(path.join(outputRoot, target))).sort(), source);
    }
  });

  it('is idempotent', async () => {
    const first = await snapshot(outputRoot);
    await buildDesignSystem(outputRoot);
    assert.deepEqual(await snapshot(outputRoot), first);
  });
});
