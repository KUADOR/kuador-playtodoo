import { Component, input } from '@angular/core';
import { ComponentFixture, TestBed } from '@angular/core/testing';
import { expectNoAxeViolations } from '../../../../testing/axe';
import { Card } from './card';

@Component({
  imports: [Card],
  template: `
    <app-card variant="booking" [selected]="selected()" [loading]="loading()">
      <p class="content">Sábado 18:00 – 19:00</p>
    </app-card>
  `,
})
class CardHost {
  readonly selected = input(false);
  readonly loading = input(false);
}

describe('Card', () => {
  let fixture: ComponentFixture<CardHost>;

  async function render(inputs: { selected?: boolean; loading?: boolean } = {}) {
    fixture = TestBed.createComponent(CardHost);
    for (const [name, value] of Object.entries(inputs)) {
      fixture.componentRef.setInput(name, value);
    }
    await fixture.whenStable();
  }

  function card(): HTMLElement {
    return fixture.nativeElement.querySelector('app-card');
  }

  it('projects its content and exposes the variant', async () => {
    await render();

    expect(card().querySelector('.content')?.textContent).toBe('Sábado 18:00 – 19:00');
    expect(card().dataset['variant']).toBe('booking');
    expect(card().classList).not.toContain('card--selected');
  });

  it('marks the selection with a check icon and text, not only colour', async () => {
    await render({ selected: true });

    const mark = card().querySelector('.card__selected-mark');
    expect(card().classList).toContain('card--selected');
    expect(mark?.querySelector('svg')).not.toBeNull();
    expect(mark?.textContent?.trim()).toBe('Seleccionada');
  });

  it('replaces the content with an announced skeleton while loading', async () => {
    await render({ loading: true });

    expect(card().getAttribute('aria-busy')).toBe('true');
    expect(card().querySelector('.content')).toBeNull();
    expect(card().querySelector('.card__skeleton')?.textContent?.trim()).toBe('Cargando…');
  });

  it.each([
    ['normal', {}],
    ['selected', { selected: true }],
    ['loading', { loading: true }],
  ])('has no axe violations in the %s state', async (_state, inputs) => {
    await render(inputs);

    await expectNoAxeViolations(fixture.nativeElement);
  });
});
