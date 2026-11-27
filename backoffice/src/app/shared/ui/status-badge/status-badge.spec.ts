import { TestBed } from '@angular/core/testing';
import { expectNoAxeViolations } from '../../../../testing/axe';
import { StatusBadge, StatusBadgeSpec } from './status-badge';

describe('StatusBadge', () => {
  async function render(status: StatusBadgeSpec): Promise<HTMLElement> {
    const fixture = TestBed.createComponent(StatusBadge);
    fixture.componentRef.setInput('status', status);
    await fixture.whenStable();
    return fixture.nativeElement;
  }

  it('shows the text with a decorative icon and the tone of the status', async () => {
    const element = await render({ label: 'Publicado', tone: 'success', icon: 'check' });

    expect(element.textContent?.trim()).toBe('Publicado');
    expect(element.getAttribute('data-tone')).toBe('success');
    expect(element.querySelector('[aria-hidden="true"] svg')).not.toBeNull();
    await expectNoAxeViolations(element);
  });
});
