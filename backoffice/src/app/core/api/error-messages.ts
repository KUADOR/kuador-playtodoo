import { ApiError, ApiErrorCode } from './api-error';

/** Texts of «Pantallas» (PLATODO-27, «Reglas comunes») shared by every screen. */
export const COMMON_MESSAGES = {
  loadFailed: 'No pudimos cargar la información.',
  saveFailed: 'No pudimos guardar los cambios. Inténtalo de nuevo.',
  sessionExpired: 'Tu sesión venció. Inicia sesión de nuevo.',
  permissionRevoked: 'Ya no tienes permiso para esta acción.',
  staleState: 'El estado del complejo cambió. Revisa los datos y vuelve a intentarlo.',
} as const;

export type ErrorMessages = Partial<Record<ApiErrorCode, string>>;

const COMMON_ERROR_MESSAGES: ErrorMessages = {
  UNAUTHENTICATED: COMMON_MESSAGES.sessionExpired,
  FORBIDDEN: COMMON_MESSAGES.permissionRevoked,
  CAPABILITY_REVOKED: COMMON_MESSAGES.permissionRevoked,
  STALE_STATE: COMMON_MESSAGES.staleState,
};

/**
 * Spanish text for an api failure: the screen's own texts first, then the common ones, then
 * `fallback` (what the screen says when the cause is unknown).
 */
export function errorMessage(
  error: ApiError,
  fallback: string,
  screenMessages: ErrorMessages = {},
): string {
  return screenMessages[error.code] ?? COMMON_ERROR_MESSAGES[error.code] ?? fallback;
}
