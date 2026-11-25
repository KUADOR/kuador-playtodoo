// @vitest-environment node
/// <reference types="node" />
import { readdirSync, readFileSync } from 'node:fs'
import { fileURLToPath } from 'node:url'
import { describe, expect, it } from 'vitest'

// Every stylesheet of the shared components must take its colors from design tokens (var(--color-…)).
const SHARED_UI_DIR = fileURLToPath(new URL('.', import.meta.url))

const stylesheets: [string, string][] = readdirSync(SHARED_UI_DIR, { recursive: true, encoding: 'utf8' })
  .filter((path) => path.endsWith('.css'))
  .map((path) => [path, readFileSync(`${SHARED_UI_DIR}${path}`, 'utf8')])

const NAMED_COLORS = [
  'black', 'white', 'red', 'green', 'blue', 'yellow', 'orange', 'purple', 'pink', 'gray', 'grey',
  'navy', 'teal', 'lime', 'silver', 'maroon', 'olive', 'aqua', 'fuchsia', 'brown',
]

const COLOR_LITERALS: { name: string; pattern: RegExp }[] = [
  { name: 'hex color', pattern: /#[0-9a-f]{3,8}\b/i },
  { name: 'color function', pattern: /\b(?:rgba?|hsla?|hwb|lab|lch|oklab|oklch|color)\(/i },
  { name: 'named color', pattern: new RegExp(`:[^;{}]*(?<![\\w-])(?:${NAMED_COLORS.join('|')})(?![\\w-])`, 'i') },
]

function withoutComments(css: string): string {
  return css.replace(/\/\*[\s\S]*?\*\//g, '')
}

describe('shared/ui stylesheets', () => {
  it('are found by the check', () => {
    expect(stylesheets.map(([path]) => path)).toEqual(
      expect.arrayContaining(['a11y.module.css', 'button/Button.module.css', 'field/Field.module.css', 'card/Card.module.css']),
    )
  })

  it.each(stylesheets)('%s has no color literals', (_path, css) => {
    const source = withoutComments(css)
    const found = COLOR_LITERALS.filter(({ pattern }) => pattern.test(source)).map(({ name }) => name)

    expect(found).toEqual([])
  })

  it('detects color literals', () => {
    const offending = ['.a { color: #0B4FB3; }', '.a { color: rgb(0 0 0); }', '.a { border: 1px solid white; }']

    for (const css of offending) {
      expect(COLOR_LITERALS.some(({ pattern }) => pattern.test(css))).toBe(true)
    }
  })

  it('accepts token references whose names contain a color word', () => {
    const css = '.a { box-shadow: inset 0 0 0 var(--border-width-md) var(--color-brand-navy); }'

    expect(COLOR_LITERALS.some(({ pattern }) => pattern.test(css))).toBe(false)
  })
})
