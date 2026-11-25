import path from 'node:path';
import { pathToFileURL } from 'node:url';
import { REPOSITORY_ROOT, buildTokens } from '../build/style-dictionary.js';
import { copyAssets } from './copy-assets.js';

/** Generates tokens and copies assets for every piece under outputRoot (the repository by default). */
export async function buildDesignSystem(outputRoot = REPOSITORY_ROOT) {
  await buildTokens(outputRoot);
  await copyAssets(outputRoot);
}

const isEntryPoint = import.meta.url === pathToFileURL(process.argv[1]).href;
if (isEntryPoint) {
  const outputRoot = process.argv[2] ? path.resolve(process.argv[2]) : REPOSITORY_ROOT;
  await buildDesignSystem(outputRoot);
  console.log(`Design system built into ${outputRoot}`);
}
