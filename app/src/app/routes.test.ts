import { describe, expect, it } from 'vitest'
import { DESIGN_CATALOG_PATH, resolveRoute } from './routes'

describe('resolveRoute', () => {
  it('opens the design catalog in development', () => {
    expect(resolveRoute(DESIGN_CATALOG_PATH, true)).toBe('design-catalog')
  })

  it('never opens the design catalog outside development', () => {
    expect(resolveRoute(DESIGN_CATALOG_PATH, false)).toBe('home')
  })

  it('falls back to home for other paths', () => {
    expect(resolveRoute('/', true)).toBe('home')
  })
})
