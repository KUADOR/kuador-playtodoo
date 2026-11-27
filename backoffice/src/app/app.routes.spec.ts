import { HttpTestingController } from '@angular/common/http/testing';
import { TestBed } from '@angular/core/testing';
import { Router, provideRouter } from '@angular/router';
import { RouterTestingHarness } from '@angular/router/testing';
import {
  CAPABILITIES_URL,
  CAPABILITY_CHANGES_URL,
  INITIAL_CAPABILITIES,
  ME_URL,
  OPERATOR_USER,
  PLAN_FIELDS_URL,
  PLAN_OPTIONS,
  PLAN_OPTIONS_URL,
  SUPER_ADMIN_USER,
  pageOf,
  storeAccessToken,
} from '../testing/platform-fixtures';
import { COMPLEXES_URL, EMPTY_PAGE, PLANS_URL } from '../testing/complex-fixtures';
import { provideTestApi, settle } from '../testing/platform-testing';
import { CurrentPlatformUser } from './core/auth/platform-user';
import { routes } from './app.routes';
// Preload the lazy chunks so navigations do not pay the module import cost inside the tests.
import './layout/shell/shell';
import './features/login/login-page';
import './features/forbidden/forbidden-page';
import './features/upcoming-section/upcoming-section-page';
import './features/operator-permissions/operator-permissions-page';
import './features/plans/plans-page/plans-page';
import './features/plans/plan-editor-page/plan-editor-page';
import './features/plans/plan-fields-page/plan-fields-page';
import './features/complexes/list/complexes-page';
import './features/complexes/register/register-complex-page';

describe('App routes and guards', () => {
  let harness: RouterTestingHarness;
  let backend: HttpTestingController;

  beforeEach(async () => {
    sessionStorage.clear();
    TestBed.configureTestingModule({ providers: [provideTestApi(), provideRouter(routes)] });
    backend = TestBed.inject(HttpTestingController);
    harness = await RouterTestingHarness.create();
  });

  afterEach(() => {
    // «Complejos» (P2) loads its list on every navigation that lands there.
    backend
      .match((request) => request.url === COMPLEXES_URL)
      .forEach((request) => request.flush(EMPTY_PAGE));
    backend.verify();
  });

  /** Navigates as `user` (answering the `GET /me` of the guards) and returns the final URL. */
  async function navigateAs(user: CurrentPlatformUser | null, url: string): Promise<string> {
    if (user !== null) {
      storeAccessToken();
    }
    const navigation = harness.navigateByUrl(url);
    if (user !== null) {
      await vi.waitFor(() => backend.expectOne(ME_URL).flush(user));
    }
    await navigation;
    await settle();
    return TestBed.inject(Router).url;
  }

  const heading = () => harness.fixture.nativeElement.querySelector('h1')?.textContent?.trim();

  it('sends anyone without a session to the login', async () => {
    expect(await navigateAs(null, '/operator-permissions')).toBe('/login');
    expect(heading()).toBe('Backoffice de Kuador');
    expect(document.title).toBe('Iniciar sesión · Backoffice de Kuador');
  });

  it('opens «Complejos» after the login', async () => {
    expect(await navigateAs(OPERATOR_USER, '/')).toBe('/complexes');
    expect(heading()).toBe('Complejos');
  });

  it('sends a signed-in user away from the login', async () => {
    expect(await navigateAs(SUPER_ADMIN_USER, '/login')).toBe('/complexes');
  });

  it.each(['/operator-permissions', '/plans', '/plans/new', '/plans/fields', '/plans/some-id'])(
    'shows «No tienes permiso» to an operator who types %s (CA15)',
    async (url) => {
      expect(await navigateAs(OPERATOR_USER, url)).toBe('/forbidden');
      expect(harness.fixture.nativeElement.textContent).toContain(
        'No tienes permiso para ver esta sección.',
      );
      expect(
        harness.fixture.nativeElement.querySelector('main a[href="/complexes"]')?.textContent,
      ).toBe('Ir a Complejos');
    },
  );

  it('lets the super admin open «Permisos del operador»', async () => {
    expect(await navigateAs(SUPER_ADMIN_USER, '/operator-permissions')).toBe(
      '/operator-permissions',
    );
    backend.expectOne(CAPABILITIES_URL).flush(INITIAL_CAPABILITIES);
    backend
      .expectOne((request) => request.url === CAPABILITY_CHANGES_URL)
      .flush({
        items: [],
        page: 0,
        size: 20,
        totalItems: 0,
      });

    expect(heading()).toBe('Permisos del operador');
  });

  it('lets the super admin open «Planes», «Crear plan» and «Campos de plan»', async () => {
    expect(await navigateAs(SUPER_ADMIN_USER, '/plans')).toBe('/plans');
    backend.expectOne((request) => request.url === PLANS_URL).flush(pageOf([]));
    await settle();
    expect(heading()).toBe('Planes');
    expect(document.title).toBe('Planes · Backoffice de Kuador');

    const creating = harness.navigateByUrl('/plans/new');
    await vi.waitFor(() => backend.expectOne(PLAN_OPTIONS_URL).flush(PLAN_OPTIONS));
    backend.expectOne(PLAN_FIELDS_URL).flush([]);
    await creating;
    await settle();
    expect(heading()).toBe('Crear plan');

    const fields = harness.navigateByUrl('/plans/fields');
    await vi.waitFor(() => backend.expectOne(PLAN_FIELDS_URL).flush([]));
    await fields;
    await settle();
    expect(heading()).toBe('Campos de plan');
    expect(
      harness.fixture.nativeElement.querySelector('[aria-current="page"]')?.textContent,
    ).toContain('Planes');
  });

  it('opens «Dar de alta complejo» only with COMPLEX_CREATE', async () => {
    expect(await navigateAs(OPERATOR_USER, '/complexes/new')).toBe('/forbidden');
  });

  it('lets the super admin open «Dar de alta complejo»', async () => {
    expect(await navigateAs(SUPER_ADMIN_USER, '/complexes/new')).toBe('/complexes/new');
    backend.expectOne((request) => request.url === PLANS_URL).flush(EMPTY_PAGE);
    await settle();

    expect(heading()).toBe('Dar de alta complejo');
    expect(document.title).toBe('Dar de alta complejo · Backoffice de Kuador');
  });

  it('sends unknown addresses home', async () => {
    expect(await navigateAs(OPERATOR_USER, '/does-not-exist')).toBe('/complexes');
  });
});
