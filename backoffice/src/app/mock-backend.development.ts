import { EnvironmentProviders, Provider } from '@angular/core';
import { HttpInterceptorFn } from '@angular/common/http';
import { of } from 'rxjs';
import { SUPPORT_COURTS } from './features/support/support-courts';
import { mockBackendInterceptor } from './dev/mock-backend/mock-backend.interceptor';
import { MOCK_COURTS } from './dev/mock-backend/mock-support-backend';

/** Development build: the api is the in-memory mock of the contract. See mock-backend.ts. */
export const mockBackendInterceptors: readonly HttpInterceptorFn[] = [mockBackendInterceptor];

/**
 * Example courts for the support view, which no contract lists yet (ADR-020, point 6), so
 * «Crear reserva» and «Bloquear horario» can be tried against the mock.
 */
export const mockBackendProviders: readonly (Provider | EnvironmentProviders)[] = [
  { provide: SUPPORT_COURTS, useValue: { courtsOf: () => of(MOCK_COURTS) } },
];
