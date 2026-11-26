import { HttpErrorResponse } from '@angular/common/http';
import { ApiError, asApiError, toApiError } from './api-error';
import { COMMON_MESSAGES, errorMessage } from './error-messages';

describe('toApiError', () => {
  it('reads the code and field errors of a Problem Details body', () => {
    const error = toApiError(
      new HttpErrorResponse({
        status: 422,
        url: '/api/v1/platform/x',
        error: {
          status: 422,
          code: 'VALIDATION_FAILED',
          title: 'Validation failed',
          fieldErrors: [{ field: 'amount', code: 'NOT_POSITIVE' }],
        },
      }),
    );

    expect(error.code).toBe('VALIDATION_FAILED');
    expect(error.status).toBe(422);
    expect(error.fieldErrors).toEqual([{ field: 'amount', code: 'NOT_POSITIVE' }]);
  });

  it('parses a Problem Details body that arrives as text', () => {
    const error = toApiError(
      new HttpErrorResponse({
        status: 409,
        error: JSON.stringify({ status: 409, code: 'STALE_STATE' }),
      }),
    );

    expect(error.code).toBe('STALE_STATE');
  });

  it('reports a failure without Problem Details as UNEXPECTED_RESPONSE', () => {
    expect(
      toApiError(new HttpErrorResponse({ status: 502, error: '<html>Bad gateway</html>' })).code,
    ).toBe('UNEXPECTED_RESPONSE');
  });

  it('reports a request that never got an answer as NETWORK_ERROR', () => {
    expect(
      toApiError(new HttpErrorResponse({ status: 0, error: new ProgressEvent('error') })).code,
    ).toBe('NETWORK_ERROR');
  });

  it('keeps an ApiError as it is and wraps anything else', () => {
    const original = new ApiError('FORBIDDEN', 403, 'Not allowed');

    expect(asApiError(original)).toBe(original);
    expect(asApiError(new Error('boom')).code).toBe('UNEXPECTED_RESPONSE');
  });
});

describe('errorMessage', () => {
  const stale = new ApiError('STALE_STATE', 409, 'Stale');

  it('uses the common text of «Pantallas» for 401, 403 and 409', () => {
    expect(errorMessage(new ApiError('UNAUTHENTICATED', 401, ''), 'x')).toBe(
      COMMON_MESSAGES.sessionExpired,
    );
    expect(errorMessage(new ApiError('CAPABILITY_REVOKED', 403, ''), 'x')).toBe(
      COMMON_MESSAGES.permissionRevoked,
    );
    expect(errorMessage(new ApiError('FORBIDDEN', 403, ''), 'x')).toBe(
      COMMON_MESSAGES.permissionRevoked,
    );
    expect(errorMessage(stale, 'x')).toBe(COMMON_MESSAGES.staleState);
  });

  it('prefers the screen text and falls back when the code has no text', () => {
    expect(errorMessage(stale, 'x', { STALE_STATE: 'Otro texto.' })).toBe('Otro texto.');
    expect(errorMessage(new ApiError('NETWORK_ERROR', 0, ''), 'No pudimos.')).toBe('No pudimos.');
  });
});
