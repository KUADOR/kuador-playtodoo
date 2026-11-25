import assert from 'node:assert/strict';
import { describe, it } from 'node:test';
import { loadResolvedTokens } from '../build/style-dictionary.js';
import { tokenValue } from '../build/values.js';

/** Values of DESIGN.md v2 (identity derived from the official logo). */
const IDENTITY_V2 = {
  'color.brand-navy': '#0B2F66',
  'color.primary': '#0B4FB3',
  'color.primary-hover': '#093F8F',
  'color.primary-tint': '#E8F0FB',
  'color.accent': '#4DB82A',
  'color.accent-strong': '#277A18',
  'color.accent-tint': '#E3F5DC',
  'color.surface': '#FFFFFF',
  'color.surface-alt': '#F3F6FA',
  'color.fg': '#111827',
  'color.fg-muted': '#4B5563',
  'color.border': '#C7D0DC',
  'color.success': '#15803D',
  'color.warning': '#B45309',
  'color.danger': '#B91C1C',
  'color.focus': '#0B4FB3',
  'color.focus-halo': '#9CC0F5',
  'color.disabled-bg': '#E5E7EB',
  'color.disabled-fg': '#4B5563',
  'color.disabled-border': '#9CA3AF',
  'color.occupied-bg': '#E5E7EB',
  'font-size.xs': '12px',
  'font-size.sm': '14px',
  'font-size.md': '16px',
  'font-size.lg': '20px',
  'font-size.xl': '24px',
  'font-size.2xl': '32px',
  'font-weight.regular': 400,
  'font-weight.semibold': 600,
  'font-weight.extrabold': 800,
  'line-height.body': 1.5,
  'line-height.heading': 1.25,
  'space.1': '4px',
  'space.2': '8px',
  'space.3': '12px',
  'space.4': '16px',
  'space.5': '24px',
  'space.6': '32px',
  'space.7': '48px',
  'radius.md': '10px',
  'radius.lg': '16px',
  'size.touch-min': '44px',
  'size.content-max': '1200px',
  'border.focus-ring': '2px',
  'border.focus-offset': '2px',
};

describe('design tokens', () => {
  it('match the values of the visual identity v2', async () => {
    const tokens = await loadResolvedTokens();
    for (const [tokenPath, expected] of Object.entries(IDENTITY_V2)) {
      assert.ok(tokens.has(tokenPath), `missing token ${tokenPath}`);
      assert.equal(tokenValue(tokens.get(tokenPath)), expected, tokenPath);
    }
  });

  it('declare Nunito for headings and Inter for body text', async () => {
    const tokens = await loadResolvedTokens();
    assert.equal(tokenValue(tokens.get('font-family.heading'))[0], 'Nunito');
    assert.equal(tokenValue(tokens.get('font-family.body'))[0], 'Inter');
  });

  it('keep the shadows of the identity', async () => {
    const tokens = await loadResolvedTokens();
    assert.deepEqual(tokenValue(tokens.get('shadow.card')), {
      offsetX: '0px', offsetY: '1px', blur: '3px', spread: '0px', color: 'rgba(11, 47, 102, 0.14)',
    });
    assert.deepEqual(tokenValue(tokens.get('shadow.overlay')), {
      offsetX: '0px', offsetY: '8px', blur: '24px', spread: '0px', color: 'rgba(11, 47, 102, 0.2)',
    });
  });

  it('give every token a DTCG type', async () => {
    const tokens = await loadResolvedTokens();
    for (const [tokenPath, token] of tokens) {
      assert.ok(token.$type, `token ${tokenPath} has no $type`);
    }
  });
});
