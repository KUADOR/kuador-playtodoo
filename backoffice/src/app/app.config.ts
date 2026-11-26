import { ApplicationConfig, provideBrowserGlobalErrorListeners } from '@angular/core';
import { provideHttpClient, withFetch, withInterceptors } from '@angular/common/http';
import { provideRouter } from '@angular/router';
import { accessTokenInterceptor, apiErrorInterceptor } from './core/api/api-interceptors';
import { routes } from './app.routes';
import { mockBackendInterceptors } from './mock-backend';

export const appConfig: ApplicationConfig = {
  providers: [
    provideBrowserGlobalErrorListeners(),
    provideRouter(routes),
    // Order matters: the token is added before the error rules read it; the mock (development
    // only) answers last, where the network would.
    provideHttpClient(
      withFetch(),
      withInterceptors([accessTokenInterceptor, apiErrorInterceptor, ...mockBackendInterceptors]),
    ),
  ],
};
