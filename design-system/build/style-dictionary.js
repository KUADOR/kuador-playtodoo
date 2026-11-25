import path from 'node:path';
import { fileURLToPath } from 'node:url';
import StyleDictionary from 'style-dictionary';
import { FORMATS } from './formats.js';
import { TOKEN_OUTPUTS } from './targets.js';

export const DESIGN_SYSTEM_ROOT = path.resolve(path.dirname(fileURLToPath(import.meta.url)), '..');
export const REPOSITORY_ROOT = path.resolve(DESIGN_SYSTEM_ROOT, '..');
const TOKEN_SOURCES = [path.join(DESIGN_SYSTEM_ROOT, 'tokens/**/*.json')];

function platformsFor(outputRoot) {
  return Object.fromEntries(
    TOKEN_OUTPUTS.map(({ platform, dir, file, format, nameTransform }) => [
      platform,
      {
        transforms: [nameTransform],
        buildPath: `${path.join(outputRoot, dir)}${path.sep}`,
        files: [{ destination: file, format }],
      },
    ]),
  );
}

function createStyleDictionary(platforms) {
  const styleDictionary = new StyleDictionary({
    source: TOKEN_SOURCES,
    platforms,
    log: { verbosity: 'silent' },
  });
  FORMATS.forEach((format) => styleDictionary.registerFormat(format));
  return styleDictionary;
}

/** Writes the token files of every piece under outputRoot. */
export async function buildTokens(outputRoot) {
  await createStyleDictionary(platformsFor(outputRoot)).buildAllPlatforms();
}

/** Resolved tokens keyed by their dotted DTCG path (e.g. "color.primary"). */
export async function loadResolvedTokens() {
  const styleDictionary = createStyleDictionary({ resolved: { transforms: [] } });
  const dictionary = await styleDictionary.getPlatformTokens('resolved');
  return new Map(dictionary.allTokens.map((token) => [token.path.join('.'), token]));
}
