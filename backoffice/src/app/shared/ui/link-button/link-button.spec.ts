import { TestBed } from '@angular/core/testing';
import { provideRouter } from '@angular/router';
import { expectNoAxeViolations } from '../../../../testing/axe';
import { LinkButton } from './link-button';

describe('LinkButton', () => {
  it('is a link to its url with the look of the chosen button variant', async () => {
    TestBed.configureTestingModule({ providers: [provideRouter([])] });
    const fixture = TestBed.createComponent(LinkButton);
    fixture.componentRef.setInput('label', 'Crear plan');
    fixture.componentRef.setInput('url', '/plans/new');
    fixture.componentRef.setInput('variant', 'secondary');
    await fixture.whenStable();

    const link = fixture.nativeElement.querySelector('a') as HTMLAnchorElement;
    expect(link.textContent?.trim()).toBe('Crear plan');
    expect(link.getAttribute('href')).toBe('/plans/new');
    expect(link.classList).toContain('button--secondary');
    await expectNoAxeViolations(fixture.nativeElement);
  });
});
