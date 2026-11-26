import { HttpTestingController } from '@angular/common/http/testing';
import { ComponentFixture, TestBed } from '@angular/core/testing';
import { Router, provideRouter } from '@angular/router';
import { expectNoAxeViolations } from '../../../testing/axe';
import {
  LOGIN_REQUEST_URL,
  ME_URL,
  SUPER_ADMIN_USER,
  problemBody,
} from '../../../testing/platform-fixtures';
import { provideTestApi, settle } from '../../../testing/platform-testing';
import { Session } from '../../core/auth/session';
import { LoginPage } from './login-page';

describe('LoginPage (P1)', () => {
  let fixture: ComponentFixture<LoginPage>;
  let backend: HttpTestingController;
  let router: Router;

  beforeEach(async () => {
    sessionStorage.clear();
    TestBed.configureTestingModule({ providers: [provideTestApi(), provideRouter([])] });
    backend = TestBed.inject(HttpTestingController);
    router = TestBed.inject(Router);
    vi.spyOn(router, 'navigateByUrl').mockResolvedValue(true);
  });

  afterEach(() => {
    backend.verify();
    fixture?.nativeElement.remove();
  });

  async function render(): Promise<void> {
    fixture = TestBed.createComponent(LoginPage);
    document.body.appendChild(fixture.nativeElement);
    await fixture.whenStable();
  }

  const element = (): HTMLElement => fixture.nativeElement;
  const inputs = () => Array.from(element().querySelectorAll('input')) as HTMLInputElement[];

  function type(input: HTMLInputElement, value: string): void {
    input.value = value;
    input.dispatchEvent(new Event('input'));
  }

  async function submit(email: string, password: string): Promise<void> {
    const [emailInput, passwordInput] = inputs();
    type(emailInput, email);
    type(passwordInput, password);
    element()
      .querySelector('form')
      ?.dispatchEvent(new Event('submit', { cancelable: true }));
    await fixture.whenStable();
  }

  async function answerLogin(body: object, status = 200): Promise<void> {
    backend
      .expectOne(LOGIN_REQUEST_URL)
      .flush(body, { status, statusText: status === 200 ? 'OK' : 'Error' });
    await settle();
  }

  it('asks for the e-mail and password with labelled fields', async () => {
    await render();

    expect(element().querySelector('h1')?.textContent).toBe('Backoffice de Kuador');
    expect(inputs().map((input) => input.labels?.[0]?.textContent)).toEqual([
      'Correo',
      'Contraseña',
    ]);
    expect(inputs().map((input) => input.type)).toEqual(['email', 'password']);
    expect(element().querySelector('button[type="submit"]')?.textContent?.trim()).toBe(
      'Iniciar sesión',
    );
  });

  it('marks the empty fields and focuses the first one without calling the api', async () => {
    await render();

    await submit('  ', '');

    expect(element().textContent).toContain('Escribe tu correo.');
    expect(element().textContent).toContain('Escribe tu contraseña.');
    expect(document.activeElement).toBe(inputs()[0]);
    backend.expectNone(LOGIN_REQUEST_URL);
  });

  it('signs in and goes to «Complejos»', async () => {
    await render();

    await submit('sam@example.com', 'secret-value');
    expect(element().querySelector('button[type="submit"]')?.textContent?.trim()).toBe(
      'Iniciando sesión…',
    );
    await answerLogin({ accessToken: 'token', tokenType: 'Bearer', expiresIn: 900 });
    backend.expectOne(ME_URL).flush(SUPER_ADMIN_USER);
    await settle();

    expect(router.navigateByUrl).toHaveBeenCalledWith('/complexes');
  });

  it('says the credentials are wrong without saying which one', async () => {
    await render();

    await submit('sam@example.com', 'wrong');
    await answerLogin(problemBody(401, 'INVALID_CREDENTIALS'), 401);

    expect(element().querySelector('[role="alert"]')?.textContent).toContain(
      'Correo o contraseña incorrectos.',
    );
    expect(inputs()[0].value).toBe('sam@example.com');
    expect(router.navigateByUrl).not.toHaveBeenCalled();
  });

  it('shows the generic failure for any other problem', async () => {
    await render();

    await submit('sam@example.com', 'x');
    backend.expectOne(LOGIN_REQUEST_URL).error(new ProgressEvent('error'));
    await settle();

    expect(element().querySelector('[role="alert"]')?.textContent).toContain(
      'No pudimos iniciar sesión. Inténtalo de nuevo.',
    );
  });

  it('explains that the session expired when the api ended it', async () => {
    TestBed.inject(Session).expire();
    await render();

    expect(element().querySelector('[role="status"]')?.textContent).toContain(
      'Tu sesión venció. Inicia sesión de nuevo.',
    );
  });

  it('has no axe violations, also with errors', async () => {
    await render();
    await expectNoAxeViolations(element());

    await submit('', '');
    await expectNoAxeViolations(element());
  });
});
