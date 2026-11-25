import axe from 'axe-core';

/**
 * Runs axe-core on a rendered element and fails with the rule ids and targets found.
 * Colour contrast is skipped because jsdom does not compute layout or colours; the
 * contrast of every token pair is verified by `npm test` in design-system/.
 */
export async function expectNoAxeViolations(element: Element): Promise<void> {
  const results = await axe.run(element, { rules: { 'color-contrast': { enabled: false } } });
  const violations = results.violations.map(
    (violation) =>
      `${violation.id}: ${violation.nodes.map((node) => node.target.join(' ')).join(', ')}`,
  );
  expect(violations).toEqual([]);
}
