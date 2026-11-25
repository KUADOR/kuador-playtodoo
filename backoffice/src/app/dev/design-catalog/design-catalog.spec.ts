import { ComponentFixture, TestBed } from '@angular/core/testing';
import { expectNoAxeViolations } from '../../../testing/axe';
import { DesignCatalog } from './design-catalog';

describe('DesignCatalog', () => {
  let fixture: ComponentFixture<DesignCatalog>;

  beforeEach(async () => {
    fixture = TestBed.createComponent(DesignCatalog);
    await fixture.whenStable();
  });

  function count(selector: string): number {
    return fixture.nativeElement.querySelectorAll(selector).length;
  }

  it('shows every button variant in normal, loading and disabled states', () => {
    expect(count('[aria-labelledby="buttons-title"] app-button')).toBe(12);
    expect(count('[aria-labelledby="buttons-title"] button[aria-busy="true"]')).toBe(4);
    expect(count('[aria-labelledby="buttons-title"] button:disabled')).toBe(4);
  });

  it('shows text and select fields, including error and disabled states', () => {
    expect(count('app-field input')).toBe(4);
    expect(count('app-field select')).toBe(3);
    expect(count('[aria-invalid="true"]')).toBe(2);
  });

  it('shows every card variant in normal, selected and loading states', () => {
    expect(count('app-card')).toBe(15);
    expect(count('app-card.card--selected')).toBe(5);
    expect(count('app-card[aria-busy="true"]')).toBe(5);
  });

  it('shows the read-only calendar in every view and in loading, error and empty states', () => {
    expect(count('app-booking-calendar')).toBe(8);
    expect(count('.calendar__readonly')).toBe(8);
    expect(count('.calendar[aria-busy="true"]')).toBe(1);
    expect(count('.calendar [role="alert"]')).toBe(1);
    expect(count('.calendar__empty')).toBe(1);
  });

  it('has no axe violations', async () => {
    await expectNoAxeViolations(fixture.nativeElement);
  });
});
