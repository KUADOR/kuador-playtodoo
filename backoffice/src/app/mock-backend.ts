import { EnvironmentProviders, Provider } from '@angular/core';
import { HttpInterceptorFn } from '@angular/common/http';

/**
 * Interceptors and providers that replace the api with the in-memory mock of the contract. Empty
 * in production; angular.json swaps this file for mock-backend.development.ts in the development
 * configuration, so the mock never reaches the production bundle.
 */
export const mockBackendInterceptors: readonly HttpInterceptorFn[] = [];

export const mockBackendProviders: readonly (Provider | EnvironmentProviders)[] = [];
