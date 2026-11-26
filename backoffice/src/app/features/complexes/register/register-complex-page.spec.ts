import { HttpTestingController, TestRequest } from '@angular/common/http/testing';
import { ComponentFixture, TestBed } from '@angular/core/testing';
import { Router, provideRouter } from '@angular/router';
import { expectNoAxeViolations } from '../../../../testing/axe';
import {
  ACTIVE_COMPLEX,
  BASIC_PLAN,
  COMPLEXES_URL,
  PLANS_URL,
  PREMIUM_PLAN,
  PROVISIONING_FAILED_COMPLEX,
  pageWith,
  SUBDOMAINS_URL,
} from '../../../../testing/complex-fixtures';
import {
  OPERATOR_USER,
  SUPER_ADMIN_USER,
  problemBody,
} from '../../../../testing/platform-fixtures';
import { provideTestApi, settle, signInAs } from '../../../../testing/platform-testing';
import { ROOT_DOMAIN } from '../../../core/api/root-domain';
import { CurrentPlatformUser } from '../../../core/auth/platform-user';
import { RegisterComplexPage, TYPING_DEBOUNCE_MS } from './register-complex-page';

const SUGGESTION_URL = `${SUBDOMAINS_URL}/suggestion`;
const availabilityUrl = (subdomain: string) => `${SUBDOMAINS_URL}/${subdomain}/availability`;

describe('RegisterComplexPage (P3)', () => {
  let fixture: ComponentFixture<RegisterComplexPage>;
  let backend: HttpTestingController;
  let router: Router;

  beforeEach(() => {
    sessionStorage.clear();
    TestBed.configureTestingModule({
      providers: [
        provideTestApi(),
        provideRouter([]),
        { provide: ROOT_DOMAIN, useValue: 'playtodoo.com' },
      ],
    });
    backend = TestBed.inject(HttpTestingController);
    router = TestBed.inject(Router);
    vi.spyOn(router, 'navigateByUrl').mockResolvedValue(true);
  });

  afterEach(() => {
    backend.verify();
    document.body.replaceChildren();
  });

  const root = (): HTMLElement => fixture.nativeElement;
  const control = (label: string) => {
    const element = Array.from(root().querySelectorAll('label')).find(
      (candidate) => candidate.textContent?.trim() === label,
    );
    return root().querySelector(`#${element?.htmlFor}`) as HTMLInputElement | HTMLSelectElement;
  };
  const button = (label: string) =>
    Array.from(root().querySelectorAll('button')).find(
      (candidate) => candidate.textContent?.trim() === label,
    ) as HTMLButtonElement;
  const subdomainStatus = () => root().querySelector('.register__subdomain-status');
  const errorOf = (label: string) => {
    const describedBy = control(label).getAttribute('aria-describedby') ?? '';
    return describedBy
      .split(' ')
      .map((id) => root().querySelector(`#${id}.field__error`)?.textContent?.trim())
      .find(Boolean);
  };

  async function type(label: string, value: string): Promise<void> {
    const element = control(label);
    element.value = value;
    element.dispatchEvent(new Event(element instanceof HTMLSelectElement ? 'change' : 'input'));
    await settle();
  }

  async function typingPause(): Promise<void> {
    await new Promise((resolve) => setTimeout(resolve, TYPING_DEBOUNCE_MS + 50));
    await settle();
  }

  function expectPlans(): TestRequest {
    const request = backend.expectOne((candidate) => candidate.url === PLANS_URL);
    expect(request.request.params.get('status')).toBe('PUBLISHED');
    return request;
  }

  async function render(
    user: CurrentPlatformUser = SUPER_ADMIN_USER,
    plans = [BASIC_PLAN, PREMIUM_PLAN],
  ): Promise<void> {
    await signInAs(user);
    fixture = TestBed.createComponent(RegisterComplexPage);
    document.body.appendChild(fixture.nativeElement);
    await fixture.whenStable();
    expectPlans().flush(pageWith(plans));
    await settle();
  }

  async function fillValidForm(): Promise<void> {
    await type('Subdominio', 'canchas-el-sol');
    await typingPause();
    backend
      .expectOne(availabilityUrl('canchas-el-sol'))
      .flush({ subdomain: 'canchas-el-sol', available: true, reason: null });
    await type('Nombre del complejo', 'Canchas El Sol');
    await type('Correo del propietario', 'owner@example.com');
    await type('Plan', PREMIUM_PLAN.id);
    await typingPause();
  }

  it('suggests the subdomain from the name and checks it live (CA1)', async () => {
    await render();

    await type('Nombre del complejo', 'Canchas El Sol');
    await typingPause();
    const suggestion = backend.expectOne((candidate) => candidate.url === SUGGESTION_URL);
    expect(suggestion.request.params.get('name')).toBe('Canchas El Sol');
    suggestion.flush({ subdomain: 'canchas-el-sol' });
    await settle();

    expect(control('Subdominio').value).toBe('canchas-el-sol');
    expect(subdomainStatus()?.textContent?.trim()).toBe('Comprobando…');
    expect(subdomainStatus()?.getAttribute('role')).toBe('status');
    await typingPause();
    backend
      .expectOne(availabilityUrl('canchas-el-sol'))
      .flush({ subdomain: 'canchas-el-sol', available: true, reason: null });
    await settle();

    expect(subdomainStatus()?.textContent?.trim()).toBe('Disponible');
    expect(root().querySelector('.field__suffix')?.textContent).toBe('.playtodoo.com');
  });

  it('stops suggesting once the subdomain is edited by hand', async () => {
    await render();
    await type('Subdominio', 'Mi-Club');
    await typingPause();
    backend
      .expectOne(availabilityUrl('mi-club'))
      .flush({ subdomain: 'mi-club', available: true, reason: null });

    await type('Nombre del complejo', 'Otro nombre');
    await typingPause();

    backend.expectNone((candidate) => candidate.url === SUGGESTION_URL);
    expect(control('Subdominio').value).toBe('mi-club');
  });

  it('rejects a malformed subdomain without asking the api, and a taken or reserved one (CA2)', async () => {
    await render();

    await type('Subdominio', 'el--sol');
    await typingPause();
    expect(errorOf('Subdominio')).toBe(
      'Usa de 3 a 63 letras minúsculas, números o guiones; sin guiones al inicio, al final ni seguidos. No puede empezar con “xn--”.',
    );

    await type('Subdominio', 'canchas-el-sol');
    await typingPause();
    backend
      .expectOne(availabilityUrl('canchas-el-sol'))
      .flush({ subdomain: 'canchas-el-sol', available: false, reason: 'TAKEN' });
    await settle();
    expect(errorOf('Subdominio')).toBe('Ese subdominio ya está en uso. Elige otro.');

    await type('Subdominio', 'admin');
    await typingPause();
    backend
      .expectOne(availabilityUrl('admin'))
      .flush({ subdomain: 'admin', available: false, reason: 'RESERVED' });
    await settle();
    expect(errorOf('Subdominio')).toBe('Ese nombre está reservado. Elige otro.');
  });

  it('validates every field and focuses the first error before sending', async () => {
    await render();

    button('Dar de alta').click();
    await settle();

    expect(errorOf('Nombre del complejo')).toBe('Escribe el nombre del complejo.');
    expect(errorOf('Subdominio')).toBe('Escribe el subdominio.');
    expect(errorOf('Correo del propietario')).toBe('Escribe el correo del propietario.');
    expect(errorOf('Plan')).toBe('Elige un plan.');
    expect(document.activeElement).toBe(control('Nombre del complejo'));
    backend.expectNone(COMPLEXES_URL);
  });

  it('shows the plan summary and registers the complex, then opens it with a notice (CA1)', async () => {
    await render();
    await fillValidForm();

    expect(root().textContent).toContain('120.00 USD · Mensual · 10 canchas · 25 usuarios');
    expect(root().textContent).toContain('La suscripción quedará activa.');
    button('Dar de alta').click();
    await settle();
    expect(button('Creando el espacio del complejo…')).toBeDefined();
    const request = backend.expectOne({ method: 'POST', url: COMPLEXES_URL });
    expect(request.request.body).toEqual({
      displayName: 'Canchas El Sol',
      subdomain: 'canchas-el-sol',
      ownerEmail: 'owner@example.com',
      planId: PREMIUM_PLAN.id,
    });
    request.flush(ACTIVE_COMPLEX, { status: 201, statusText: 'Created' });
    await settle();

    expect(router.navigateByUrl).toHaveBeenCalledWith(`/complexes/${ACTIVE_COMPLEX.id}`, {
      state: { resultNotice: 'Dimos de alta Canchas El Sol. Su suscripción quedó activa.' },
    });
  });

  it('opens the incomplete registration without a success notice', async () => {
    await render();
    await fillValidForm();

    button('Dar de alta').click();
    await settle();
    backend
      .expectOne({ method: 'POST', url: COMPLEXES_URL })
      .flush(PROVISIONING_FAILED_COMPLEX, { status: 201, statusText: 'Created' });
    await settle();

    expect(router.navigateByUrl).toHaveBeenCalledWith(`/complexes/${ACTIVE_COMPLEX.id}`);
  });

  it('keeps the form and marks the subdomain when it was taken meanwhile (CA2)', async () => {
    await render();
    await fillValidForm();

    button('Dar de alta').click();
    await settle();
    backend
      .expectOne({ method: 'POST', url: COMPLEXES_URL })
      .flush(problemBody(409, 'SUBDOMAIN_TAKEN'), { status: 409, statusText: 'Conflict' });
    await settle();

    expect(errorOf('Subdominio')).toBe('Ese subdominio ya está en uso. Elige otro.');
    expect(control('Nombre del complejo').value).toBe('Canchas El Sol');
    expect(document.activeElement).toBe(control('Subdominio'));
    expect(router.navigateByUrl).not.toHaveBeenCalled();
  });

  it('asks for another plan when the chosen one is no longer published (CA3)', async () => {
    await render();
    await fillValidForm();

    button('Dar de alta').click();
    await settle();
    backend
      .expectOne({ method: 'POST', url: COMPLEXES_URL })
      .flush(problemBody(422, 'PLAN_NOT_PUBLISHED'), { status: 422, statusText: 'Unprocessable' });
    await settle();
    expectPlans().flush(pageWith([BASIC_PLAN]));
    await settle();

    expect(errorOf('Plan')).toBe('Ese plan ya no está publicado. Elige otro.');
  });

  it('cannot register without published plans and links to «Planes» only for the super admin', async () => {
    await render(SUPER_ADMIN_USER, []);

    expect(root().textContent).toContain(
      'No hay planes publicados. Publica un plan antes de dar de alta un complejo.',
    );
    expect(root().querySelector('a[href="/plans"]')?.textContent).toBe('Ir a Planes');
    expect(button('Dar de alta').disabled).toBe(true);
  });

  it('does not link to «Planes» for an operator who can register complexes', async () => {
    await render({ ...OPERATOR_USER, capabilities: ['COMPLEX_CREATE'] }, []);

    expect(root().textContent).toContain('No hay planes publicados.');
    expect(root().querySelector('a[href="/plans"]')).toBeNull();
  });

  it('has no axe violations', async () => {
    await render();
    await fillValidForm();

    await expectNoAxeViolations(root());
  });
});
