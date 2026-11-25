import { GENERATED_HEADER } from './targets.js';
import { toCss, toDart, toTs } from './values.js';

const INDENT = '  ';

function cssVariables({ dictionary }) {
  const declarations = dictionary.allTokens.map((token) => `${INDENT}--${token.name}: ${toCss(token)};`);
  return [`/* ${GENERATED_HEADER} */`, ':root {', ...declarations, '}', ''].join('\n');
}

function typescriptConstants({ dictionary }) {
  const entries = dictionary.allTokens.map((token) => `${INDENT}${token.name}: ${toTs(token)},`);
  return [
    `/* ${GENERATED_HEADER} */`,
    'export const tokens = {',
    ...entries,
    '} as const;',
    '',
    'export type Tokens = typeof tokens;',
    '',
  ].join('\n');
}

function dartConstants({ dictionary }) {
  const fields = dictionary.allTokens.map((token) => {
    const { type, value } = toDart(token);
    return `${INDENT}static const ${type} ${token.name} = ${value};`;
  });
  return [
    `// ${GENERATED_HEADER}`,
    "import 'package:flutter/painting.dart';",
    '',
    '/// Design tokens of Playtodoo. Map them to ThemeData in app_theme.dart.',
    'abstract final class AppTokens {',
    ...fields,
    '}',
    '',
  ].join('\n');
}

export const FORMATS = [
  { name: 'playtodoo/css', format: cssVariables },
  { name: 'playtodoo/ts', format: typescriptConstants },
  { name: 'playtodoo/dart', format: dartConstants },
];
