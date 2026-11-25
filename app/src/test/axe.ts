import axe from 'axe-core'
import { expect } from 'vitest'

// jsdom has no layout nor canvas, so axe cannot compute contrast here. Token contrast is checked by
// `npm test` in design-system/ (contrast-pairs.json, WCAG formula).
const RULES_NEEDING_LAYOUT = { 'color-contrast': { enabled: false } }

/** Runs axe on a rendered container and fails with the rule ids of any violation. */
export async function expectNoA11yViolations(container: Element): Promise<void> {
  const results = await axe.run(container, { rules: RULES_NEEDING_LAYOUT })
  const violations = results.violations.map((violation) => `${violation.id}: ${violation.help}`)
  expect(violations).toEqual([])
}
