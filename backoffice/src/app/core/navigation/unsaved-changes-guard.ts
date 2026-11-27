import { inject } from '@angular/core';
import { CanDeactivateFn } from '@angular/router';
import { Session } from '../auth/session';

/** A page with a form that may hold changes the person has not saved. */
export interface LeaveConfirmation {
  /** True to leave now; otherwise the page asks the person and resolves with the answer. */
  confirmLeave(): boolean | Promise<boolean>;
}

/**
 * «Salir con cambios pide confirmación» («Pantallas», P11). Without a session (signed out or
 * expired) the page is left without asking: nothing could be saved anyway.
 */
export const unsavedChangesGuard: CanDeactivateFn<LeaveConfirmation> = (page) =>
  inject(Session).user() === null || page.confirmLeave();
