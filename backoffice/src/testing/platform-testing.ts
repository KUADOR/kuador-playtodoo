import { ApplicationRef, EnvironmentProviders, Provider } from '@angular/core';
import { provideHttpClient, withInterceptors } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { TestBed } from '@angular/core/testing';
import { API_BASE_URL } from '../app/core/api/api-base-url';
import { accessTokenInterceptor, apiErrorInterceptor } from '../app/core/api/api-interceptors';
import { CurrentPlatformUser } from '../app/core/auth/platform-user';
import { Session } from '../app/core/auth/session';
import { ME_URL, TEST_API_BASE_URL, storeAccessToken } from './platform-fixtures';

/** HttpClient with the app interceptors in front of `HttpTestingController`. */
export function provideTestApi(): (Provider | EnvironmentProviders)[] {
  return [
    provideHttpClient(withInterceptors([accessTokenInterceptor, apiErrorInterceptor])),
    provideHttpClientTesting(),
    { provide: API_BASE_URL, useValue: TEST_API_BASE_URL },
  ];
}

/** Leaves `user` signed in, answering the `GET /me` that the session sends. */
export async function signInAs(user: CurrentPlatformUser): Promise<void> {
  storeAccessToken();
  const loaded = TestBed.inject(Session).ensureUser();
  TestBed.inject(HttpTestingController).expectOne(ME_URL).flush(user);
  await loaded;
}

/** Lets pending promises and the resulting change detection run. */
export async function settle(): Promise<void> {
  await new Promise((resolve) => setTimeout(resolve));
  await TestBed.inject(ApplicationRef).whenStable();
}
