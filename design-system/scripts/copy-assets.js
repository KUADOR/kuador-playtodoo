import { copyFile, mkdir, readdir, writeFile } from 'node:fs/promises';
import path from 'node:path';
import { DESIGN_SYSTEM_ROOT } from '../build/style-dictionary.js';
import {
  CALENDAR_FIXTURE_TARGETS,
  FONT_FACES,
  FONT_LICENSE_FILE,
  GENERATED_HEADER,
  NATIVE_FONT_TARGETS,
  WEB_FONT_TARGETS,
} from '../build/targets.js';

const FONTS_SOURCE = path.join(DESIGN_SYSTEM_ROOT, 'assets/fonts');
const FIXTURES_SOURCE = path.join(DESIGN_SYSTEM_ROOT, 'fixtures/calendar');

async function copyInto(sourceFile, targetDir, targetName = path.basename(sourceFile)) {
  await mkdir(targetDir, { recursive: true });
  await copyFile(sourceFile, path.join(targetDir, targetName));
}

function licenseName(fontDir) {
  return `${fontDir}-${FONT_LICENSE_FILE}`;
}

async function copyFonts(targetDir, extension) {
  for (const face of FONT_FACES) {
    await copyInto(path.join(FONTS_SOURCE, face.dir, `${face.file}.${extension}`), targetDir);
  }
  const fontDirs = [...new Set(FONT_FACES.map((face) => face.dir))];
  for (const fontDir of fontDirs) {
    await copyInto(path.join(FONTS_SOURCE, fontDir, FONT_LICENSE_FILE), targetDir, licenseName(fontDir));
  }
}

function fontFacesCss(relativeFontsDir) {
  const faces = FONT_FACES.map((face) =>
    [
      '@font-face {',
      `  font-family: "${face.family}";`,
      '  font-style: normal;',
      `  font-weight: ${face.weight};`,
      '  font-display: swap;',
      `  src: url("./${relativeFontsDir}/${face.file}.woff2") format("woff2");`,
      '}',
    ].join('\n'),
  );
  return [`/* ${GENERATED_HEADER} */`, ...faces, ''].join('\n');
}

async function copyWebFonts(outputRoot) {
  for (const { cssDir, fontsDir } of WEB_FONT_TARGETS) {
    await copyFonts(path.join(outputRoot, fontsDir), 'woff2');
    const relativeFontsDir = path.relative(cssDir, fontsDir).split(path.sep).join('/');
    await writeFile(path.join(outputRoot, cssDir, 'fonts.css'), fontFacesCss(relativeFontsDir));
  }
}

async function copyNativeFonts(outputRoot) {
  for (const { fontsDir } of NATIVE_FONT_TARGETS) {
    await copyFonts(path.join(outputRoot, fontsDir), 'ttf');
  }
}

async function copyCalendarFixtures(outputRoot) {
  const fixtures = (await readdir(FIXTURES_SOURCE)).filter((name) => name.endsWith('.json'));
  for (const targetDir of CALENDAR_FIXTURE_TARGETS) {
    for (const fixture of fixtures) {
      await copyInto(path.join(FIXTURES_SOURCE, fixture), path.join(outputRoot, targetDir));
    }
  }
}

/** Copies fonts (woff2 for web, ttf for Flutter) and calendar fixtures into every piece. */
export async function copyAssets(outputRoot) {
  await copyWebFonts(outputRoot);
  await copyNativeFonts(outputRoot);
  await copyCalendarFixtures(outputRoot);
}
