import { HttpTestingController } from '@angular/common/http/testing';
import { TestBed } from '@angular/core/testing';
import { provideRouter } from '@angular/router';
import {
  LOGIN_REQUEST_URL,
  ME_URL,
  SUPER_ADMIN_USER,
  problemBody,
  storeAccessToken,
} from '../../../testing/platform-fixtures';
import { provideTestApi } from '../../../testing/platform-testing';
import { Session } from './session';

describe('Session', () => {
  let session: Session;
  let backend: HttpTestingController;

  beforeEach(() => {
    sessionStorage.clear();
    TestBed.configureTestingModule({ providers: [provideTestApi(), provideRouter([])] });
    session = TestBed.inject(Session);
    backend = TestBed.inject(HttpTestingController);
  });

  afterEach(() => backend.verify());

  async function flushLoginThenMe(): Promise<void> {
    backend
      .expectOne(LOGIN_REQUEST_URL)
      .flush({ accessToken: 'issued-token', tokenType: 'Bearer', expiresIn: 900 });
    await vi.waitFor(() => backend.expectOne(ME_URL).flush(SUPER_ADMIN_USER));
  }

  it('signs in, keeps the token and loads the user with it', async () => {
    const signedIn = session.signIn({ email: 'sam@example.com', password: 'x' });
    await flushLoginThenMe();

    await expect(signedIn).resolves.toEqual(SUPER_ADMIN_USER);
    expect(session.user()).toEqual(SUPER_ADMIN_USER);
    expect(session.accessToken()).toBe('issued-token');
  });

  it('sends the credentials as the LoginRequest of the contract and keeps nothing on failure', async () => {
    const signedIn = session.signIn({ email: 'sam@example.com', password: 'x' });

    const login = backend.expectOne(LOGIN_REQUEST_URL);
    expect(login.request.method).toBe('POST');
    expect(login.request.body).toEqual({ email: 'sam@example.com', password: 'x' });
    login.flush(problemBody(401, 'INVALID_CREDENTIALS'), {
      status: 401,
      statusText: 'Unauthorized',
    });

    await expect(signedIn).rejects.toEqual(
      expect.objectContaining({ code: 'INVALID_CREDENTIALS' }),
    );
    expect(session.accessToken()).toBeNull();
    expect(session.user()).toBeNull();
  });

  it('loads GET /me only once for concurrent callers', async () => {
    storeAccessToken();

    const first = session.ensureUser();
    const second = session.ensureUser();
    backend.expectOne(ME_URL).flush(SUPER_ADMIN_USER);

    await expect(first).resolves.toEqual(SUPER_ADMIN_USER);
    await expect(second).resolves.toEqual(SUPER_ADMIN_USER);
  });

  it('has no user and sends nothing without a token', async () => {
    await expect(session.ensureUser()).resolves.toBeNull();
  });

  it('drops an expired token without calling the api', async () => {
    sessionStorage.setItem(
      'playtodoo.backoffice.access-token',
      JSON.stringify({ accessToken: 'old', expiresAt: Date.now() - 1 }),
    );

    await expect(session.ensureUser()).resolves.toBeNull();
    expect(session.accessToken()).toBeNull();
  });

  it('leaves a notice and no token when GET /me fails for another reason', async () => {
    storeAccessToken();

    const loaded = session.ensureUser();
    backend
      .expectOne(ME_URL)
      .flush(problemBody(503, 'INVALID_REQUEST'), { status: 503, statusText: 'Unavailable' });

    await expect(loaded).resolves.toBeNull();
    expect(session.notice()).toBe('USER_LOAD_FAILED');
    expect(session.accessToken()).toBeNull();
  });

  it('keeps the user when reloading GET /me fails for a reason other than 401', async () => {
    storeAccessToken();
    const loaded = session.ensureUser();
    backend.expectOne(ME_URL).flush(SUPER_ADMIN_USER);
    await loaded;

    const refreshed = session.refreshUser();
    backend.expectOne(ME_URL).error(new ProgressEvent('error'));
    await refreshed;

    expect(session.user()).toEqual(SUPER_ADMIN_USER);
    expect(session.accessToken()).not.toBeNull();
  });

  it('signs out without a notice', async () => {
    const signedIn = session.signIn({ email: 'sam@example.com', password: 'x' });
    await flushLoginThenMe();
    await signedIn;

    session.signOut();

    expect(session.user()).toBeNull();
    expect(session.accessToken()).toBeNull();
    expect(session.notice()).toBeNull();
  });
});
