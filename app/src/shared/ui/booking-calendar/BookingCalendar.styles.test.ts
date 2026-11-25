// @vitest-environment node
/// <reference types="node" />
import { readFileSync } from 'node:fs'
import { describe, expect, it } from 'vitest'

// jsdom has no layout, so the 44 × 44 touch area is checked on the stylesheet itself (calendar.md · Accesibilidad).
const css = readFileSync(new URL('./BookingCalendar.module.css', import.meta.url), 'utf8')

function declarationsOf(selector: string): string {
  const match = new RegExp(`(?:^|\\n)\\.${selector}\\s*\\{([^}]*)\\}`).exec(css)
  return match?.[1] ?? ''
}

describe('BookingCalendar stylesheet', () => {
  it.each(['slot', 'monthDay'])('gives every .%s the minimum touch area', (selector) => {
    const declarations = declarationsOf(selector)

    expect(declarations).toMatch(/min-width:\s*var\(--size-touch-min\)/)
    expect(declarations).toMatch(/(?:^|\s)height:\s*var\(--size-touch-min\)/)
  })

  it('stops the skeleton animation when the user prefers reduced motion', () => {
    expect(css).toMatch(/@media \(prefers-reduced-motion: reduce\)\s*\{\s*\.skeletonRow\s*\{\s*animation:\s*none/)
  })
})
