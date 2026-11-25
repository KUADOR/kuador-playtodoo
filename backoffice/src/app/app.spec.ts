import { TestBed } from '@angular/core/testing';
import { provideRouter } from '@angular/router';
import { RouterTestingHarness } from '@angular/router/testing';
import { routes } from './app.routes';
// Preloads the lazy chunk so the navigation below does not pay the module import cost.
import './dev/design-catalog/design-catalog';

describe('App routes (development build)', () => {
  beforeEach(() => {
    TestBed.configureTestingModule({ providers: [provideRouter(routes)] });
  });

  it('serves the design catalog at /design', async () => {
    const harness = await RouterTestingHarness.create();

    await harness.navigateByUrl('/design');

    expect(harness.routeNativeElement?.querySelector('h1')?.textContent).toBe('Componentes base');
  });
});
