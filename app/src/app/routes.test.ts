import { describe, expect, it } from 'vitest'
import { DESIGN_CATALOG_PATH, resolveRoute, STAFF_CALENDAR_MOCK_PATH } from './routes'

describe('resolveRoute', () => {
  it('opens the design catalog in development', () => {
    expect(resolveRoute(DESIGN_CATALOG_PATH, true)).toBe('design-catalog')
  })

  it('never opens the design catalog outside development', () => {
    expect(resolveRoute(DESIGN_CATALOG_PATH, false)).toBe('home')
  })

  it('opens the staff calendar on the api mock only in development', () => {
    expect(resolveRoute(STAFF_CALENDAR_MOCK_PATH, true)).toBe('staff-calendar-mock')
    expect(resolveRoute(STAFF_CALENDAR_MOCK_PATH, false)).toBe('home')
  })

  it('falls back to home for other paths', () => {
    expect(resolveRoute('/', true)).toBe('home')
  })
})
