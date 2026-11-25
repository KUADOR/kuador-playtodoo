import { readFile } from 'node:fs/promises';
import path from 'node:path';
import { pathToFileURL } from 'node:url';
import { DESIGN_SYSTEM_ROOT, loadResolvedTokens } from '../build/style-dictionary.js';
import { tokenValue } from '../build/values.js';

const PAIRS_FILE = path.join(DESIGN_SYSTEM_ROOT, 'contrast-pairs.json');
const HEX_PATTERN = /^#([0-9a-f]{2})([0-9a-f]{2})([0-9a-f]{2})$/i;
const EXEMPT_KIND = 'exempt';

/** WCAG 2.x relative luminance of an sRGB channel expressed as 0–255. */
function channelLuminance(channel) {
  const normalized = channel / 255;
  return normalized <= 0.03928 ? normalized / 12.92 : ((normalized + 0.055) / 1.055) ** 2.4;
}

export function relativeLuminance(hexColor) {
  const match = HEX_PATTERN.exec(hexColor);
  if (!match) {
    throw new Error(`Contrast pairs need opaque #RRGGBB colors, got "${hexColor}"`);
  }
  const [red, green, blue] = match.slice(1).map((hex) => channelLuminance(parseInt(hex, 16)));
  return 0.2126 * red + 0.7152 * green + 0.0722 * blue;
}

export function contrastRatio(foreground, background) {
  const [lighter, darker] = [relativeLuminance(foreground), relativeLuminance(background)].sort((a, b) => b - a);
  return (lighter + 0.05) / (darker + 0.05);
}

function colorOf(tokens, tokenPath) {
  const token = tokens.get(tokenPath);
  if (!token) {
    throw new Error(`Contrast pair references unknown token "${tokenPath}"`);
  }
  return tokenValue(token);
}

/** Evaluates every pair; exempt pairs are measured but never fail. */
export function evaluatePairs({ minimumRatio, pairs }, tokens) {
  return pairs.map((pair) => {
    const ratio = contrastRatio(colorOf(tokens, pair.foreground), colorOf(tokens, pair.background));
    const minimum = minimumRatio[pair.kind] ?? null;
    if (pair.kind !== EXEMPT_KIND && minimum === null) {
      throw new Error(`Unknown contrast kind "${pair.kind}" for ${pair.foreground} on ${pair.background}`);
    }
    const passes = pair.kind === EXEMPT_KIND || ratio >= minimum;
    return { ...pair, ratio, minimum, passes };
  });
}

export async function checkContrast() {
  const definition = JSON.parse(await readFile(PAIRS_FILE, 'utf8'));
  return evaluatePairs(definition, await loadResolvedTokens());
}

function describe({ foreground, background, kind, ratio, minimum, passes, usage }) {
  const verdict = kind === EXEMPT_KIND ? 'INFO' : passes ? 'PASS' : 'FAIL';
  const required = minimum === null ? 'exempt' : `min ${minimum}:1`;
  return `${verdict}  ${ratio.toFixed(2)}:1 (${required})  ${foreground} on ${background}  · ${usage}`;
}

const isEntryPoint = import.meta.url === pathToFileURL(process.argv[1]).href;
if (isEntryPoint) {
  const results = await checkContrast();
  results.forEach((result) => console.log(describe(result)));
  const failures = results.filter((result) => !result.passes);
  if (failures.length > 0) {
    console.error(`${failures.length} contrast pair(s) below the WCAG AA minimum.`);
    process.exitCode = 1;
  }
}
