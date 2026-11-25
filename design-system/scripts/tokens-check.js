import { mkdtemp, readFile, readdir, rm } from 'node:fs/promises';
import os from 'node:os';
import path from 'node:path';
import { pathToFileURL } from 'node:url';
import { REPOSITORY_ROOT } from '../build/style-dictionary.js';
import { buildDesignSystem } from './build.js';

async function listFiles(dir) {
  const entries = await readdir(dir, { withFileTypes: true, recursive: true });
  return entries
    .filter((entry) => entry.isFile())
    .map((entry) => path.relative(dir, path.join(entry.parentPath, entry.name)))
    .sort();
}

async function readOrNull(file) {
  try {
    return await readFile(file);
  } catch (error) {
    if (error.code === 'ENOENT') {
      return null;
    }
    throw error;
  }
}

/**
 * Regenerates everything into a temporary directory and lists the committed files
 * that are missing or differ from that fresh output.
 */
export async function findOutdatedFiles(repositoryRoot = REPOSITORY_ROOT) {
  const freshRoot = await mkdtemp(path.join(os.tmpdir(), 'design-system-check-'));
  try {
    await buildDesignSystem(freshRoot);
    const outdated = [];
    for (const relativeFile of await listFiles(freshRoot)) {
      const expected = await readFile(path.join(freshRoot, relativeFile));
      const committed = await readOrNull(path.join(repositoryRoot, relativeFile));
      if (!committed || !committed.equals(expected)) {
        outdated.push(relativeFile);
      }
    }
    return outdated;
  } finally {
    await rm(freshRoot, { recursive: true, force: true });
  }
}

const isEntryPoint = import.meta.url === pathToFileURL(process.argv[1]).href;
if (isEntryPoint) {
  const outdated = await findOutdatedFiles();
  if (outdated.length > 0) {
    console.error('Generated files are out of date. Run `npm run build` in design-system/ and commit:');
    outdated.forEach((file) => console.error(`  - ${file}`));
    process.exitCode = 1;
  } else {
    console.log('Generated files are up to date.');
  }
}
