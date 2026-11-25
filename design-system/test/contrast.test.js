import assert from 'node:assert/strict';
import { describe, it } from 'node:test';
import { checkContrast, contrastRatio, evaluatePairs } from '../scripts/contrast-check.js';

const colorTokens = (entries) => new Map(Object.entries(entries).map(([path, $value]) => [path, { $value }]));
const MINIMUMS = { text: 4.5, component: 3 };

describe('contrast check', () => {
  it('computes WCAG ratios', () => {
    assert.equal(contrastRatio('#000000', '#FFFFFF'), 21);
    assert.equal(contrastRatio('#FFFFFF', '#FFFFFF'), 1);
    assert.equal(contrastRatio('#FFFFFF', '#000000'), contrastRatio('#000000', '#FFFFFF'));
  });

  it('fails a text pair below 4.5:1 and passes a component pair above 3:1', () => {
    const tokens = colorTokens({ fg: '#777777', bg: '#FFFFFF' });
    const [text, component] = evaluatePairs(
      {
        minimumRatio: MINIMUMS,
        pairs: [
          { foreground: 'fg', background: 'bg', kind: 'text' },
          { foreground: 'fg', background: 'bg', kind: 'component' },
        ],
      },
      tokens,
    );
    assert.equal(text.passes, false);
    assert.equal(component.passes, true);
  });

  it('reports exempt pairs without failing them', () => {
    const tokens = colorTokens({ fg: '#EEEEEE', bg: '#FFFFFF' });
    const [result] = evaluatePairs(
      { minimumRatio: MINIMUMS, pairs: [{ foreground: 'fg', background: 'bg', kind: 'exempt' }] },
      tokens,
    );
    assert.equal(result.passes, true);
    assert.equal(result.minimum, null);
  });

  it('rejects unknown tokens and kinds', () => {
    const tokens = colorTokens({ fg: '#000000', bg: '#FFFFFF' });
    assert.throws(() =>
      evaluatePairs({ minimumRatio: MINIMUMS, pairs: [{ foreground: 'nope', background: 'bg', kind: 'text' }] }, tokens),
    );
    assert.throws(() =>
      evaluatePairs({ minimumRatio: MINIMUMS, pairs: [{ foreground: 'fg', background: 'bg', kind: 'large' }] }, tokens),
    );
  });

  it('resolves every pair of contrast-pairs.json against the tokens', async () => {
    const results = await checkContrast();
    assert.ok(results.length > 0);
    results.forEach((result) => assert.ok(Number.isFinite(result.ratio), `${result.foreground} on ${result.background}`));
  });
});
