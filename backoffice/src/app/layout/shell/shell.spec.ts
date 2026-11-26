import { Component } from '@angular/core';
import { HttpTestingController } from '@angular/common/http/testing';
import { TestBed } from '@angular/core/testing';
import { Router, provideRouter } from '@angular/router';
import { RouterTestingHarness } from '@angular/router/testing';
import { expectNoAxeViolations } from '../../../testing/axe';
import { ME_URL, OPERATOR_USER, SUPER_ADMIN_USER } from '../../../testing/platform-fixtures';
import { provideTestApi, settle, signInAs } from '../../../testing/platform-testing';
import { CurrentPlatformUser } from '../../core/auth/platform-user';
import { Session } from '../../core/auth/session';
import { Shell } from './shell';

@Component({ template: '<h1>Página de prueba</h1>' })
class TestPage {}

describe('Shell', () => {
  let harness: RouterTestingHarness;
  let backend: HttpTestingController;

  beforeEach(() => {
    sessionStorage.clear();
    TestBed.configureTestingModule({
      providers: [
        provideTestApi(),
        provideRouter([
          {
            path: '',
            component: Shell,
            children: [
              { path: 'complexes', component: TestPage },
              { path: 'operator-permissions', component: TestPage },
            ],
          },
          { path: 'login', component: TestPage },
        ]),
      ],
    });
    backend = TestBed.inject(HttpTestingController);
  });

  afterEach(() => {
    backend.verify();
    document.body.replaceChildren();
  });

  async function renderAs(user: CurrentPlatformUser, url = '/complexes'): Promise<HTMLElement> {
    await signInAs(user);
    harness = await RouterTestingHarness.create();
    document.body.appendChild(harness.fixture.nativeElement);
    await harness.navigateByUrl(url);
    await settle();
    return harness.fixture.nativeElement;
  }

  /** jsdom does not evaluate media queries: show the top bar that the narrow layout shows. */
  function showMobileTopBar(): void {
    const style = document.createElement('style');
    style.textContent = '.shell__top-bar { display: flex !important; }';
    document.head.appendChild(style);
    onTestFinished(() => style.remove());
  }

  const menuLabels = (root: HTMLElement) =>
    Array.from(root.querySelectorAll('.shell__nav-link')).map((link) => link.textContent?.trim());

  it('shows every section, the name and the role to the super admin', async () => {
    const root = await renderAs(SUPER_ADMIN_USER);

    expect(menuLabels(root)).toEqual(['Complejos', 'Planes', 'Permisos del operador']);
    expect(root.querySelector('.shell__user')?.textContent).toContain('Sam Example');
    expect(root.querySelector('.shell__user')?.textContent).toContain('Super administrador');
  });

  it('hides the sections an operator may not open instead of disabling them (CA15)', async () => {
    const root = await renderAs(OPERATOR_USER);

    expect(menuLabels(root)).toEqual(['Complejos']);
    expect(root.querySelector('.shell__user')?.textContent).toContain('Operador');
  });

  it('marks the current section with aria-current and focuses the page heading', async () => {
    const root = await renderAs(SUPER_ADMIN_USER, '/operator-permissions');

    const current = root.querySelector('[aria-current="page"]');
    expect(current?.textContent?.trim()).toBe('Permisos del operador');
    expect(document.activeElement).toBe(root.querySelector('main h1'));
  });

  it('follows GET /me when it is reloaded (CA16)', async () => {
    const root = await renderAs(SUPER_ADMIN_USER);

    const refreshed = TestBed.inject(Session).refreshUser();
    backend.expectOne(ME_URL).flush({ ...SUPER_ADMIN_USER, role: 'OPERATOR' });
    await refreshed;
    await settle();

    expect(menuLabels(root)).toEqual(['Complejos']);
  });

  it('skips to the content from the first link', async () => {
    const root = await renderAs(OPERATOR_USER);
    const skipLink = root.querySelector<HTMLAnchorElement>('.shell__skip-link');
    (document.activeElement as HTMLElement | null)?.blur();

    skipLink?.click();

    expect(root.querySelector('a')).toBe(skipLink);
    expect(skipLink?.textContent).toBe('Saltar al contenido');
    expect(document.activeElement).toBe(root.querySelector('main h1'));
  });

  it('opens the menu as a modal panel that closes with Escape and returns the focus', async () => {
    const root = await renderAs(OPERATOR_USER);
    showMobileTopBar();
    const menuButton = root.querySelector<HTMLButtonElement>(
      '.shell__menu-button',
    ) as HTMLButtonElement;

    menuButton.click();
    await settle();
    const panel = root.querySelector('#shell-navigation') as HTMLElement;
    expect(menuButton.getAttribute('aria-expanded')).toBe('true');
    expect(panel.getAttribute('role')).toBe('dialog');
    expect(root.querySelector('main')?.hasAttribute('inert')).toBe(true);
    expect(document.activeElement).toBe(panel.querySelector('a'));

    panel.dispatchEvent(new KeyboardEvent('keydown', { key: 'Escape', bubbles: true }));
    await settle();

    expect(menuButton.getAttribute('aria-expanded')).toBe('false');
    expect(panel.hasAttribute('role')).toBe(false);
    expect(document.activeElement).toBe(menuButton);
  });

  it('signs out and goes to the login', async () => {
    const root = await renderAs(OPERATOR_USER);

    root.querySelector<HTMLButtonElement>('.shell__sign-out')?.click();
    await settle();

    expect(TestBed.inject(Session).user()).toBeNull();
    expect(TestBed.inject(Router).url).toBe('/login');
  });

  it('has no axe violations with the menu closed and open', async () => {
    const root = await renderAs(SUPER_ADMIN_USER);
    await expectNoAxeViolations(root);

    root.querySelector<HTMLButtonElement>('.shell__menu-button')?.click();
    await settle();
    await expectNoAxeViolations(root);
  });
});
