import { ApiError } from '../../../core/api/api-error';
import { COMMON_MESSAGES, errorMessage } from '../../../core/api/error-messages';
import { SESSION_ENDED_CODES, SUPPORT_ERROR_MESSAGES } from '../support-texts';

/** What a support dialog does with a failure of the complex host. */
export type SupportFailure =
  | { readonly kind: 'session-ended' }
  | { readonly kind: 'slot-taken'; readonly message: string }
  | { readonly kind: 'message'; readonly message: string };

/**
 * Sorts a failure: the session ended (P9 drops everything), the slot was taken meanwhile (the
 * occupancy is read again) or a message to show in the dialog.
 */
export function supportFailure(error: ApiError): SupportFailure {
  if (SESSION_ENDED_CODES.includes(error.code)) {
    return { kind: 'session-ended' };
  }
  const message = errorMessage(error, COMMON_MESSAGES.saveFailed, SUPPORT_ERROR_MESSAGES);
  return error.code === 'SLOT_UNAVAILABLE'
    ? { kind: 'slot-taken', message }
    : { kind: 'message', message };
}
