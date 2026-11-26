import { HttpInterceptorFn } from '@angular/common/http';
import { mockBackendInterceptor } from './dev/mock-backend/mock-backend.interceptor';

/** Development build: the api is the in-memory mock of the contract. See mock-backend.ts. */
export const mockBackendInterceptors: readonly HttpInterceptorFn[] = [mockBackendInterceptor];
