import { HttpClient } from '@angular/common/http';
import { HttpTestingController } from '@angular/common/http/testing';
import { TestBed } from '@angular/core/testing';
import { Router, provideRouter } from '@angular/router';
import { firstValueFrom } from 'rxjs';
import {
  ME_URL,
  OPERATOR_USER,
  problemBody,
  storeAccessToken,
} from '../../../testing/platform-fixtures';
import { provideTestApi, signInAs } from '../../../testing/platform-testing';
import { Session } from '../auth/session';
import { ApiError } from './api-error';

describe('api interceptors', () => {
  let http: HttpClient;
  let backend: HttpTestingController;
  let session: Session;
  let router: Router;

  beforeEach(() => {
    sessionStorage.clear();
    TestBed.configureTestingModule({ providers: [provideTestApi(), provideRouter([])] });
    http = TestBed.inject(HttpClient);
    backend = TestBed.inject(HttpTestingController);
    session = TestBed.inject(Session);
    router = TestBed.inject(Router);
    vi.spyOn(router, 'navigateByUrl').mockResolvedValue(true);
  });

  afterEach(() => backend.verify());

  it('sends the access token to the api only', () => {
    storeAccessToken('abc');

    http.get('/api/v1/platform/complexes').subscribe();
    http.get('https://elsewhere.example/data').subscribe();

    expect(
      backend.expectOne('/api/v1/platform/complexes').request.headers.get('Authorization'),
    ).toBe('Bearer abc');
    expect(
      backend.expectOne('https://elsewhere.example/data').request.headers.has('Authorization'),
    ).toBe(false);
  });

  it('turns a Problem Details failure into an ApiError', async () => {
    const response = firstValueFrom(http.get('/api/v1/platform/plans'));
    backend.expectOne('/api/v1/platform/plans').flush(problemBody(409, 'STALE_STATE'), {
      status: 409,
      statusText: 'Conflict',
    });

    await expect(response).rejects.toEqual(
      expect.objectContaining({ code: 'STALE_STATE', status: 409 }),
    );
    await expect(response).rejects.toBeInstanceOf(ApiError);
  });

  it('ends the session and goes to the login when a signed-in request answers 401', async () => {
    await signInAs(OPERATOR_USER);

    const response = firstValueFrom(http.get('/api/v1/platform/complexes'));
    backend.expectOne('/api/v1/platform/complexes').flush(problemBody(401, 'UNAUTHENTICATED'), {
      status: 401,
      statusText: 'Unauthorized',
    });

    await expect(response).rejects.toEqual(expect.objectContaining({ code: 'UNAUTHENTICATED' }));
    expect(session.user()).toBeNull();
    expect(session.notice()).toBe('SESSION_EXPIRED');
    expect(session.accessToken()).toBeNull();
    expect(router.navigateByUrl).toHaveBeenCalledWith('/login');
  });

  it('does not treat a 401 without a token (wrong credentials) as an expired session', async () => {
    const response = firstValueFrom(http.post('/api/v1/auth/platform/login', {}));
    backend
      .expectOne('/api/v1/auth/platform/login')
      .flush(problemBody(401, 'INVALID_CREDENTIALS'), {
        status: 401,
        statusText: 'Unauthorized',
      });

    await expect(response).rejects.toEqual(
      expect.objectContaining({ code: 'INVALID_CREDENTIALS' }),
    );
    expect(session.notice()).toBeNull();
    expect(router.navigateByUrl).not.toHaveBeenCalled();
  });

  it('reloads GET /me when a capability was revoked, so the menu and buttons follow it (CA16)', async () => {
    await signInAs(OPERATOR_USER);

    const response = firstValueFrom(http.post('/api/v1/platform/complexes/1/payments', {}));
    backend
      .expectOne('/api/v1/platform/complexes/1/payments')
      .flush(problemBody(403, 'CAPABILITY_REVOKED'), {
        status: 403,
        statusText: 'Forbidden',
      });

    await expect(response).rejects.toEqual(expect.objectContaining({ code: 'CAPABILITY_REVOKED' }));
    backend.expectOne(ME_URL).flush({ ...OPERATOR_USER, capabilities: [] });
    await vi.waitFor(() => expect(session.user()?.capabilities).toEqual([]));
  });
});
